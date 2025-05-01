package OneUP.main.controller;

import OneUP.main.service.HeroesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/heroes")
@RequiredArgsConstructor
public class HeroesController {

    private final HeroesService heroesService;

    @GetMapping("/{nickname}")
    public ResponseEntity<?> getHeroes(@PathVariable String nickname) {
        try {
            return ResponseEntity.ok(heroesService.getPlayerHeroes(nickname));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to fetch heroes");
        }
    }

    @PostMapping("/save/{nickname}")
    public ResponseEntity<?> saveHeroes(@PathVariable String nickname,
                                        @RequestBody Map<String, Map<String, String>> heroes) {
        try {
            heroesService.savePlayerHeroes(nickname, heroes);
            return ResponseEntity.ok("Saved");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to save heroes");
        }
    }

    @GetMapping()
    public ResponseEntity<?> getHeroes() {
        try {
            return ResponseEntity.ok(heroesService.getPlayerHeroes(null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to fetch heroes");
        }
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveHeroes(@RequestBody Map<String, Map<String, String>> heroes) {
        try {
            heroesService.savePlayerHeroes(null, heroes);
            return ResponseEntity.ok("Saved");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to save heroes");
        }
    }
}
