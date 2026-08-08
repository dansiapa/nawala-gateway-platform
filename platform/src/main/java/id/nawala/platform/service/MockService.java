package id.nawala.platform.service;

import id.nawala.platform.model.ApiMock;
import id.nawala.platform.model.User;
import java.util.List;

/**
 * API Mock / Sandbox service for testing endpoints without real backend.
 */
public interface MockService {

    ApiMock create(Long userId, String name, String path, String method,
                   int statusCode, String responseBody, String contentType, int delayMs);
    
    ApiMock createMock(User owner, String name, String method, String path, 
                       int statusCode, String responseBody, String contentType);

    List<ApiMock> getByUser(Long userId);

    List<ApiMock> getAllActive();

    void delete(Long mockId);
    
    void deleteMock(Long mockId);

    void toggle(Long mockId, boolean active);

    ApiMock findByPathAndMethod(String path, String method);
}
