package id.nawala.gateway.core;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class RouteDefinition {
    private Long id;
    private String name;
    private String method;
    private String path;
    private String targetUrl;
    private boolean authRequired;
    private boolean rateLimitEnabled;
    private int rateLimitPerMinute;
    private boolean payloadEncryption;
    private boolean loadBalanced;
    private String lbStrategy;
    private int timeoutSeconds;
    private List<TargetDefinition> targets;
}
