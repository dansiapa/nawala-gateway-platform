package id.nawala.gateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RequestTransformer {
    
    private final ObjectMapper objectMapper;
    
    public TransformResult transform(byte[] body, HttpHeaders headers, List<TransformRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return new TransformResult(body, headers);
        }
        
        HttpHeaders newHeaders = new HttpHeaders();
        newHeaders.addAll(headers);
        byte[] newBody = body;
        
        for (TransformRule rule : rules) {
            switch (rule.type()) {
                case ADD_HEADER -> newHeaders.add(rule.key(), rule.value());
                case REMOVE_HEADER -> newHeaders.remove(rule.key());
                case REPLACE_HEADER -> {
                    newHeaders.remove(rule.key());
                    newHeaders.add(rule.key(), rule.value());
                }
                case ADD_BODY_FIELD -> newBody = addJsonField(newBody, rule.key(), rule.value());
                case REMOVE_BODY_FIELD -> newBody = removeJsonField(newBody, rule.key());
                case REPLACE_BODY_FIELD -> newBody = replaceJsonField(newBody, rule.key(), rule.value());
            }
        }
        
        return new TransformResult(newBody, newHeaders);
    }
    
    private byte[] addJsonField(byte[] body, String field, String value) {
        if (body == null || body.length == 0) return body;
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node.isObject()) {
                ((ObjectNode) node).put(field, value);
                return objectMapper.writeValueAsBytes(node);
            }
        } catch (Exception e) {
            log.debug("Cannot add field to non-JSON body");
        }
        return body;
    }
    
    private byte[] removeJsonField(byte[] body, String field) {
        if (body == null || body.length == 0) return body;
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node.isObject()) {
                ((ObjectNode) node).remove(field);
                return objectMapper.writeValueAsBytes(node);
            }
        } catch (Exception e) {
            log.debug("Cannot remove field from non-JSON body");
        }
        return body;
    }
    
    private byte[] replaceJsonField(byte[] body, String field, String value) {
        if (body == null || body.length == 0) return body;
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node.isObject() && node.has(field)) {
                ((ObjectNode) node).put(field, value);
                return objectMapper.writeValueAsBytes(node);
            }
        } catch (Exception e) {
            log.debug("Cannot replace field in non-JSON body");
        }
        return body;
    }
    
    public record TransformResult(byte[] body, HttpHeaders headers) {}
    
    public record TransformRule(TransformType type, String key, String value) {}
    
    public enum TransformType {
        ADD_HEADER, REMOVE_HEADER, REPLACE_HEADER,
        ADD_BODY_FIELD, REMOVE_BODY_FIELD, REPLACE_BODY_FIELD
    }
}
