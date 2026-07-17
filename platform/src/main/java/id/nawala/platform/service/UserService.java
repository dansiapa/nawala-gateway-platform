package id.nawala.platform.service;

import id.nawala.platform.model.User;
import id.nawala.platform.viewmodel.RegisterViewModel;
import id.nawala.platform.viewmodel.ProfileViewModel;

import java.util.Optional;

public interface UserService {

    User register(RegisterViewModel viewModel);

    Optional<User> findByUsername(String username);

    User updateProfile(String username, ProfileViewModel viewModel);

    void updateLastLogin(String username);

    long getTotalUsers();

    long getActiveUsers();
}
