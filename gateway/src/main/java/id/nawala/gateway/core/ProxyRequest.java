package id.nawala.gateway.core;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpHeaders;

@Data
@Builder
public class ProxyRequest {
    private String requestId;
    private String method;
    private String path;
    private String queryString;
    private String targetUrl;
    private HttpHeaders headers;
    private byte[] body;
    private String clientIp;
    private String originalHost;
    private int timeoutSeconds;
    private long startTime;
    
    // Route metadata
    private Long routeId;
    private String routeName;
    private boolean authRequired;
    private boolean rateLimitEnabled;
    private int rateLimitPerMinute;
    private boolean payloadEncryption;
}
