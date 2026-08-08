package id.nawala.platform.viewmodel;

import id.nawala.platform.model.LoadBalancerStrategy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiRouteViewModel {

    @NotBlank(message = "Route name is required")
    @Size(max = 100)
    private String name;

    @Size(max = 255)
    private String description;

    @NotBlank(message = "HTTP method is required")
    private String method;

    @NotBlank(message = "Path is required")
    @Size(max = 500)
    private String path;

    @Size(max = 500)
    private String maskedPath;

    @NotBlank(message = "Target URL is required")
    @Size(max = 500)
    private String targetUrl;

    private boolean authRequired;

    private boolean rateLimitEnabled;

    private int rateLimitPerMinute;

    private boolean payloadEncryption;

    @Size(max = 500)
    private String healthCheckUrl;

    // ========== LOAD BALANCER ==========
    private boolean loadBalanced;

    private LoadBalancerStrategy loadBalancerStrategy = LoadBalancerStrategy.ROUND_ROBIN;

    // Additional targets for load balancing
    private List<String> targetUrls;
    private List<Integer> targetWeights;
}
