package id.nawala.platform.sso;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/sso")
@RequiredArgsConstructor
public class SsoController {

    private final SsoService ssoService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("configs", ssoService.getAllConfigs());
        model.addAttribute("ssoTypes", SsoConfig.SsoType.values());
        return "admin/sso";
    }

    @PostMapping
    public String create(@ModelAttribute SsoConfig config, RedirectAttributes ra) {
        ssoService.save(config);
        ra.addFlashAttribute("success", "SSO config created!");
        return "redirect:/admin/sso";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        ssoService.getAllConfigs().stream()
            .filter(c -> c.getId().equals(id))
            .findFirst()
            .ifPresent(c -> {
                c.setEnabled(!c.isEnabled());
                ssoService.save(c);
            });
        ra.addFlashAttribute("success", "SSO config toggled!");
        return "redirect:/admin/sso";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        ssoService.delete(id);
        ra.addFlashAttribute("success", "SSO config deleted!");
        return "redirect:/admin/sso";
    }
}
