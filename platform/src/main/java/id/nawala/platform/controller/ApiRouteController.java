package id.nawala.platform.controller;

import id.nawala.platform.model.ApiRoute;
import id.nawala.platform.model.LoadBalancerStrategy;
import id.nawala.platform.model.RouteTarget;
import id.nawala.platform.model.User;
import id.nawala.platform.repository.RouteTargetRepository;
import id.nawala.platform.service.ActivityLogService;
import id.nawala.platform.service.ApiRouteService;
import id.nawala.platform.service.AuditService;
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

import java.util.List;

@Controller
@RequestMapping("/routes")
@RequiredArgsConstructor
public class ApiRouteController {

    private final ApiRouteService apiRouteService;
    private final UserService userService;
    private final ActivityLogService activityLogService;
    private final AuditService auditService;
    private final RouteTargetRepository routeTargetRepository;

    @GetMapping
    public String listRoutes(Model model) {
        model.addAttribute("routes", apiRouteService.findAll());
        return "routes/list";
    }

    @GetMapping("/new")
    public String newRouteForm(Model model) {
        model.addAttribute("form", new ApiRouteViewModel());
        model.addAttribute("strategies", LoadBalancerStrategy.values());
        return "routes/form";
    }

    @PostMapping("/new")
    public String createRoute(@AuthenticationPrincipal UserDetails userDetails,
                              @Valid @ModelAttribute("form") ApiRouteViewModel form,
                              BindingResult result,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        if (result.hasErrors()) {
            model.addAttribute("strategies", LoadBalancerStrategy.values());
            return "routes/form";
        }

        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();

        try {
            ApiRoute route = apiRouteService.register(form, user);
            
            // Handle load balancer targets
            if (form.isLoadBalanced() && form.getTargetUrls() != null) {
                saveLoadBalancerTargets(route, form.getTargetUrls(), form.getTargetWeights());
            }
            
            activityLogService.log(user, "ROUTE_CREATE", "Created route: " + form.getMethod() + " " + form.getPath(), null);
            auditService.log("CREATE", "ROUTE", "Created route: " + form.getName(), userDetails.getUsername());
            redirectAttributes.addFlashAttribute("success", "Route registered successfully");
        } catch (IllegalArgumentException e) {
            result.reject("error.global", e.getMessage());
            model.addAttribute("strategies", LoadBalancerStrategy.values());
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
        form.setPayloadEncryption(route.isPayloadEncryption());
        form.setHealthCheckUrl(route.getHealthCheckUrl());
        
        // Load balancer settings
        form.setLoadBalanced(route.isLoadBalanced());
        form.setLoadBalancerStrategy(route.getLoadBalancerStrategy());
        
        // Load existing targets
        List<RouteTarget> targets = routeTargetRepository.findByRoute(route);
        if (!targets.isEmpty()) {
            form.setTargetUrls(targets.stream().map(RouteTarget::getUrl).toList());
            form.setTargetWeights(targets.stream().map(RouteTarget::getWeight).toList());
        }

        model.addAttribute("form", form);
        model.addAttribute("routeId", id);
        model.addAttribute("route", route);
        model.addAttribute("strategies", LoadBalancerStrategy.values());
        model.addAttribute("targets", targets);
        
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
            model.addAttribute("strategies", LoadBalancerStrategy.values());
            return "routes/form";
        }

        ApiRoute route = apiRouteService.update(id, form);
        
        // Update load balancer targets
        routeTargetRepository.deleteByRoute(route);
        if (form.isLoadBalanced() && form.getTargetUrls() != null) {
            saveLoadBalancerTargets(route, form.getTargetUrls(), form.getTargetWeights());
        }
        
        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        activityLogService.log(user, "ROUTE_UPDATE", "Updated route: " + form.getMethod() + " " + form.getPath(), null);
        auditService.log("UPDATE", "ROUTE", "Updated route: " + form.getName(), userDetails.getUsername());
        redirectAttributes.addFlashAttribute("success", "Route updated successfully");
        return "redirect:/routes";
    }

    @PostMapping("/{id}/toggle")
    public String toggleRoute(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        ApiRoute route = apiRouteService.toggleActive(id);
        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        activityLogService.log(user, "ROUTE_TOGGLE", "Toggled route status: #" + id, null);
        auditService.log("UPDATE", "ROUTE", "Route " + (route.isActive() ? "activated" : "deactivated") + ": " + route.getName(), userDetails.getUsername());
        redirectAttributes.addFlashAttribute("success", "Route status updated");
        return "redirect:/routes";
    }

    @PostMapping("/{id}/delete")
    public String deleteRoute(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        ApiRoute route = apiRouteService.findById(id).orElseThrow();
        apiRouteService.delete(id);
        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        activityLogService.log(user, "ROUTE_DELETE", "Deleted route: #" + id, null);
        auditService.log("DELETE", "ROUTE", "Deleted route: " + route.getName(), userDetails.getUsername());
        redirectAttributes.addFlashAttribute("success", "Route deleted successfully");
        return "redirect:/routes";
    }

    // ========== LOAD BALANCER TARGETS ==========
    @PostMapping("/{id}/targets/add")
    public String addTarget(@PathVariable Long id,
                           @RequestParam String url,
                           @RequestParam(defaultValue = "50") int weight,
                           @AuthenticationPrincipal UserDetails userDetails,
                           RedirectAttributes redirectAttributes) {
        ApiRoute route = apiRouteService.findById(id).orElseThrow();
        
        RouteTarget target = RouteTarget.builder()
                .route(route)
                .url(url)
                .weight(weight)
                .healthy(true)
                .active(true)
                .build();
        routeTargetRepository.save(target);
        
        // Enable load balancing if not already
        if (!route.isLoadBalanced()) {
            route.setLoadBalanced(true);
            apiRouteService.save(route);
        }
        
        auditService.log("CREATE", "ROUTE_TARGET", "Added LB target to route: " + route.getName(), userDetails.getUsername());
        redirectAttributes.addFlashAttribute("success", "Target added successfully");
        return "redirect:/routes/" + id + "/edit";
    }

    @PostMapping("/{routeId}/targets/{targetId}/remove")
    public String removeTarget(@PathVariable Long routeId,
                              @PathVariable Long targetId,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        routeTargetRepository.deleteById(targetId);
        auditService.log("DELETE", "ROUTE_TARGET", "Removed LB target from route #" + routeId, userDetails.getUsername());
        redirectAttributes.addFlashAttribute("success", "Target removed");
        return "redirect:/routes/" + routeId + "/edit";
    }

    @PostMapping("/{routeId}/targets/{targetId}/toggle")
    public String toggleTarget(@PathVariable Long routeId,
                              @PathVariable Long targetId,
                              RedirectAttributes redirectAttributes) {
        RouteTarget target = routeTargetRepository.findById(targetId).orElseThrow();
        target.setActive(!target.isActive());
        routeTargetRepository.save(target);
        redirectAttributes.addFlashAttribute("success", "Target " + (target.isActive() ? "enabled" : "disabled"));
        return "redirect:/routes/" + routeId + "/edit";
    }

    // Helper method
    private void saveLoadBalancerTargets(ApiRoute route, List<String> urls, List<Integer> weights) {
        for (int i = 0; i < urls.size(); i++) {
            String url = urls.get(i);
            if (url != null && !url.isBlank()) {
                int weight = (weights != null && i < weights.size()) ? weights.get(i) : 50;
                RouteTarget target = RouteTarget.builder()
                        .route(route)
                        .url(url)
                        .weight(weight)
                        .healthy(true)
                        .active(true)
                        .build();
                routeTargetRepository.save(target);
            }
        }
    }
}
