package id.nawala.platform.config;

import id.nawala.platform.service.ActivityLogService;
import id.nawala.platform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationEventListener {

    private final UserService userService;
    private final ActivityLogService activityLogService;

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        userService.updateLastLogin(username);
        userService.findByUsername(username).ifPresent(user ->
                activityLogService.log(user, "LOGIN", "Signed in successfully", null)
        );
    }
}
