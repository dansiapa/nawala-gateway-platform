package id.nawala.platform.service.impl;

import id.nawala.platform.model.ApiDoc;
import id.nawala.platform.model.ApiRoute;
import id.nawala.platform.model.User;
import id.nawala.platform.repository.ApiDocRepository;
import id.nawala.platform.repository.ApiRouteRepository;
import id.nawala.platform.repository.UserRepository;
import id.nawala.platform.service.ApiDocService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
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
    
    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getApiGroups() {
        // Group routes by their base path
        List<ApiRoute> routes = apiRouteRepository.findByActiveTrue();
        
        Map<String, List<ApiRoute>> grouped = routes.stream()
                .collect(Collectors.groupingBy(r -> {
                    String path = r.getPath();
                    if (path.startsWith("/")) path = path.substring(1);
                    int idx = path.indexOf("/");
                    return idx > 0 ? path.substring(0, idx) : path;
                }));
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<ApiRoute>> entry : grouped.entrySet()) {
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("name", entry.getKey());
            group.put("description", "APIs under /" + entry.getKey());
            group.put("routes", entry.getValue().stream().map(r -> {
                Map<String, Object> routeMap = new LinkedHashMap<>();
                routeMap.put("id", r.getId());
                routeMap.put("name", r.getName());
                routeMap.put("method", r.getMethod());
                routeMap.put("path", r.getPath());
                routeMap.put("description", r.getDescription());
                return routeMap;
            }).toList());
            result.add(group);
        }
        
        return result;
    }
    
    @Override
    @Transactional
    public void regenerateDocumentation() {
        log.info("Regenerating API documentation...");
        // In a real implementation, this would:
        // 1. Scan all active routes
        // 2. Generate OpenAPI spec from route definitions
        // 3. Update or create ApiDoc entries
        
        List<ApiRoute> routes = apiRouteRepository.findByActiveTrue();
        log.info("Found {} active routes for documentation", routes.size());
        
        // For now, just log that we're regenerating
        // Full implementation would create proper OpenAPI specs
    }
}
