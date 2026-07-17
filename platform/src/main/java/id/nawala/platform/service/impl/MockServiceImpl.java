package id.nawala.platform.service.impl;

import id.nawala.platform.model.ApiMock;
import id.nawala.platform.model.User;
import id.nawala.platform.repository.ApiMockRepository;
import id.nawala.platform.repository.UserRepository;
import id.nawala.platform.service.MockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MockServiceImpl implements MockService {

    private final ApiMockRepository mockRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ApiMock create(Long userId, String name, String path, String method,
                          int statusCode, String responseBody, String contentType, int delayMs) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        ApiMock mock = ApiMock.builder()
                .owner(user).name(name).path(path).method(method.toUpperCase())
                .statusCode(statusCode).responseBody(responseBody)
                .contentType(contentType != null ? contentType : "application/json")
                .delayMs(delayMs).active(true)
                .build();
        return mockRepository.save(mock);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiMock> getByUser(Long userId) {
        return mockRepository.findByOwnerId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiMock> getAllActive() {
        return mockRepository.findByActiveTrue();
    }

    @Override
    @Transactional
    public void delete(Long mockId) {
        mockRepository.deleteById(mockId);
    }

    @Override
    @Transactional
    public void toggle(Long mockId, boolean active) {
        mockRepository.findById(mockId).ifPresent(m -> {
            m.setActive(active);
            mockRepository.save(m);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public ApiMock findByPathAndMethod(String path, String method) {
        return mockRepository.findByPathAndMethodAndActiveTrue(path, method.toUpperCase())
                .orElse(null);
    }
}
