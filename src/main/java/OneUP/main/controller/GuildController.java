package OneUP.main.controller;

import OneUP.main.request.SavePlanRequest;
import OneUP.main.service.GuildService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/guild")
public class GuildController {

    private final GuildService guildService;

    @PostMapping("/savePlan")
    ResponseEntity<?> savePlan(@Valid @RequestBody SavePlanRequest model){
        guildService.savePlan(model);
        return ResponseEntity.ok().body("success");
    }

    @GetMapping("/getPlan/{nameMap}")
    ResponseEntity<?> getPlan(@PathVariable String nameMap){
        return ResponseEntity.ok(guildService.getPlanListForMap(nameMap));
    }
}
