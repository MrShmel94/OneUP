package OneUP.main.service;

public interface JwtService {

    String generateToken(String username);
    boolean isTokenValid(String token);
    String extractUsername(String token);
    String extractTokenFromRequest();
}
