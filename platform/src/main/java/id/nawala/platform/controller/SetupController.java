package id.nawala.platform.controller;

import id.nawala.platform.model.User;
import id.nawala.platform.service.SetupService;
import id.nawala.platform.service.SetupService.SetupRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequestMapping("/setup")
@RequiredArgsConstructor
@Slf4j
public class SetupController {

    private final SetupService setupService;
    
    // Temporary storage for wizard steps (session-based would be better in production)
    private final Map<String, SetupWizardData> wizardSessions = new ConcurrentHashMap<>();

    @GetMapping
    public String setupPage(HttpServletRequest request, Model model) {
        // If setup already completed, redirect to login
        if (setupService.isSetupCompleted()) {
            return "redirect:/login";
        }
        
        String sessionId = request.getSession().getId();
        SetupWizardData data = wizardSessions.computeIfAbsent(sessionId, k -> new SetupWizardData());
        
        model.addAttribute("currentStep", data.currentStep);
        model.addAttribute("data", data);
        
        return "setup";
    }

    @PostMapping("/step1")
    public String step1(
            @RequestParam String platformName,
            @RequestParam String platformUrl,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        
        if (setupService.isSetupCompleted()) {
            return "redirect:/login";
        }
        
        String sessionId = request.getSession().getId();
        SetupWizardData data = wizardSessions.computeIfAbsent(sessionId, k -> new SetupWizardData());
        
        data.platformName = platformName;
        data.platformUrl = platformUrl;
        data.currentStep = 2;
        
        return "redirect:/setup";
    }

    @PostMapping("/step2")
    public String step2(
            @RequestParam String dbType,
            @RequestParam(required = false) String dbHost,
            @RequestParam(required = false) String dbPort,
            @RequestParam(required = false) String dbName,
            HttpServletRequest request) {
        
        if (setupService.isSetupCompleted()) {
            return "redirect:/login";
        }
        
        String sessionId = request.getSession().getId();
        SetupWizardData data = wizardSessions.computeIfAbsent(sessionId, k -> new SetupWizardData());
        
        data.dbType = dbType;
        data.dbHost = dbHost;
        data.dbPort = dbPort;
        data.dbName = dbName;
        data.currentStep = 3;
        
        return "redirect:/setup";
    }

    @PostMapping("/step3")
    public String step3(
            @RequestParam String adminUsername,
            @RequestParam String adminEmail,
            @RequestParam String adminPassword,
            @RequestParam String adminFullName,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        
        if (setupService.isSetupCompleted()) {
            return "redirect:/login";
        }
        
        // Validation
        if (adminPassword.length() < 8) {
            redirectAttributes.addFlashAttribute("error", "Password must be at least 8 characters");
            return "redirect:/setup";
        }
        
        String sessionId = request.getSession().getId();
        SetupWizardData data = wizardSessions.computeIfAbsent(sessionId, k -> new SetupWizardData());
        
        data.adminUsername = adminUsername;
        data.adminEmail = adminEmail;
        data.adminPassword = adminPassword;
        data.adminFullName = adminFullName;
        data.currentStep = 4;
        
        return "redirect:/setup";
    }

    @PostMapping("/step4")
    public String step4(
            @RequestParam(defaultValue = "false") boolean smtpEnabled,
            @RequestParam(required = false) String smtpHost,
            @RequestParam(defaultValue = "587") int smtpPort,
            @RequestParam(required = false) String smtpUser,
            @RequestParam(required = false) String smtpPass,
            HttpServletRequest request) {
        
        if (setupService.isSetupCompleted()) {
            return "redirect:/login";
        }
        
        String sessionId = request.getSession().getId();
        SetupWizardData data = wizardSessions.computeIfAbsent(sessionId, k -> new SetupWizardData());
        
        data.smtpEnabled = smtpEnabled;
        data.smtpHost = smtpHost;
        data.smtpPort = smtpPort;
        data.smtpUser = smtpUser;
        data.smtpPass = smtpPass;
        data.currentStep = 5; // Ready to complete
        
        return "redirect:/setup";
    }

    @PostMapping("/complete")
    public String complete(HttpServletRequest request, HttpSession session, RedirectAttributes redirectAttributes) {
        if (setupService.isSetupCompleted()) {
            return "redirect:/login";
        }
        
        String sessionId = request.getSession().getId();
        SetupWizardData data = wizardSessions.get(sessionId);
        
        if (data == null || data.currentStep < 5) {
            redirectAttributes.addFlashAttribute("error", "Please complete all steps first");
            return "redirect:/setup";
        }
        
        try {
            // Create setup request
            SetupRequest setupRequest = new SetupRequest(
                    data.platformName,
                    data.platformUrl,
                    data.dbType,
                    data.adminUsername,
                    data.adminEmail,
                    data.adminPassword,
                    data.adminFullName,
                    data.smtpEnabled,
                    data.smtpHost,
                    data.smtpPort,
                    data.smtpUser,
                    data.smtpPass
            );
            
            // Complete setup
            User admin = setupService.completeSetup(setupRequest);
            
            // Auto-login the admin
            var authorities = List.of(new SimpleGrantedAuthority(admin.getRole().getAuthority()));
            var auth = new UsernamePasswordAuthenticationToken(admin.getUsername(), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
            
            // Clean up wizard data
            wizardSessions.remove(sessionId);
            
            log.info("Setup completed, admin logged in: {}", admin.getUsername());
            redirectAttributes.addFlashAttribute("success", "Setup completed! Welcome to Nawala Gateway.");
            
            return "redirect:/dashboard";
            
        } catch (Exception e) {
            log.error("Setup failed", e);
            redirectAttributes.addFlashAttribute("error", "Setup failed: " + e.getMessage());
            return "redirect:/setup";
        }
    }

    @PostMapping("/back")
    public String back(HttpServletRequest request) {
        String sessionId = request.getSession().getId();
        SetupWizardData data = wizardSessions.get(sessionId);
        
        if (data != null && data.currentStep > 1) {
            data.currentStep--;
        }
        
        return "redirect:/setup";
    }

    // Wizard data holder
    private static class SetupWizardData {
        int currentStep = 1;
        // Step 1
        String platformName = "Nawala Gateway";
        String platformUrl = "http://localhost:8080";
        // Step 2
        String dbType = "h2";
        String dbHost;
        String dbPort;
        String dbName;
        // Step 3
        String adminUsername;
        String adminEmail;
        String adminPassword;
        String adminFullName;
        // Step 4
        boolean smtpEnabled = false;
        String smtpHost;
        int smtpPort = 587;
        String smtpUser;
        String smtpPass;
    }
}
