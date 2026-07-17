package id.nawala.platform.controller;

import id.nawala.platform.model.User;
import id.nawala.platform.service.ActivityLogService;
import id.nawala.platform.service.UserService;
import id.nawala.platform.viewmodel.ProfileViewModel;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final ActivityLogService activityLogService;

    @GetMapping("/profile")
    public String profilePage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();

        ProfileViewModel form = new ProfileViewModel();
        form.setFullName(user.getFullName());
        form.setEmail(user.getEmail());

        model.addAttribute("form", form);
        model.addAttribute("user", user);
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                @Valid @ModelAttribute("form") ProfileViewModel form,
                                BindingResult result,
                                Model model,
                                RedirectAttributes redirectAttributes) {

        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();

        if (form.isChangingPassword()) {
            if (form.getCurrentPassword() == null || form.getCurrentPassword().isBlank()) {
                result.rejectValue("currentPassword", "error.form", "Current password is required");
            } else if (!passwordEncoder.matches(form.getCurrentPassword(), user.getPassword())) {
                result.rejectValue("currentPassword", "error.form", "Current password is incorrect");
            }
            if (!form.isNewPasswordMatching()) {
                result.rejectValue("confirmNewPassword", "error.form", "Passwords do not match");
            }
        }

        if (result.hasErrors()) {
            model.addAttribute("user", user);
            return "profile";
        }

        userService.updateProfile(userDetails.getUsername(), form);
        activityLogService.log(user, "PROFILE_UPDATE", "Updated profile information", null);
        redirectAttributes.addFlashAttribute("success", "Profile updated successfully");
        return "redirect:/profile";
    }
}
