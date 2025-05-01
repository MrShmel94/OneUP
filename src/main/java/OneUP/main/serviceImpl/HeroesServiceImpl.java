package OneUP.main.serviceImpl;

import OneUP.main.service.HeroesService;
import OneUP.main.service.JwtService;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class HeroesServiceImpl implements HeroesService {

    private final Firestore firestore = FirestoreClient.getFirestore();
    private final JwtService jwtService;
    private static final String COLLECTION = "heroes";

    @Override
    public void savePlayerHeroes(String nickname, Map<String, Map<String, String>> heroes) throws ExecutionException, InterruptedException, BadRequestException {

        if(nickname == null || nickname.isEmpty()) {
            nickname = jwtService.extractUsername(jwtService.extractTokenFromRequest());
            if(nickname == null || nickname.isEmpty()) {
                throw new BadRequestException("Nickname cannot be empty");
            }
        }

        firestore.collection(COLLECTION)
                .document(nickname.toLowerCase())
                .set(heroes)
                .get();
    }

    @Override
    public Map<String, Map<String, String>> getPlayerHeroes(String nickname) throws ExecutionException, InterruptedException, BadRequestException {

        if(nickname == null || nickname.isEmpty()) {
            nickname = jwtService.extractUsername(jwtService.extractTokenFromRequest());
            if(nickname == null || nickname.isEmpty()) {
                throw new BadRequestException("Nickname cannot be empty");
            }
        }

        DocumentSnapshot snapshot = firestore.collection(COLLECTION)
                .document(nickname.toLowerCase())
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

        return result;
    }
}
