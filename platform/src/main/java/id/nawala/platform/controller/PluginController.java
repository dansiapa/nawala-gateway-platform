package id.nawala.platform.controller;

import id.nawala.platform.model.Plugin;
import id.nawala.platform.model.User;
import id.nawala.platform.service.PluginService;
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
public class PluginController {

    private final PluginService pluginService;
    private final UserService userService;

    @GetMapping("/plugins")
    public String plugins(@AuthenticationPrincipal UserDetails ud, Model model) {
        User user = userService.findByUsername(ud.getUsername()).orElseThrow();
        List<Plugin> plugins = pluginService.getByUser(user.getId());
        model.addAttribute("plugins", plugins);
        return "advanced/plugins";
    }

    @PostMapping("/plugins")
    public String createPlugin(@AuthenticationPrincipal UserDetails ud,
                               @RequestParam String name,
                               @RequestParam(required = false) String description,
                               @RequestParam String hookType,
                               @RequestParam String script,
                               @RequestParam(required = false) Long routeId,
                               @RequestParam(defaultValue = "100") int priority,
                               RedirectAttributes ra) {
        User user = userService.findByUsername(ud.getUsername()).orElseThrow();
        pluginService.create(user.getId(), name, description, hookType, script, routeId, priority);
        ra.addFlashAttribute("success", "Plugin created");
        return "redirect:/plugins";
    }

    @PostMapping("/plugins/{id}/delete")
    public String deletePlugin(@PathVariable Long id, RedirectAttributes ra) {
        pluginService.delete(id);
        ra.addFlashAttribute("success", "Plugin deleted");
        return "redirect:/plugins";
    }

    @PostMapping("/plugins/{id}/toggle")
    public String togglePlugin(@PathVariable Long id, @RequestParam boolean active, RedirectAttributes ra) {
        pluginService.toggle(id, active);
        ra.addFlashAttribute("success", "Plugin updated");
        return "redirect:/plugins";
    }
}
