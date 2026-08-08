package id.nawala.gateway.graphql;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GraphQL Schema Registry - Manages GraphQL schemas for different APIs
 */
@Slf4j
@Service
public class GraphQLSchemaRegistry {

    private final Map<String, GraphQLEndpoint> endpoints = new ConcurrentHashMap<>();

    public void registerEndpoint(String name, String url, String schema) {
        GraphQLEndpoint endpoint = new GraphQLEndpoint(name, url, schema);
        endpoints.put(name, endpoint);
        log.info("Registered GraphQL endpoint: {}", name);
    }

    public void removeEndpoint(String name) {
        endpoints.remove(name);
        log.info("Removed GraphQL endpoint: {}", name);
    }

    public GraphQLEndpoint getEndpoint(String name) {
        return endpoints.get(name);
    }

    public Map<String, GraphQLEndpoint> getAllEndpoints() {
        return Map.copyOf(endpoints);
    }

    public static class GraphQLEndpoint {
        private final String name;
        private final String url;
        private final String schema;
        private boolean enabled = true;

        public GraphQLEndpoint(String name, String url, String schema) {
            this.name = name;
            this.url = url;
            this.schema = schema;
        }

        public String getName() { return name; }
        public String getUrl() { return url; }
        public String getSchema() { return schema; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
