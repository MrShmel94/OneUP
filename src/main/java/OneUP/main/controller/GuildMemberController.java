package OneUP.main.controller;

import OneUP.main.model.GuildMember;
import OneUP.main.request.ChangeRoleRequest;
import OneUP.main.request.PlayerDataRequest;
import OneUP.main.request.ToggleBanRequest;
import OneUP.main.service.GuildMembersService;
import OneUP.main.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/guild/member")
@RequiredArgsConstructor
public class GuildMemberController {

    private final UserService userService;
    private final GuildMembersService guildMembersService;

    @GetMapping("/getAllMembers")
    public ResponseEntity<List<GuildMember>> getAllMembers() {
        return ResponseEntity.ok(guildMembersService.getAllMembers());
    }

    @PostMapping("/changeRole")
    public ResponseEntity<Void> confirm(@RequestBody @Valid ChangeRoleRequest request) {
        userService.changeUserRole(request.nickname(), request.role());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/toggleBan")
    public ResponseEntity<Void> banned(@RequestBody @Valid ToggleBanRequest request) {
        if(request.isBanned()){
            userService.banned(request.nickname());
        }else{
            userService.unbanned(request.nickname());
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/saveMainInfo")
    public ResponseEntity<Void> save(@RequestBody PlayerDataRequest request) {
        guildMembersService.savePlayerData(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/getMainInfoForAll")
    public ResponseEntity<Map<String, PlayerDataRequest>> getAll() {
        return ResponseEntity.ok(guildMembersService.getAllPlayersData());
    }

    @GetMapping("/{nickname}")
    public ResponseEntity<PlayerDataRequest> getOne(@PathVariable String nickname) {
        return ResponseEntity.ok(guildMembersService.getPlayerData(nickname));
    }

    @GetMapping("/getMainInfo")
    public ResponseEntity<PlayerDataRequest> getOneForCurrentUser() {
        return ResponseEntity.ok(guildMembersService.getPlayerData());
    }
}
