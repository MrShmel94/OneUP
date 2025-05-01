package OneUP.main.security;

import OneUP.main.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class GuildAccessAspect {

    private final UserService userService;
    private final HttpServletRequest request;

    @Before("@annotation(OneUP.main.security.RequireGuildAccess)")
    public void checkGuildAccess(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireGuildAccess annotation = method.getAnnotation(RequireGuildAccess.class);

        String[] allowedRoles = annotation.roles();

        if (allowedRoles.length == 0) {
            userService.checkAccess("USER");
        } else {
            String required = getMostPrivilegedRole(allowedRoles);
            userService.checkAccess(required);
        }
    }

    private String getMostPrivilegedRole(String[] roles) {
        String[] hierarchy = {"USER", "MODERATOR", "ADMIN"};
        int maxIndex = -1;
        for (String r : roles) {
            for (int i = 0; i < hierarchy.length; i++) {
                if (hierarchy[i].equalsIgnoreCase(r) && i > maxIndex) {
                    maxIndex = i;
                }
            }
        }
        return maxIndex >= 0 ? hierarchy[maxIndex] : "USER";
    }
}
