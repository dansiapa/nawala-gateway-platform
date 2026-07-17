package id.nawala.platform.service.impl;

import id.nawala.platform.model.ApiDoc;
import id.nawala.platform.model.ApiRoute;
import id.nawala.platform.model.User;
import id.nawala.platform.repository.ApiDocRepository;
import id.nawala.platform.repository.ApiRouteRepository;
import id.nawala.platform.repository.UserRepository;
import id.nawala.platform.service.ApiDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApiDocServiceImpl implements ApiDocService {

    private final ApiDocRepository apiDocRepository;
    private final UserRepository userRepository;
    private final ApiRouteRepository apiRouteRepository;

    @Override
    @Transactional
    public ApiDoc create(Long userId, Long routeId, String title, String version, String openApiSpec, String description) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        ApiRoute route = routeId != null ? apiRouteRepository.findById(routeId).orElse(null) : null;
        ApiDoc doc = ApiDoc.builder()
                .owner(owner).route(route).title(title)
                .version(version != null ? version : "1.0.0")
                .openApiSpec(openApiSpec).description(description)
                .published(false)
                .build();
        return apiDocRepository.save(doc);
    }

    @Override
    @Transactional
    public ApiDoc update(Long docId, String title, String version, String openApiSpec, String description) {
        ApiDoc doc = apiDocRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("Doc not found"));
        if (title != null) doc.setTitle(title);
        if (version != null) doc.setVersion(version);
        if (openApiSpec != null) doc.setOpenApiSpec(openApiSpec);
        if (description != null) doc.setDescription(description);
        return apiDocRepository.save(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiDoc> getByUser(Long userId) {
        return apiDocRepository.findByOwnerId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiDoc> getPublished() {
        return apiDocRepository.findByPublishedTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public ApiDoc getById(Long docId) {
        return apiDocRepository.findById(docId).orElse(null);
    }

    @Override
    @Transactional
    public void publish(Long docId, boolean published) {
        apiDocRepository.findById(docId).ifPresent(doc -> {
            doc.setPublished(published);
            apiDocRepository.save(doc);
        });
    }

    @Override
    @Transactional
    public void delete(Long docId) {
        apiDocRepository.deleteById(docId);
    }
}
