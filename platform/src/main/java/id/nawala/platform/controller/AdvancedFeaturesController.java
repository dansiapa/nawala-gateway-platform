package id.nawala.platform.controller;

import id.nawala.platform.model.*;
import id.nawala.platform.repository.*;
import id.nawala.platform.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class AdvancedFeaturesController {

    private final UserRepository userRepository;
    private final WafRuleRepository wafRuleRepository;
    private final WebhookRepository webhookRepository;
    private final OAuthClientRepository oauthClientRepository;
    private final PluginRepository pluginRepository;
    private final ApiMockRepository mockRepository;
    private final ApiDocRepository docRepository;
    private final WafService wafService;
    private final WebhookService webhookService;
    private final OAuthService oauthService;
    private final PluginService pluginService;
    private final MockService mockService;
    private final ApiDocService docService;
    private final AuditService auditService;

    // ========== WAF ==========
    @GetMapping("/waf")
    public String waf(Model model) {
        model.addAttribute("rules", wafRuleRepository.findAll());
        model.addAttribute("blockedIps", wafService.getBlockedIps());
        model.addAttribute("wafEnabled", wafService.isEnabled());
        return "advanced/waf";
    }

    @PostMapping("/waf/toggle")
    public String toggleWaf(@RequestParam String feature, @RequestParam boolean enabled,
                           @AuthenticationPrincipal UserDetails userDetails,
                           RedirectAttributes redirectAttributes) {
        wafService.toggleFeature(feature, enabled);
        auditService.log("UPDATE", "WAF", "WAF " + feature + " " + (enabled ? "enabled" : "disabled"), userDetails.getUsername());
        redirectAttributes.addFlashAttribute("success", "WAF settings updated");
        return "redirect:/waf";
    }

    @PostMapping("/waf/block-ip")
    public String blockIp(@RequestParam String ip, @RequestParam(required = false) String reason,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        wafService.blockIp(ip, reason);
        auditService.log("CREATE", "WAF", "Blocked IP: " + ip, userDetails.getUsername());
        redirectAttributes.addFlashAttribute("success", "IP blocked successfully");
        return "redirect:/waf";
    }

    @PostMapping("/waf/unblock-ip")
    public String unblockIp(@RequestParam String ip,
                           @AuthenticationPrincipal UserDetails userDetails,
                           RedirectAttributes redirectAttributes) {
        wafService.unblockIp(ip);
        auditService.log("DELETE", "WAF", "Unblocked IP: " + ip, userDetails.getUsername());
        redirectAttributes.addFlashAttribute("success", "IP unblocked");
        return "redirect:/waf";
    }

    // ========== WEBHOOKS ==========
    @GetMapping("/webhooks")
    public String webhooks(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        model.addAttribute("webhooks", webhookRepository.findByOwner(user));
        return "advanced/webhooks";
    }

    @PostMapping("/webhooks/create")
    public String createWebhook(@RequestParam String name, @RequestParam String url,
                               @RequestParam(required = false) List<String> events,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        webhookService.createWebhook(user, name, url, events != null ? events : List.of());
        auditService.log("CREATE", "WEBHOOK", "Created webhook: " + name, userDetails.getUsername());
        redirectAttributes.addFlashAttribute("success", "Webhook created successfully");
        return "redirect:/webhooks";
    }

    @PostMapping("/webhooks/{id}/delete")
    public String deleteWebhook(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        webhookService.deleteWebhook(id);
        auditService.log("DELETE", "WEBHOOK", "Deleted webhook #" + id, userDetails.getUsername());
        redirectAttributes.addFlashAttribute("success", "Webhook deleted");
        return "redirect:/webhooks";
    }

    @PostMapping("/webhooks/{id}/test")
    public String testWebhook(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        boolean success = webhookService.testWebhook(id);
        redirectAttributes.addFlashAttribute(success ? "success" : "error",
                success ? "Test webhook sent successfully" : "Webhook test failed");
        return "redirect:/webhooks";
    }

    // ========== OAUTH CLIENTS ==========
    @GetMapping("/oauth-clients")
    public String oauthClients(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        model.addAttribute("clients", oauthClientRepository.findByOwner(user));
        return "advanced/oauth-clients";
    }

    @PostMapping("/oauth-clients/create")
    public String createOAuthClient(@RequestParam String name, @RequestParam String redirectUri,
                                   @RequestParam(required = false) List<String> scopes,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        OAuthClient client = oauthService.createClient(user, name, redirectUri, scopes != null ? scopes : List.of());
        auditService.log("CREATE", "OAUTH", "Created OAuth client: " + name, userDetails.getUsername());
        redirectAttributes.addFlashAttribute("success", "OAuth client created");
        redirectAttributes.addFlashAttribute("clientSecret", client.getClientSecret()); // Show once
        return "redirect:/oauth-clients";
    }

    @PostMapping("/oauth-clients/{id}/regenerate")
    public String regenerateSecret(@PathVariable Long id,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        String newSecret = oauthService.regenerateSecret(id);
        auditService.log("UPDATE", "OAUTH", "Regenerated secret for client #" + id, userDetails.getUsername());
        redirectAttributes.addFlashAttribute("success", "Client secret regenerated");
        redirectAttributes.addFlashAttribute("clientSecret", newSecret);
        return "redirect:/oauth-clients";
    }

    @PostMapping("/oauth-clients/{id}/delete")
    public String deleteOAuthClient(@PathVariable Long id,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        oauthService.deleteClient(id);
        auditService.log("DELETE", "OAUTH", "Deleted OAuth client #" + id, userDetails.getUsername());
        redirectAttributes.addFlashAttribute("success", "OAuth client deleted");
        return "redirect:/oauth-clients";
    }

    // ========== PLUGINS ==========
    @GetMapping("/plugins")
    public String plugins(Model model) {
        model.addAttribute("plugins", pluginRepository.findAll());
        model.addAttribute("availablePlugins", pluginService.getAvailablePlugins());
        return "advanced/plugins";
    }

    @PostMapping("/plugins/{id}/toggle")
    public String togglePlugin(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        Plugin plugin = pluginService.togglePlugin(id);
        String action = plugin.isEnabled() ? "enabled" : "disabled";
        auditService.log("UPDATE", "PLUGIN", "Plugin " + action + ": " + plugin.getName(), userDetails.getUsername());
        redirectAttributes.addFlashAttribute("success", "Plugin " + action);
        return "redirect:/plugins";
    }

    // ========== MOCKS (API Sandbox) ==========
    @GetMapping("/mocks")
    public String mocks(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        model.addAttribute("mocks", mockRepository.findByCreatedBy(user));
        return "advanced/mocks";
    }

    @PostMapping("/mocks/create")
    public String createMock(@RequestParam String name, @RequestParam String method,
                            @RequestParam String path, @RequestParam int statusCode,
                            @RequestParam String responseBody,
                            @RequestParam(required = false) String contentType,
                            @AuthenticationPrincipal UserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        mockService.createMock(user, name, method, path, statusCode, responseBody, 
                contentType != null ? contentType : "application/json");
        auditService.log("CREATE", "MOCK", "Created mock: " + name, userDetails.getUsername());
        redirectAttributes.addFlashAttribute("success", "Mock endpoint created");
        return "redirect:/mocks";
    }

    @PostMapping("/mocks/{id}/delete")
    public String deleteMock(@PathVariable Long id,
                            @AuthenticationPrincipal UserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
        mockService.deleteMock(id);
        auditService.log("DELETE", "MOCK", "Deleted mock #" + id, userDetails.getUsername());
        redirectAttributes.addFlashAttribute("success", "Mock deleted");
        return "redirect:/mocks";
    }

    // ========== API DOCS ==========
    @GetMapping("/docs")
    public String docs(Model model) {
        model.addAttribute("apiGroups", docService.getApiGroups());
        return "advanced/docs";
    }

    @PostMapping("/docs/generate")
    public String generateDocs(@AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        docService.regenerateDocumentation();
        auditService.log("CREATE", "DOCS", "Regenerated API documentation", userDetails.getUsername());
        redirectAttributes.addFlashAttribute("success", "Documentation regenerated");
        return "redirect:/docs";
    }

    @GetMapping("/docs/public")
    public String publicDocs(Model model) {
        model.addAttribute("apiGroups", docService.getApiGroups());
        return "advanced/docs-public";
    }
}
