package id.nawala.platform.controller;

import id.nawala.platform.model.ApiMock;
import id.nawala.platform.model.User;
import id.nawala.platform.model.WafRule;
import id.nawala.platform.model.Webhook;
import id.nawala.platform.service.*;
import id.nawala.platform.service.AnalyticsService.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class AdvancedFeaturesController {

    private final AnalyticsService analyticsService;
    private final WafService wafService;
    private final WebhookService webhookService;
    private final MockService mockService;
    private final UserService userService;

    @GetMapping("/analytics")
    public String analytics(Model model) {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        AnalyticsSummary summary = analyticsService.getSummary(since);
        List<RouteAnalytics> topRoutes = analyticsService.getTopRoutes(since, 10);
        Map<Integer, Long> statusDist = analyticsService.getStatusDistribution(since);
        Map<Integer, Long> hourly = analyticsService.getHourlyTraffic(since);
        Map<String, Long> geo = analyticsService.getGeoDistribution(since);
        model.addAttribute("summary", summary);
        model.addAttribute("topRoutes", topRoutes);
        model.addAttribute("statusDist", statusDist);
        model.addAttribute("hourlyTraffic", hourly);
        model.addAttribute("geoData", geo);
        return "advanced/analytics";
    }

    @GetMapping("/waf")
    public String wafRules(Model model) {
        model.addAttribute("rules", wafService.getAllActiveRules());
        return "advanced/waf";
    }

    @PostMapping("/waf/rules")
    public String createWafRule(@RequestParam String name, @RequestParam String ruleType,
                                @RequestParam String pattern, @RequestParam String action,
                                @RequestParam String targetField, @RequestParam(required = false) Long routeId,
                                @RequestParam(defaultValue = "100") int priority,
                                @RequestParam(required = false) String description,
                                RedirectAttributes ra) {
        wafService.createRule(name, ruleType, pattern, action, targetField, routeId, priority, description);
        ra.addFlashAttribute("success", "WAF rule created");
        return "redirect:/waf";
    }

    @PostMapping("/waf/rules/{id}/delete")
    public String deleteWafRule(@PathVariable Long id, RedirectAttributes ra) {
        wafService.deleteRule(id);
        ra.addFlashAttribute("success", "Rule deleted");
        return "redirect:/waf";
    }

    @PostMapping("/waf/rules/{id}/toggle")
    public String toggleWafRule(@PathVariable Long id, @RequestParam boolean active, RedirectAttributes ra) {
        wafService.toggleRule(id, active);
        ra.addFlashAttribute("success", "Rule updated");
        return "redirect:/waf";
    }

    @GetMapping("/webhooks")
    public String webhooks(@AuthenticationPrincipal UserDetails ud, Model model) {
        User user = userService.findByUsername(ud.getUsername()).orElseThrow();
        model.addAttribute("webhooks", webhookService.getByUser(user.getId()));
        return "advanced/webhooks";
    }

    @PostMapping("/webhooks")
    public String createWebhook(@AuthenticationPrincipal UserDetails ud,
                                @RequestParam String name, @RequestParam String targetUrl,
                                @RequestParam String eventType, @RequestParam(required = false) String secret,
                                RedirectAttributes ra) {
        User user = userService.findByUsername(ud.getUsername()).orElseThrow();
        webhookService.create(user.getId(), name, targetUrl, eventType, secret);
        ra.addFlashAttribute("success", "Webhook created");
        return "redirect:/webhooks";
    }

    @PostMapping("/webhooks/{id}/delete")
    public String deleteWebhook(@PathVariable Long id, RedirectAttributes ra) {
        webhookService.delete(id);
        ra.addFlashAttribute("success", "Webhook deleted");
        return "redirect:/webhooks";
    }

    @GetMapping("/mocks")
    public String mocks(@AuthenticationPrincipal UserDetails ud, Model model) {
        User user = userService.findByUsername(ud.getUsername()).orElseThrow();
        model.addAttribute("mocks", mockService.getByUser(user.getId()));
        return "advanced/mocks";
    }

    @PostMapping("/mocks")
    public String createMock(@AuthenticationPrincipal UserDetails ud,
                             @RequestParam String name, @RequestParam String path,
                             @RequestParam String method, @RequestParam int statusCode,
                             @RequestParam String responseBody,
                             @RequestParam(defaultValue = "application/json") String contentType,
                             @RequestParam(defaultValue = "0") int delayMs,
                             RedirectAttributes ra) {
        User user = userService.findByUsername(ud.getUsername()).orElseThrow();
        mockService.create(user.getId(), name, path, method, statusCode, responseBody, contentType, delayMs);
        ra.addFlashAttribute("success", "Mock endpoint created");
        return "redirect:/mocks";
    }

    @PostMapping("/mocks/{id}/delete")
    public String deleteMock(@PathVariable Long id, RedirectAttributes ra) {
        mockService.delete(id);
        ra.addFlashAttribute("success", "Mock deleted");
        return "redirect:/mocks";
    }

    @PostMapping("/mocks/{id}/toggle")
    public String toggleMock(@PathVariable Long id, @RequestParam boolean active, RedirectAttributes ra) {
        mockService.toggle(id, active);
        ra.addFlashAttribute("success", "Mock updated");
        return "redirect:/mocks";
    }
}
