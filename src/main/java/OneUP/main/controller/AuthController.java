package OneUP.main.controller;

import OneUP.main.request.ConfirmRequest;
import OneUP.main.request.LoginRequest;
import OneUP.main.request.RegisterRequest;
import OneUP.main.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterRequest request) {
        userService.registerUser(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/confirm")
    public ResponseEntity<Void> confirm(@RequestBody @Valid ConfirmRequest request, HttpServletResponse response) {
        String token = userService.confirmUser(request);
        setTokenCookie(response, token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody @Valid LoginRequest request, HttpServletResponse response) {
        String token = userService.login(request);
        setTokenCookie(response, token);
        return ResponseEntity.noContent().build();
    }


    private void setTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("token", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) Duration.ofDays(1).getSeconds());
        cookie.setSecure(true);
        response.addCookie(cookie);
    }
}
