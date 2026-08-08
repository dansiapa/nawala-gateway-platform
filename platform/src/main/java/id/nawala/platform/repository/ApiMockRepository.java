package id.nawala.platform.repository;

import id.nawala.platform.model.ApiMock;
import id.nawala.platform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiMockRepository extends JpaRepository<ApiMock, Long> {

    List<ApiMock> findByOwnerIdAndActiveTrue(Long userId);

    List<ApiMock> findByOwnerId(Long userId);
    
    List<ApiMock> findByCreatedBy(User user);

    Optional<ApiMock> findByPathAndMethodAndActiveTrue(String path, String method);

    List<ApiMock> findByActiveTrue();
}
