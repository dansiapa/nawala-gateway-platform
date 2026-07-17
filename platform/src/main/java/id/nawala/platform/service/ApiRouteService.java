package id.nawala.platform.service;

import id.nawala.platform.model.ApiRoute;
import id.nawala.platform.model.User;
import id.nawala.platform.viewmodel.ApiRouteViewModel;

import java.util.List;
import java.util.Optional;

public interface ApiRouteService {

    ApiRoute register(ApiRouteViewModel viewModel, User createdBy);

    ApiRoute update(Long id, ApiRouteViewModel viewModel);

    void delete(Long id);

    void toggleActive(Long id);

    Optional<ApiRoute> findById(Long id);

    List<ApiRoute> findAll();

    List<ApiRoute> findActiveRoutes();

    long getTotalRoutes();

    long getActiveRoutes();
}
