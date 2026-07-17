package id.nawala.platform.viewmodel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    /**
     * Public-facing masked path (optional).
     * If set, clients use this URL instead of the real internal path.
     */
    @Size(max = 500)
    private String maskedPath;

    @NotBlank(message = "Target URL is required")
    @Size(max = 500)
    private String targetUrl;

    private boolean authRequired;

    private boolean rateLimitEnabled;

    private int rateLimitPerMinute;

    /**
     * Enable end-to-end payload encryption for this route.
     */
    private boolean payloadEncryption;

    /**
     * Health check endpoint URL for live monitoring.
     */
    @Size(max = 500)
    private String healthCheckUrl;
}

