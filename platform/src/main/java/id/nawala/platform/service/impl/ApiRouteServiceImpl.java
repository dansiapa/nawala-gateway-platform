package id.nawala.platform.service.impl;

import id.nawala.platform.model.ApiRoute;
import id.nawala.platform.model.User;
import id.nawala.platform.repository.ApiRouteRepository;
import id.nawala.platform.service.ApiRouteService;
import id.nawala.platform.viewmodel.ApiRouteViewModel;
import id.nawala.platform.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ApiRouteServiceImpl implements ApiRouteService {

    private final ApiRouteRepository apiRouteRepository;

    @Override
    public ApiRoute register(ApiRouteViewModel viewModel, User createdBy) {
        if (apiRouteRepository.existsByPathAndMethod(viewModel.getPath(), viewModel.getMethod())) {
            throw new IllegalArgumentException("Route already exists: " + viewModel.getMethod() + " " + viewModel.getPath());
        }

        ApiRoute route = ApiRoute.builder()
                .name(viewModel.getName().trim())
                .description(viewModel.getDescription())
                .method(viewModel.getMethod().toUpperCase())
                .path(viewModel.getPath().trim())
                .maskedPath(viewModel.getMaskedPath() != null && !viewModel.getMaskedPath().isBlank()
                        ? viewModel.getMaskedPath().trim() : null)
                .targetUrl(viewModel.getTargetUrl().trim())
                .authRequired(viewModel.isAuthRequired())
                .rateLimitEnabled(viewModel.isRateLimitEnabled())
                .rateLimitPerMinute(viewModel.getRateLimitPerMinute() > 0 ? viewModel.getRateLimitPerMinute() : 60)
                .payloadEncryption(viewModel.isPayloadEncryption())
                .healthCheckUrl(viewModel.getHealthCheckUrl() != null && !viewModel.getHealthCheckUrl().isBlank()
                        ? viewModel.getHealthCheckUrl().trim() : null)
                .active(true)
                .createdBy(createdBy)
                .build();

        return apiRouteRepository.save(route);
    }

    @Override
    public ApiRoute update(Long id, ApiRouteViewModel viewModel) {
        ApiRoute route = apiRouteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found"));

        route.setName(viewModel.getName().trim());
        route.setDescription(viewModel.getDescription());
        route.setMethod(viewModel.getMethod().toUpperCase());
        route.setPath(viewModel.getPath().trim());
        route.setMaskedPath(viewModel.getMaskedPath() != null && !viewModel.getMaskedPath().isBlank()
                ? viewModel.getMaskedPath().trim() : null);
        route.setTargetUrl(viewModel.getTargetUrl().trim());
        route.setAuthRequired(viewModel.isAuthRequired());
        route.setRateLimitEnabled(viewModel.isRateLimitEnabled());
        route.setRateLimitPerMinute(viewModel.getRateLimitPerMinute());
        route.setPayloadEncryption(viewModel.isPayloadEncryption());
        route.setHealthCheckUrl(viewModel.getHealthCheckUrl() != null && !viewModel.getHealthCheckUrl().isBlank()
                ? viewModel.getHealthCheckUrl().trim() : null);

        return apiRouteRepository.save(route);
    }

    @Override
    public void delete(Long id) {
        if (!apiRouteRepository.existsById(id)) {
            throw new ResourceNotFoundException("Route not found");
        }
        apiRouteRepository.deleteById(id);
    }

    @Override
    public void toggleActive(Long id) {
        ApiRoute route = apiRouteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found"));
        route.setActive(!route.isActive());
        apiRouteRepository.save(route);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ApiRoute> findById(Long id) {
        return apiRouteRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiRoute> findAll() {
        return apiRouteRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiRoute> findActiveRoutes() {
        return apiRouteRepository.findByActiveTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalRoutes() {
        return apiRouteRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long getActiveRoutes() {
        return apiRouteRepository.countByActive(true);
    }
}
