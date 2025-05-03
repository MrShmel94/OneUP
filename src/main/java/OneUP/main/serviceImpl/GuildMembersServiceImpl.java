package OneUP.main.serviceImpl;

import OneUP.main.model.GuildMember;
import OneUP.main.security.Role;
import OneUP.main.security.VisibleForRole;
import OneUP.main.service.GuildMembersService;
import OneUP.main.service.JwtService;
import OneUP.main.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuildMembersServiceImpl implements GuildMembersService {

    private final Firestore firestore = FirestoreClient.getFirestore();
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    private static final String REDIS_KEY = "guild:members";
    private final Map<String, Role> fieldAccessMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void initFieldAccess() {
        for (Field field : GuildMember.class.getDeclaredFields()) {
            field.setAccessible(true);
            VisibleForRole annotation = field.getAnnotation(VisibleForRole.class);
            if (annotation != null) {
                fieldAccessMap.put(field.getName(), annotation.value());
            }
        }
        log.info("Field role access map initialized: {}", fieldAccessMap);
    }

    @Override
    public List<GuildMember> getAllMembers() {
        Role requesterRole = userService.checkAccess(Role.USER);

        Map<String, Object> raw = getMembersData();
        List<GuildMember> members = new ArrayList<>();

        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            GuildMember member = objectMapper.convertValue(entry.getValue(), GuildMember.class);
            members.add(filterMember(member, requesterRole));
        }

        return members;
    }

    private Map<String, Object> getMembersData() {
        Map<String, Object> raw;
        Object cached = redisTemplate.opsForHash().entries(REDIS_KEY);

        if (cached instanceof Map<?, ?> map && !map.isEmpty()) {
            raw = (Map<String, Object>) map;
        } else {
            try {
                DocumentSnapshot snapshot = firestore.collection("guild").document("members").get().get();
                if (!snapshot.exists()) throw new IllegalStateException("Guild data not found");

                raw = snapshot.getData();
                redisTemplate.opsForHash().putAll(REDIS_KEY, raw);
            } catch (Exception e) {
                log.error("Failed to fetch guild members from Firestore", e);
                throw new RuntimeException("Internal error while fetching guild members");
            }
        }
        return raw;
    }

    private GuildMember filterMember(GuildMember m, Role r) {
        return GuildMember.builder()
                .nickname(m.getNickname())
                .role(shouldShow("role", r) ? m.getRole() : "*****")
                .isBanned(shouldShow("isBanned", r) && m.isBanned())
                .fullName(shouldShow("fullName", r) ? m.getFullName() : "*****")
                .location(shouldShow("location", r) ? m.getLocation() : "*****")
                .timezone(shouldShow("timezone", r) ? m.getTimezone() : "*****")
                .about(shouldShow("about", r) ? m.getAbout() : "*****")
                .confirmCode(shouldShow("confirmCode", r) ? m.getConfirmCode() : "*****")
                .build();
    }

    private boolean shouldShow(String field, Role requesterRole) {
        Role minRole = fieldAccessMap.get(field);
        return minRole == null || requesterRole.hasAtLeast(minRole);
    }
}
