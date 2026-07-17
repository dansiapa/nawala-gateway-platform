package id.nawala.platform.controller;

import id.nawala.platform.exception.UserAlreadyExistsException;
import id.nawala.platform.logging.SecurityLogger;
import id.nawala.platform.service.UserService;
import id.nawala.platform.viewmodel.RegisterViewModel;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("form", new RegisterViewModel());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterViewModel form,
                           BindingResult result,
                           RedirectAttributes redirectAttributes,
                           HttpServletRequest request) {
        if (!form.isPasswordMatching()) {
            result.rejectValue("confirmPassword", "error.form", "Passwords do not match");
        }

        if (result.hasErrors()) {
            SecurityLogger.log().info("Register failed validation user={} ip={}",
                    form.getUsername(), request.getRemoteAddr());
            return "register";
        }

        try {
            userService.register(form);
            SecurityLogger.log().info("Register success user={} email={} ip={}",
                    form.getUsername(), form.getEmail(), request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("success", "Account created successfully. Please sign in.");
            return "redirect:/login";
        } catch (UserAlreadyExistsException e) {
            SecurityLogger.log().warn("Register duplicate user={} ip={}",
                    form.getUsername(), request.getRemoteAddr());
            result.reject("error.global", e.getMessage());
            return "register";
        }
    }
}

