package OneUP.main.controller;

import OneUP.main.service.ArtifactService;
import OneUP.main.service.HeroesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/artifact")
@RequiredArgsConstructor
public class ArtifactController {

    private final ArtifactService artifactServiceService;

    @GetMapping("/{nickname}")
    public ResponseEntity<?> getArtifact(@PathVariable String nickname) {
        try {
            return ResponseEntity.ok(artifactServiceService.getPlayerArtifact(nickname));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to fetch heroes");
        }
    }

    @PostMapping("/save/{nickname}")
    public ResponseEntity<?> saveArtifact(@PathVariable String nickname,
                                        @RequestBody Map<String, Map<String, String>> artifacts) {
        try {
            artifactServiceService.savePlayerArtifact(nickname, artifacts);
            return ResponseEntity.ok("Saved");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to save heroes");
        }
    }

    @GetMapping()
    public ResponseEntity<?> getArtifact() {
        try {
            return ResponseEntity.ok(artifactServiceService.getPlayerArtifact(null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to fetch heroes");
        }
    }

    @GetMapping("/getAllGuildArtifact")
    public ResponseEntity<?> getAllGuildArtifact() {
        try {
            return ResponseEntity.ok(artifactServiceService.getAllGuildArtifact());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to fetch heroes");
        }
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveArtifact(@RequestBody Map<String, Map<String, String>> artifacts) {
        try {
            artifactServiceService.savePlayerArtifact(null, artifacts);
            return ResponseEntity.ok("Saved");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to save heroes");
        }
    }
}
