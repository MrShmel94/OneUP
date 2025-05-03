package OneUP.main.service;


import OneUP.main.model.AppUser;
import OneUP.main.request.ConfirmRequest;
import OneUP.main.request.LoginRequest;
import OneUP.main.request.RegisterRequest;
import OneUP.main.security.Role;

public interface UserService {

    void registerUser(RegisterRequest dto);
    AppUser getUser(String nickname);
    boolean passwordMatches(String rawPassword, String encodedPassword);
    String login(LoginRequest dto);
    String confirmUser(ConfirmRequest requestModel);
    Role checkAccess(Role requiredRole);
    void changeUserRole(String nickname, String newRole);

    void banned (String nickname);
    void unbanned (String nickname);
}
