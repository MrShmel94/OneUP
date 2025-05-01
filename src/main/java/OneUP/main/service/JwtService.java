package OneUP.main.service;

public interface JwtService {

    String generateToken(String username);
    boolean isTokenValid(String token);
    String extractUsername(String token);
    String extractTokenFromRequest();
    String extractRole(String token);
    String generateToken(String username, String role);

    void setJwtCookieInResponse(String token);
    void clearJwtFromResponse();
}
