package id.nawala.platform.service;

import id.nawala.platform.model.ApiDoc;
import java.util.List;

/**
 * API Documentation service for managing OpenAPI specs.
 */
public interface ApiDocService {

    ApiDoc create(Long userId, Long routeId, String title, String version, String openApiSpec, String description);

    ApiDoc update(Long docId, String title, String version, String openApiSpec, String description);

    List<ApiDoc> getByUser(Long userId);

    List<ApiDoc> getPublished();

    ApiDoc getById(Long docId);

    void publish(Long docId, boolean published);

    void delete(Long docId);
}
