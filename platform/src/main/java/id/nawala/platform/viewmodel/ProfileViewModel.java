package id.nawala.platform.viewmodel;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileViewModel {

    @NotBlank(message = "Full name is required")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    private String email;

    private String currentPassword;

    @Size(min = 8, message = "New password must be at least 8 characters")
    private String newPassword;

    private String confirmNewPassword;

    public boolean isChangingPassword() {
        return newPassword != null && !newPassword.isBlank();
    }

    public boolean isNewPasswordMatching() {
        if (!isChangingPassword()) return true;
        return newPassword.equals(confirmNewPassword);
    }
}
