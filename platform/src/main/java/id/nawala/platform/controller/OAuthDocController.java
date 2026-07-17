package id.nawala.platform.controller;

import id.nawala.platform.model.ApiDoc;
import id.nawala.platform.model.OAuthClient;
import id.nawala.platform.model.User;
import id.nawala.platform.service.ApiDocService;
import id.nawala.platform.service.OAuthService;
import id.nawala.platform.service.UserService;
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
public class OAuthDocController {

    private final OAuthService oAuthService;
    private final ApiDocService apiDocService;
    private final UserService userService;

    // ==================== OAuth Clients ====================

    @GetMapping("/oauth-clients")
    public String oauthClients(@AuthenticationPrincipal UserDetails ud, Model model) {
        User user = userService.findByUsername(ud.getUsername()).orElseThrow();
        List<OAuthClient> clients = oAuthService.getClientsByUser(user.getId());
        model.addAttribute("clients", clients);
        return "advanced/oauth-clients";
    }

    @PostMapping("/oauth-clients")
    public String createClient(@AuthenticationPrincipal UserDetails ud,
                               @RequestParam String name,
                               @RequestParam(defaultValue = "client_credentials") String grantTypes,
                               @RequestParam(defaultValue = "read") String scopes,
                               @RequestParam(required = false) String redirectUris,
                               RedirectAttributes ra) {
        User user = userService.findByUsername(ud.getUsername()).orElseThrow();
        OAuthClient client = oAuthService.registerClient(user.getId(), name, grantTypes, scopes, redirectUris);
        ra.addFlashAttribute("success", "OAuth client created. Secret: " + client.getClientSecretHash());
        return "redirect:/oauth-clients";
    }

    @PostMapping("/oauth-clients/{id}/delete")
    public String deleteClient(@PathVariable Long id, RedirectAttributes ra) {
        oAuthService.deleteClient(id);
        ra.addFlashAttribute("success", "Client deleted");
        return "redirect:/oauth-clients";
    }

    // ==================== API Docs ====================

    @GetMapping("/docs")
    public String docs(@AuthenticationPrincipal UserDetails ud, Model model) {
        User user = userService.findByUsername(ud.getUsername()).orElseThrow();
        model.addAttribute("docs", apiDocService.getByUser(user.getId()));
        return "advanced/docs";
    }

    @GetMapping("/docs/public")
    public String publicDocs(Model model) {
        model.addAttribute("docs", apiDocService.getPublished());
        return "advanced/docs-public";
    }

    @GetMapping("/docs/{id}/view")
    public String viewDoc(@PathVariable Long id, Model model,
                          @AuthenticationPrincipal UserDetails ud) {
        ApiDoc doc = apiDocService.getById(id);
        if (doc == null) return "redirect:/docs";
        // Public access only allowed for published docs
        if (!doc.isPublished() && ud == null) {
            return "redirect:/docs/public";
        }
        model.addAttribute("doc", doc);
        return "advanced/doc-view";
    }

    @PostMapping("/docs")
    public String createDoc(@AuthenticationPrincipal UserDetails ud,
                            @RequestParam String title,
                            @RequestParam(required = false) Long routeId,
                            @RequestParam(defaultValue = "1.0.0") String version,
                            @RequestParam String openApiSpec,
                            @RequestParam(required = false) String description,
                            RedirectAttributes ra) {
        User user = userService.findByUsername(ud.getUsername()).orElseThrow();
        apiDocService.create(user.getId(), routeId, title, version, openApiSpec, description);
        ra.addFlashAttribute("success", "API Documentation created");
        return "redirect:/docs";
    }

    @PostMapping("/docs/{id}/publish")
    public String publishDoc(@PathVariable Long id, @RequestParam boolean published, RedirectAttributes ra) {
        apiDocService.publish(id, published);
        ra.addFlashAttribute("success", published ? "Published" : "Unpublished");
        return "redirect:/docs";
    }

    @PostMapping("/docs/{id}/delete")
    public String deleteDoc(@PathVariable Long id, RedirectAttributes ra) {
        apiDocService.delete(id);
        ra.addFlashAttribute("success", "Doc deleted");
        return "redirect:/docs";
    }
}
