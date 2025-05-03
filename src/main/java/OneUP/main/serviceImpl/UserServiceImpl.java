package OneUP.main.serviceImpl;

import OneUP.main.exception.InvalidCredentialsException;
import OneUP.main.exception.UnauthorizedException;
import OneUP.main.exception.UserNotValidatedException;
import OneUP.main.model.AppUser;
import OneUP.main.model.GuildMember;
import OneUP.main.request.ConfirmRequest;
import OneUP.main.request.LoginRequest;
import OneUP.main.request.RegisterRequest;
import OneUP.main.security.RequireGuildAccess;
import OneUP.main.security.Role;
import OneUP.main.service.JwtService;
import OneUP.main.service.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;
import com.google.firebase.cloud.FirestoreClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final Firestore firestore;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String REDIS_KEY = "guild:members";

    @Override
    public Role checkAccess(Role requiredRole) {
        String token = jwtService.extractTokenFromRequest();
        String nickname = jwtService.extractUsername(token);
        String redisKey = nickname.toLowerCase();

        Map<String, Object> userInfo = getUserInfoFromCacheOrDb(redisKey);

        if (Boolean.TRUE.equals(userInfo.get("banned"))) {
            jwtService.clearJwtFromResponse();
            throw new UnauthorizedException("You are banned");
        }

        String actualRoleString = String.valueOf(userInfo.get("role"));
        Role actualRole = Role.valueOf(actualRoleString);
        String tokenRole = jwtService.extractRole(token);

        if (!actualRole.name().equals(tokenRole)) {
            String newToken = jwtService.generateToken(nickname, actualRole.name());
            jwtService.setJwtCookieInResponse(newToken);
        }

        if (!actualRole.hasAtLeast(requiredRole)) {
            throw new AccessDeniedException("Insufficient role: required " + requiredRole + ", but have " + actualRole);
        }

        return actualRole;
    }

    public void registerUser(RegisterRequest dto) {
        String nickname = dto.nickname().toLowerCase().trim();
        DocumentReference userDoc = firestore.collection("users").document(nickname);
        DocumentReference guildDoc = firestore.collection("guild").document("members");

        try {
            if (userDoc.get().get().exists()) {
                throw new IllegalStateException("Nickname already in use");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to check existing user", e);
        }

        String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));

        AppUser user = AppUser.builder()
                .nickname(nickname)
                .fullName(dto.fullName())
                .location(dto.location())
                .timezone(dto.timezone())
                .password(passwordEncoder.encode(dto.password()))
                .about(dto.about())
                .validated(false)
                .confirmCode(code)
                .createdAt(Instant.now())
                .build();

        try {
            userDoc.set(user).get();

            GuildMember member = GuildMember.builder()
                    .nickname(nickname)
                    .fullName(dto.fullName())
                    .location(dto.location())
                    .timezone(dto.timezone())
                    .about(dto.about())
                    .role(Role.USER.name())
                    .isBanned(false)
                    .confirmCode(code)
                    .build();

            Map<String, Object> update = Map.of(nickname, member);
            guildDoc.set(update, SetOptions.merge()).get();

            if (Boolean.FALSE.equals(redisTemplate.hasKey(REDIS_KEY))) {
                DocumentSnapshot snapshot = firestore.collection("guild").document("members").get().get();
                if (snapshot.exists()) {
                    Map<String, Object> raw = snapshot.getData();
                    redisTemplate.opsForHash().putAll(REDIS_KEY, raw);
                }
            } else {
                redisTemplate.opsForHash().put(REDIS_KEY, nickname, member);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to register user", e);
        }
    }

    @Override
    public String login(LoginRequest dto) {
        String nickname = dto.nickname().toLowerCase();
        AppUser user = getUser(nickname);

        if (!user.isValidated()) {
            throw new UserNotValidatedException();
        }

        if (!passwordEncoder.matches(dto.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String role = checkAccessWithoutToken(nickname);

        return jwtService.generateToken(nickname, role);
    }

    public String checkAccessWithoutToken(String nickname) {
        String redisKey = nickname.toLowerCase();

        Map<String, Object> userInfo = getUserInfoFromCacheOrDb(redisKey);

        if (Boolean.TRUE.equals(userInfo.get("banned"))) {
            throw new UnauthorizedException("You are banned");
        }

        return String.valueOf(userInfo.get("role"));
    }

    public AppUser getUser(String nickname) {
        try {
            DocumentSnapshot snapshot = firestore.collection("users").document(nickname.toLowerCase()).get().get();
            if (!snapshot.exists()) {
                throw new NoSuchElementException("User not found");
            }
            return snapshot.toObject(AppUser.class);
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException(String.format("User not found %s", nickname));
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to fetch user -> %s", e.getMessage()));
        }
    }

    public boolean passwordMatches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public String confirmUser(ConfirmRequest requestModel) {
        String nickname = requestModel.nickname().toLowerCase();
        DocumentReference userDoc = firestore.collection("users").document(nickname);
        DocumentReference guildDoc = firestore.collection("guild").document("members");

        try {
            AppUser user = getUser(nickname);
            if (user.isValidated()) {
                throw new IllegalStateException("User already validated");
            }

            if (!user.getConfirmCode().equals(requestModel.code())) {
                throw new IllegalArgumentException("Invalid confirmation code");
            }

            userDoc.update("validated", true).get();

            GuildMember member = GuildMember.builder()
                    .nickname(user.getNickname())
                    .fullName(user.getFullName())
                    .location(user.getLocation())
                    .timezone(user.getTimezone())
                    .about(user.getAbout())
                    .role("USER")
                    .isBanned(false)
                    .confirmCode(null)
                    .build();

            Map<String, Object> update = Map.of(nickname, member);
            guildDoc.set(update, SetOptions.merge()).get();

            redisTemplate.opsForHash().put(REDIS_KEY, nickname, member);

            return jwtService.generateToken(nickname);

        } catch (Exception e) {
            throw new RuntimeException("Failed to confirm user", e);
        }
    }

    @RequireGuildAccess(roles = {Role.ADMIN})
    @Override
    public void changeUserRole(String nickname, String newRole) {
        String key = nickname.toLowerCase();
        String currentUser = jwtService.extractUsername(jwtService.extractTokenFromRequest());

        if (currentUser.equals(key)) {
            throw new AccessDeniedException("You cannot change your own role");
        }

        DocumentReference guildDoc = firestore.collection("guild").document("members");

        try {
            DocumentSnapshot snapshot = guildDoc.get().get();
            if (!snapshot.exists()) {
                throw new RuntimeException("Guild members document not found");
            }

            Map<String, Object> members = snapshot.getData();
            if (members == null || !members.containsKey(key)) {
                throw new NoSuchElementException("Guild member not found: " + key);
            }

            guildDoc.update(key + ".role", newRole).get();

            Object cached = redisTemplate.opsForHash().get(REDIS_KEY, key);
            if (cached != null) {
                GuildMember member = objectMapper.convertValue(cached, GuildMember.class);
                member.setRole(newRole);
                redisTemplate.opsForHash().put(REDIS_KEY, key, member);
            }

            log.info("Changed role of {} to {}", key, newRole);

        } catch (NoSuchElementException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to change user role", e);
        }
    }

    @RequireGuildAccess(roles = {Role.ADMIN})
    @Override
    public void banned (String nickname){

        String currentUser = jwtService.extractUsername(jwtService.extractTokenFromRequest());

        if (currentUser.equals(nickname)) {
            throw new AccessDeniedException("You can't ban yourself");
        }

        changeBanStatus(nickname, true);
    }

    @RequireGuildAccess(roles = {Role.ADMIN})
    @Override
    public void unbanned (String nickname){
        String currentUser = jwtService.extractUsername(jwtService.extractTokenFromRequest());

        if (currentUser.equals(nickname)) {
            throw new AccessDeniedException("You can't ban yourself");
        }

        changeBanStatus(nickname, false);
    }

    private Map<String, Object> getUserInfoFromCacheOrDb(String redisKey) {
        Object cached = redisTemplate.opsForHash().get(REDIS_KEY, redisKey);
        if (cached != null) {
            return objectMapper.convertValue(cached, new TypeReference<>() {});
        }

        try {
            DocumentSnapshot snapshot = firestore.collection("guild").document("members").get().get();
            if (!snapshot.exists()) throw new UnauthorizedException("Guild data not found");

            Map<String, Object> raw = snapshot.getData();
            Map<String, Map<String, Object>> members = new HashMap<>();

            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                Map<String, Object> info = objectMapper.convertValue(entry.getValue(), new TypeReference<>() {});
                members.put(entry.getKey(), info);
            }

            redisTemplate.opsForHash().putAll(REDIS_KEY, members);

            Map<String, Object> result = members.get(redisKey);
            if (result == null) {
                throw new UnauthorizedException("You are not a guild member");
            }

            return members.get(redisKey);
        } catch (Exception e) {
            log.error("Failed to read Firestore", e);
            throw new RuntimeException("Internal error");
        }
    }

    private void changeBanStatus(String nickname, boolean banned) {
        String key = nickname.toLowerCase();
        DocumentReference guildDoc = firestore.collection("guild").document("members");

        try {
            guildDoc.update(key + ".banned", banned).get();

            Object cached = redisTemplate.opsForHash().get(REDIS_KEY, key);
            if (cached != null) {
                GuildMember member = objectMapper.convertValue(cached, GuildMember.class);
                member.setBanned(banned);
                redisTemplate.opsForHash().put(REDIS_KEY, key, member);
            }

            log.info("User '{}' has been {}", key, banned ? "banned" : "unbanned");
        } catch (Exception e) {
            throw new RuntimeException("Failed to change ban status for " + key, e);
        }
    }
}