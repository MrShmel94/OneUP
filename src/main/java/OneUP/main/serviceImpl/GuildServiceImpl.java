package OneUP.main.serviceImpl;

import OneUP.main.request.SavePlanRequest;
import OneUP.main.response.PlanResponse;
import OneUP.main.security.RequireGuildAccess;
import OneUP.main.security.Role;
import OneUP.main.service.GuildService;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuildServiceImpl implements GuildService {

    private final Firestore firestore = FirestoreClient.getFirestore();
    private final JwtServiceImpl jwtService;


    @Override
    @RequireGuildAccess(roles = {Role.MODERATOR, Role.ADMIN})
    public void savePlan(SavePlanRequest request) {
        String currentUser = jwtService.extractUsername(jwtService.extractTokenFromRequest());

        try {
            Map<String, Object> plan = new HashMap<>();
            plan.put("nameCreator", currentUser);
            plan.put("name", request.name());
            plan.put("rowData", request.rowData());
            plan.put("dateCreate", LocalDate.now().toString());

            firestore.collection("plans")
                    .document(request.nameMap())
                    .update("list", FieldValue.arrayUnion(plan))
                    .get();

        } catch (Exception e) {
            Map<String, Object> plan = new HashMap<>();
            plan.put("nameCreator", currentUser);
            plan.put("name", request.name());
            plan.put("rowData", request.rowData());
            plan.put("dateCreate", LocalDate.now().toString());

            List<Map<String, Object>> initialList = List.of(plan);
            Map<String, Object> doc = Map.of("list", initialList);

            try {
                firestore.collection("plans")
                        .document(request.nameMap())
                        .set(doc)
                        .get();
            } catch (Exception ex) {
                throw new RuntimeException("Failed to create document", ex);
            }
        }
    }

    @Override
    public List<PlanResponse> getPlanListForMap(String nameMap) {
        try {
            DocumentSnapshot snapshot = firestore.collection("plans").document(nameMap).get().get();

            if (!snapshot.exists()) {
                return List.of();
            }

            List<Map<String, Object>> list = (List<Map<String, Object>>) snapshot.get("list");

            if (list == null) return List.of();

            return list.stream()
                    .map(item -> new PlanResponse(
                            (String) item.get("nameCreator"),
                            (String) item.get("name"),
                            (String) item.get("rowData"),
                            LocalDate.parse((String) item.get("dateCreate"))
                    ))
                    .toList();

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
