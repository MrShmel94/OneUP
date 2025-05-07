package OneUP.main.service;

import OneUP.main.request.SavePlanRequest;
import OneUP.main.response.PlanResponse;

import java.util.List;

public interface GuildService {

    void savePlan(SavePlanRequest request);
    List<PlanResponse> getPlanListForMap(String nameMap);
}
