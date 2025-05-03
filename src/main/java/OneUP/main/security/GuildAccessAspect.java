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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class GuildAccessAspect {

    private final UserService userService;

    @Before("@annotation(requireGuildAccess)")
    public void checkGuildAccess(RequireGuildAccess requireGuildAccess) {
        Role[] roles = requireGuildAccess.roles();
        Role highestRequired = Arrays.stream(roles)
                .max(Comparator.comparingInt(Role::ordinal))
                .orElse(Role.USER);

        userService.checkAccess(highestRequired);
    }
}
