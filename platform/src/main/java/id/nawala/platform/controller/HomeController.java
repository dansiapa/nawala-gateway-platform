package id.nawala.platform.controller;

import id.nawala.platform.service.SetupService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final SetupService setupService;

    @GetMapping("/")
    public String home(Authentication auth) {
        // If setup not completed, redirect to setup
        if (!setupService.isSetupCompleted()) {
            return "redirect:/setup";
        }
        
        // If authenticated, go to dashboard
        if (auth != null && auth.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        
        // Otherwise, go to login
        return "redirect:/login";
    }
}
