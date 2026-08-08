package id.nawala.platform.datacenter;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DatacenterRepository extends JpaRepository<DatacenterConfig, Long> {
    
    Optional<DatacenterConfig> findByPrimaryTrue();
    
    List<DatacenterConfig> findByEnabledTrueOrderByWeightDesc();
    
    List<DatacenterConfig> findByRegion(String region);
    
    Optional<DatacenterConfig> findByName(String name);
    
    List<DatacenterConfig> findByStatus(DatacenterConfig.DatacenterStatus status);
}
