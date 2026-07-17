package id.nawala.platform.controller;

import id.nawala.platform.model.ApiRoute;
import id.nawala.platform.model.User;
import id.nawala.platform.service.ActivityLogService;
import id.nawala.platform.service.ApiRouteService;
import id.nawala.platform.service.UserService;
import id.nawala.platform.viewmodel.ApiRouteViewModel;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/routes")
@RequiredArgsConstructor
public class ApiRouteController {

    private final ApiRouteService apiRouteService;
    private final UserService userService;
    private final ActivityLogService activityLogService;

    @GetMapping
    public String listRoutes(Model model) {
        model.addAttribute("routes", apiRouteService.findAll());
        return "routes/list";
    }

    @GetMapping("/new")
    public String newRouteForm(Model model) {
        model.addAttribute("form", new ApiRouteViewModel());
        return "routes/form";
    }

    @PostMapping("/new")
    public String createRoute(@AuthenticationPrincipal UserDetails userDetails,
                              @Valid @ModelAttribute("form") ApiRouteViewModel form,
                              BindingResult result,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "routes/form";
        }

        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();

        try {
            apiRouteService.register(form, user);
            activityLogService.log(user, "ROUTE_CREATE", "Created route: " + form.getMethod() + " " + form.getPath(), null);
            redirectAttributes.addFlashAttribute("success", "Route registered successfully");
        } catch (IllegalArgumentException e) {
            result.reject("error.global", e.getMessage());
            return "routes/form";
        }

        return "redirect:/routes";
    }

    @GetMapping("/{id}/edit")
    public String editRouteForm(@PathVariable Long id, Model model) {
        ApiRoute route = apiRouteService.findById(id).orElseThrow();

        ApiRouteViewModel form = new ApiRouteViewModel();
        form.setName(route.getName());
        form.setDescription(route.getDescription());
        form.setMethod(route.getMethod());
        form.setPath(route.getPath());
        form.setTargetUrl(route.getTargetUrl());
        form.setAuthRequired(route.isAuthRequired());
        form.setRateLimitEnabled(route.isRateLimitEnabled());
        form.setRateLimitPerMinute(route.getRateLimitPerMinute());

        model.addAttribute("form", form);
        model.addAttribute("routeId", id);
        return "routes/form";
    }

    @PostMapping("/{id}/edit")
    public String updateRoute(@PathVariable Long id,
                              @Valid @ModelAttribute("form") ApiRouteViewModel form,
                              BindingResult result,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        if (result.hasErrors()) {
            model.addAttribute("routeId", id);
            return "routes/form";
        }

        apiRouteService.update(id, form);
        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        activityLogService.log(user, "ROUTE_UPDATE", "Updated route: " + form.getMethod() + " " + form.getPath(), null);
        redirectAttributes.addFlashAttribute("success", "Route updated successfully");
        return "redirect:/routes";
    }

    @PostMapping("/{id}/toggle")
    public String toggleRoute(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        apiRouteService.toggleActive(id);
        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        activityLogService.log(user, "ROUTE_TOGGLE", "Toggled route status: #" + id, null);
        redirectAttributes.addFlashAttribute("success", "Route status updated");
        return "redirect:/routes";
    }

    @PostMapping("/{id}/delete")
    public String deleteRoute(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        apiRouteService.delete(id);
        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        activityLogService.log(user, "ROUTE_DELETE", "Deleted route: #" + id, null);
        redirectAttributes.addFlashAttribute("success", "Route deleted successfully");
        return "redirect:/routes";
    }
}
