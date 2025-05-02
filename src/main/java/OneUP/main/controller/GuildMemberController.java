package OneUP.main.controller;

import OneUP.main.model.GuildMember;
import OneUP.main.request.ChangeRoleRequest;
import OneUP.main.service.GuildMembersService;
import OneUP.main.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/banned/{nickname}")
    public ResponseEntity<Void> banned(@PathVariable String nickname) {
        userService.banned(nickname);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unbanned/{nickname}")
    public ResponseEntity<Void> unbanned(@PathVariable String nickname) {
        userService.unbanned(nickname);
        return ResponseEntity.noContent().build();
    }
}
