package id.nawala.platform.controller;

import id.nawala.platform.model.AnomalyEvent;
import id.nawala.platform.model.User;
import id.nawala.platform.service.AnomalyDetectionService;
import id.nawala.platform.service.HealthMonitorService;
import id.nawala.platform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class SecurityController {

    private final AnomalyDetectionService anomalyDetectionService;
    private final HealthMonitorService healthMonitorService;
    private final UserService userService;

    @GetMapping("/threats")
    public String threats(Model model) {
        List<AnomalyEvent> events = anomalyDetectionService.getRecentEvents();
        AnomalyDetectionService.ThreatStats stats = anomalyDetectionService.getThreatStats();

        model.addAttribute("events", events);
        model.addAttribute("stats", stats);
        return "security/threats";
    }

    @PostMapping("/threats/{id}/resolve")
    public String resolveEvent(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        anomalyDetectionService.resolveEvent(id, user.getId());
        redirectAttributes.addFlashAttribute("success", "Threat resolved successfully");
        return "redirect:/threats";
    }

    @GetMapping("/health")
    public String health(Model model) {
        HealthMonitorService.HealthSummary summary = healthMonitorService.getHealthSummary();
        List<HealthMonitorService.RouteHealth> routes = healthMonitorService.getAllRouteHealth();

        model.addAttribute("summary", summary);
        model.addAttribute("routes", routes);
        return "security/health";
    }
}
