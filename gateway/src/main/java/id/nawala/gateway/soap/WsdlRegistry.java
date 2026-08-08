package id.nawala.gateway.soap;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import lombok.Data;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class WsdlRegistry {

    private final Map<String, WsdlEndpoint> endpoints = new ConcurrentHashMap<>();

    public void registerEndpoint(String name, String wsdlUrl, String targetUrl) {
        WsdlEndpoint endpoint = new WsdlEndpoint(name, wsdlUrl, targetUrl);
        endpoints.put(name, endpoint);
        log.info("Registered SOAP endpoint: {}", name);
    }

    public void removeEndpoint(String name) {
        endpoints.remove(name);
    }

    public WsdlEndpoint getEndpoint(String name) {
        return endpoints.get(name);
    }

    public Map<String, WsdlEndpoint> getAllEndpoints() {
        return Map.copyOf(endpoints);
    }

    @Data
    public static class WsdlEndpoint {
        private final String name;
        private final String wsdlUrl;
        private final String targetUrl;
        private boolean enabled = true;
        private String username;
        private String password;
        private boolean useMtls = false;
    }
}
