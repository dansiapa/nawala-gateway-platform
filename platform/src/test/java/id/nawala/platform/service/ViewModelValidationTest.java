package id.nawala.platform.viewmodel;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ViewModel Validation Regression Tests")
class ViewModelValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Valid RegisterViewModel passes validation")
    void validRegister() {
        RegisterViewModel vm = new RegisterViewModel("Full Name", "username",
                "email@test.com", "Password1!", "Password1!");
        Set<ConstraintViolation<RegisterViewModel>> violations = validator.validate(vm);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("RegisterViewModel blank username fails")
    void blankUsername() {
        RegisterViewModel vm = new RegisterViewModel("Full", "", "e@t.com", "Pass123!", "Pass123!");
        Set<ConstraintViolation<RegisterViewModel>> violations = validator.validate(vm);
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("RegisterViewModel short username fails")
    void shortUsername() {
        RegisterViewModel vm = new RegisterViewModel("Full", "ab", "e@t.com", "Pass123!", "Pass123!");
        Set<ConstraintViolation<RegisterViewModel>> violations = validator.validate(vm);
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("RegisterViewModel invalid email fails")
    void invalidEmail() {
        RegisterViewModel vm = new RegisterViewModel("Full", "user", "notanemail", "Pass123!", "Pass123!");
        Set<ConstraintViolation<RegisterViewModel>> violations = validator.validate(vm);
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("RegisterViewModel short password fails")
    void shortPassword() {
        RegisterViewModel vm = new RegisterViewModel("Full", "user", "e@t.com", "short", "short");
        Set<ConstraintViolation<RegisterViewModel>> violations = validator.validate(vm);
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("RegisterViewModel password matching works")
    void passwordMatching() {
        RegisterViewModel match = new RegisterViewModel("F", "usr", "e@t.com", "Password1!", "Password1!");
        RegisterViewModel noMatch = new RegisterViewModel("F", "usr", "e@t.com", "Password1!", "Different!");
        assertThat(match.isPasswordMatching()).isTrue();
        assertThat(noMatch.isPasswordMatching()).isFalse();
    }

    @Test
    @DisplayName("Valid ApiRouteViewModel passes validation")
    void validRoute() {
        ApiRouteViewModel vm = new ApiRouteViewModel("Route", "desc", "GET",
                "/api/test", null, "http://localhost:8080", true, true, 60, false, null);
        Set<ConstraintViolation<ApiRouteViewModel>> violations = validator.validate(vm);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("ApiRouteViewModel blank name fails")
    void blankRouteName() {
        ApiRouteViewModel vm = new ApiRouteViewModel("", "desc", "GET",
                "/api/test", null, "http://localhost:8080", true, true, 60, false, null);
        Set<ConstraintViolation<ApiRouteViewModel>> violations = validator.validate(vm);
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("ApiRouteViewModel blank method fails")
    void blankRouteMethod() {
        ApiRouteViewModel vm = new ApiRouteViewModel("Route", "desc", "",
                "/api/test", null, "http://localhost:8080", true, true, 60, false, null);
        Set<ConstraintViolation<ApiRouteViewModel>> violations = validator.validate(vm);
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("ApiRouteViewModel blank targetUrl fails")
    void blankTargetUrl() {
        ApiRouteViewModel vm = new ApiRouteViewModel("Route", "desc", "GET",
                "/api/test", null, "", true, true, 60, false, null);
        Set<ConstraintViolation<ApiRouteViewModel>> violations = validator.validate(vm);
        assertThat(violations).isNotEmpty();
    }
}
