package id.nawala.gateway.grpc;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * gRPC Service Registry - Manages gRPC service endpoints
 */
@Slf4j
@Service
public class GrpcServiceRegistry {

    private final Map<String, GrpcEndpoint> services = new ConcurrentHashMap<>();

    public void registerService(String serviceName, String host, int port, String protoFile) {
        GrpcEndpoint endpoint = new GrpcEndpoint(serviceName, host, port, protoFile);
        services.put(serviceName, endpoint);
        log.info("Registered gRPC service: {} at {}:{}", serviceName, host, port);
    }

    public void removeService(String serviceName) {
        services.remove(serviceName);
        log.info("Removed gRPC service: {}", serviceName);
    }

    public GrpcEndpoint getService(String serviceName) {
        return services.get(serviceName);
    }

    public Map<String, GrpcEndpoint> getAllServices() {
        return Map.copyOf(services);
    }

    public static class GrpcEndpoint {
        private final String serviceName;
        private final String host;
        private final int port;
        private final String protoFile;
        private boolean enabled = true;
        private boolean useTls = false;

        public GrpcEndpoint(String serviceName, String host, int port, String protoFile) {
            this.serviceName = serviceName;
            this.host = host;
            this.port = port;
            this.protoFile = protoFile;
        }

        public String getServiceName() { return serviceName; }
        public String getHost() { return host; }
        public int getPort() { return port; }
        public String getProtoFile() { return protoFile; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isUseTls() { return useTls; }
        public void setUseTls(boolean useTls) { this.useTls = useTls; }
        
        public String getAddress() {
            return host + ":" + port;
        }
    }
}
