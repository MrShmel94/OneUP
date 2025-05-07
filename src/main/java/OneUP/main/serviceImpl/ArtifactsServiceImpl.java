package OneUP.main.serviceImpl;

import OneUP.main.service.ArtifactService;
import OneUP.main.service.HeroesService;
import OneUP.main.service.JwtService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;
import com.google.firebase.cloud.FirestoreClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArtifactsServiceImpl implements ArtifactService {

    private final Firestore firestore = FirestoreClient.getFirestore();
    private final JwtService jwtService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;


    private static final String COLLECTION = "artifacts";
    private static final String REDIS_KEY = "artifacts";

    @Override
    public void savePlayerArtifact(String nickname, Map<String, Map<String, String>> artifacts) throws ExecutionException, InterruptedException, BadRequestException {

        if(nickname == null || nickname.isEmpty()) {
            nickname = jwtService.extractUsername(jwtService.extractTokenFromRequest());
            if(nickname == null || nickname.isEmpty()) {
                throw new BadRequestException("Nickname cannot be empty");
            }
        }

        String key = nickname.toLowerCase();

        firestore.collection(COLLECTION)
                .document(key)
                .set(artifacts, SetOptions.merge())
                .get();

        redisTemplate.opsForHash().put(REDIS_KEY, key, artifacts);
    }

    @Override
    public Map<String, Map<String, String>> getPlayerArtifact(String nickname)
            throws ExecutionException, InterruptedException, BadRequestException {

        if (nickname == null || nickname.isEmpty()) {
            nickname = jwtService.extractUsername(jwtService.extractTokenFromRequest());
            if (nickname == null || nickname.isEmpty()) {
                throw new BadRequestException("Nickname cannot be empty");
            }
        }

        String key = nickname.toLowerCase();

        try {
            Object cached = redisTemplate.opsForHash().get(REDIS_KEY, key);

            if (cached != null) {
                Map<String, Map<String, String>> artifacts = objectMapper.convertValue(
                        cached,
                        new TypeReference<>() {}
                );
                return artifacts;
            }
        } catch (Exception e) {
            log.warn("Redis unavailable or error while reading heroes for '{}': {}", key, e.getMessage());
        }

        DocumentSnapshot snapshot = firestore.collection(COLLECTION)
                .document(key)
                .get()
                .get();

        if (!snapshot.exists()) return Collections.emptyMap();

        Map<String, Object> rawData = snapshot.getData();
        Map<String, Map<String, String>> result = new HashMap<>();

        for (Map.Entry<String, Object> entry : Objects.requireNonNull(rawData).entrySet()) {
            String heroId = entry.getKey();
            Map<String, Object> skillMap = (Map<String, Object>) entry.getValue();

            Map<String, String> parsedSkills = new HashMap<>();
            for (Map.Entry<String, Object> skill : skillMap.entrySet()) {
                parsedSkills.put(skill.getKey(), String.valueOf(skill.getValue()));
            }

            result.put(heroId, parsedSkills);
        }

        try {
            redisTemplate.opsForHash().put(REDIS_KEY, key, result);
        } catch (Exception e) {
            log.warn("Redis unavailable — skipping cache save for '{}'", key);
        }

        return result;
    }

    @Override
    public Map<String, Map<String, Map<String, String>>> getAllGuildArtifact() throws BadRequestException, ExecutionException, InterruptedException {
        Map<String, Map<String, Object>> guildMembers;
        Map<String, Map<String, Map<String, String>>> result = new HashMap<>();

        Map<Object, Object> cachedMembers = redisTemplate.opsForHash().entries("guild:members");
        if (cachedMembers.isEmpty()) {
            try {
                DocumentSnapshot snapshot = firestore.collection("guild").document("members").get().get();
                if (!snapshot.exists()) return result;

                Map<String, Object> raw = snapshot.getData();
                guildMembers = new HashMap<>();

                for (Map.Entry<String, Object> entry : raw.entrySet()) {
                    Map<String, Object> info = objectMapper.convertValue(entry.getValue(), new TypeReference<>() {});
                    guildMembers.put(entry.getKey(), info);
                }

                redisTemplate.opsForHash().putAll("guild:members", guildMembers);
            } catch (Exception e) {
                log.error("Failed to fetch guild members from Firestore", e);
                throw new RuntimeException("Internal error");
            }
        } else {
            guildMembers = new HashMap<>();
            for (Map.Entry<Object, Object> entry : cachedMembers.entrySet()) {
                guildMembers.put(
                        String.valueOf(entry.getKey()),
                        objectMapper.convertValue(entry.getValue(), new TypeReference<>() {})
                );
            }
        }

        for (Map.Entry<String, Map<String, Object>> entry : guildMembers.entrySet()) {
            String nickname = entry.getKey();
            Map<String, Object> info = entry.getValue();

            if (Boolean.TRUE.equals(info.get("banned"))) continue;

            String nicknameLC = nickname.toLowerCase();
            Map<String, Map<String, String>> heroesUser = getPlayerArtifact(nicknameLC);

            result.put(nickname, heroesUser);
        }

        return result;
    }
}
