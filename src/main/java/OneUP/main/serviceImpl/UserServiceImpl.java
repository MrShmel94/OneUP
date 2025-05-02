package OneUP.main.serviceImpl;

import OneUP.main.exception.InvalidCredentialsException;
import OneUP.main.exception.UnauthorizedException;
import OneUP.main.exception.UserNotValidatedException;
import OneUP.main.model.AppUser;
import OneUP.main.model.GuildMember;
import OneUP.main.request.ConfirmRequest;
import OneUP.main.request.LoginRequest;
import OneUP.main.request.RegisterRequest;
import OneUP.main.service.JwtService;
import OneUP.main.service.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;
import com.google.firebase.cloud.FirestoreClient;
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

    private final Firestore firestore = FirestoreClient.getFirestore();
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String REDIS_KEY = "guild:members";

    @Override
    public String checkAccess(String requiredRole) {
        String token = jwtService.extractTokenFromRequest();
        String nickname = jwtService.extractUsername(token);
        String redisKey = nickname.toLowerCase();

        Object cached = redisTemplate.opsForHash().get(REDIS_KEY, redisKey);
        Map<String, Object> userInfo;

        if (cached != null) {
            userInfo = objectMapper.convertValue(cached, new TypeReference<>() {});
        } else {
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
                userInfo = members.get(redisKey);
            } catch (Exception e) {
                log.error("Failed to read Firestore", e);
                throw new RuntimeException("Internal error");
            }

            if (userInfo == null) throw new UnauthorizedException("You are not a guild member");
        }

        if (Boolean.TRUE.equals(userInfo.get("banned"))) {
            jwtService.clearJwtFromResponse();
            throw new UnauthorizedException("You are banned");
        }

        String actualRole = String.valueOf(userInfo.get("role"));
        String tokenRole = jwtService.extractRole(token);

        if (!actualRole.equals(tokenRole)) {
            String newToken = jwtService.generateToken(nickname, actualRole);
            jwtService.setJwtCookieInResponse(newToken);
        }

        if (!hasSufficientRole(actualRole, requiredRole)) {
            throw new AccessDeniedException("Insufficient role: required " + requiredRole + ", but have " + actualRole);
        }

        return actualRole;
    }

    private boolean hasSufficientRole(String actual, String required) {
        List<String> hierarchy = List.of("USER", "MODERATOR", "ADMIN");
        return hierarchy.indexOf(actual) >= hierarchy.indexOf(required);
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
                    .role("USER")
                    .isBanned(false)
                    .confirmCode(code)
                    .build();

            Map<String, Object> update = Map.of(nickname, member);
            guildDoc.set(update, SetOptions.merge()).get();
            redisTemplate.opsForHash().put(REDIS_KEY, nickname, member);

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

        Object cached = redisTemplate.opsForHash().get(REDIS_KEY, redisKey);
        Map<String, Object> userInfo;

        if (cached != null) {
            userInfo = objectMapper.convertValue(cached, new TypeReference<>() {});
        } else {
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
                userInfo = members.get(redisKey);
            } catch (Exception e) {
                log.error("Failed to read Firestore", e);
                throw new RuntimeException("Internal error");
            }

            if (userInfo == null) throw new UnauthorizedException("You are not a guild member");
        }

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
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch user", e);
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

    @Override
    public void changeUserRole(String nickname, String newRole) {
        String key = nickname.toLowerCase();
        DocumentReference guildDoc = firestore.collection("guild").document("members");

        try {
            guildDoc.update(key + ".role", newRole).get();

            Object cached = redisTemplate.opsForHash().get(REDIS_KEY, key);
            if (cached != null) {
                GuildMember member = objectMapper.convertValue(cached, GuildMember.class);
                member.setRole(newRole);
                redisTemplate.opsForHash().put(REDIS_KEY, key, member);
            }

            log.info("Changed role of {} to {}", key, newRole);
        } catch (Exception e) {
            throw new RuntimeException("Failed to change user role", e);
        }
    }

    @Override
    public void banned (String nickname){

    }

    @Override
    public void unbanned (String nickname){

    }
}