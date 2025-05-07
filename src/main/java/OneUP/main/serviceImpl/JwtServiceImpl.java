package OneUP.main.serviceImpl;

import OneUP.main.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {


    @Value("${jwt.secret}")
    private String secret;

    private SecretKey key;
    private final Duration tokenLifetime = Duration.ofDays(1);

    private final HttpServletRequest request;
    private final HttpServletResponse response;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username, String role) {
        Claims claims = Jwts.claims()
                .subject(username)
                .add("role", role)
                .build();

        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + tokenLifetime.toMillis()))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public String generateToken(String username) {
        return generateToken(username, "USER");
    }

    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public String extractTokenFromRequest() {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        throw new RuntimeException("JWT token not found in request");
    }

    public String extractUsername(String token) {

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public String extractRole(String token) {
        return (String) Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role");
    }

    public void setJwtCookieInResponse(String token) {
        String domain = request.getServerName().contains("dev.") ? "dev.1uppower.club" : "1uppower.club";

        Cookie cookie = new Cookie("token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setDomain(domain);
        cookie.setMaxAge((int) tokenLifetime.getSeconds());
        response.addCookie(cookie);
    }

    public void clearJwtFromResponse() {
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
    }
}
