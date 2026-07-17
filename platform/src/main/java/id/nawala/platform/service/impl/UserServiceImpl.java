package id.nawala.platform.service.impl;

import id.nawala.platform.exception.ResourceNotFoundException;
import id.nawala.platform.exception.UserAlreadyExistsException;
import id.nawala.platform.model.Role;
import id.nawala.platform.model.User;
import id.nawala.platform.repository.UserRepository;
import id.nawala.platform.service.UserService;
import id.nawala.platform.viewmodel.ProfileViewModel;
import id.nawala.platform.viewmodel.RegisterViewModel;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User register(RegisterViewModel viewModel) {
        if (userRepository.existsByUsername(viewModel.getUsername().trim().toLowerCase())) {
            throw new UserAlreadyExistsException("Username is already taken");
        }
        if (userRepository.existsByEmail(viewModel.getEmail().trim().toLowerCase())) {
            throw new UserAlreadyExistsException("Email is already registered");
        }

        User user = User.builder()
                .username(viewModel.getUsername().trim().toLowerCase())
                .email(viewModel.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(viewModel.getPassword()))
                .fullName(viewModel.getFullName().trim())
                .role(Role.USER)
                .enabled(true)
                .build();

        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public User updateProfile(String username, ProfileViewModel viewModel) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setFullName(viewModel.getFullName().trim());
        user.setEmail(viewModel.getEmail().trim().toLowerCase());

        if (viewModel.getNewPassword() != null && !viewModel.getNewPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(viewModel.getNewPassword()));
        }

        return userRepository.save(user);
    }

    @Override
    public void updateLastLogin(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalUsers() {
        return userRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long getActiveUsers() {
        return userRepository.countByEnabled(true);
    }
}
