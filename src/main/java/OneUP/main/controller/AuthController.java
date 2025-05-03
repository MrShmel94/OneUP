package OneUP.main.controller;

import OneUP.main.request.ConfirmRequest;
import OneUP.main.request.LoginRequest;
import OneUP.main.request.RegisterRequest;
import OneUP.main.response.UserResponse;
import OneUP.main.security.Role;
import OneUP.main.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Void> confirm(@RequestBody @Valid ConfirmRequest requestObject, HttpServletResponse response, HttpServletRequest request) {
        String token = userService.confirmUser(requestObject);
        setTokenCookie(request, response, token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody @Valid LoginRequest requestObject, HttpServletResponse response, HttpServletRequest request) {
        String token = userService.login(requestObject);
        setTokenCookie(request, response, token);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = authentication.getName();
        String role = userService.checkAccess(Role.USER).toString();
        return ResponseEntity.ok(new UserResponse(username, role));
    }


    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {

        String domain = request.getServerName().contains("dev.") ? "dev.1uppower.club" : "1uppower.club";

        ResponseCookie deleteToken = ResponseCookie.from("token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .domain(domain)
                .maxAge(0)
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, deleteToken.toString());

        return ResponseEntity.ok().build();
    }


    private void setTokenCookie(HttpServletRequest request, HttpServletResponse response, String token) {

        String domain = request.getServerName().contains("dev.") ? "dev.1uppower.club" : "1uppower.club";

        Cookie cookie = new Cookie("token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setDomain(domain);
        cookie.setMaxAge((int) Duration.ofDays(1).getSeconds());
        response.addCookie(cookie);
    }
}
