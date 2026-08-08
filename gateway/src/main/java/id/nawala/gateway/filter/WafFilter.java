package id.nawala.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Component
@Slf4j
public class WafFilter {
    
    private final Set<String> blockedIps = ConcurrentHashMap.newKeySet();
    private volatile boolean sqlInjectionEnabled = true;
    private volatile boolean xssEnabled = true;
    private volatile boolean pathTraversalEnabled = true;
    
    // SQL Injection patterns
    private static final Pattern[] SQL_PATTERNS = {
        Pattern.compile("(?i)(\\b(SELECT|INSERT|UPDATE|DELETE|DROP|UNION|ALTER|CREATE|TRUNCATE)\\b.*\\b(FROM|INTO|TABLE|DATABASE)\\b)"),
        Pattern.compile("(?i)(--|#|/\\*|\\*/|;\\s*$)"),
        Pattern.compile("(?i)'\\s*(OR|AND)\\s*'?\\d*'?\\s*=\\s*'?\\d*"),
        Pattern.compile("(?i)'\\s*(OR|AND)\\s+'[^']*'\\s*=\\s*'[^']*'"),
        Pattern.compile("(?i)\\bEXEC(UTE)?\\b|\\bXP_"),
    };
    
    // XSS patterns
    private static final Pattern[] XSS_PATTERNS = {
        Pattern.compile("(?i)<script[^>]*>.*?</script>"),
        Pattern.compile("(?i)<[^>]+(on\\w+\\s*=)[^>]*>"),
        Pattern.compile("(?i)javascript\\s*:"),
        Pattern.compile("(?i)<iframe[^>]*>"),
        Pattern.compile("(?i)<object[^>]*>"),
        Pattern.compile("(?i)<embed[^>]*>"),
    };
    
    // Path traversal patterns
    private static final Pattern[] PATH_PATTERNS = {
        Pattern.compile("\\.\\./"),
        Pattern.compile("\\.\\.\\\\"),
        Pattern.compile("%2e%2e[%2f%5c]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)/etc/passwd"),
        Pattern.compile("(?i)/proc/self"),
    };
    
    public WafResult check(String clientIp, String path, String queryString, String body, java.util.Map<String, String> headers) {
        // Check blocked IPs
        if (blockedIps.contains(clientIp)) {
            log.warn("Blocked IP attempted access: {}", clientIp);
            return WafResult.blocked("IP_BLOCKED", "Your IP has been blocked");
        }
        
        String fullInput = buildFullInput(path, queryString, body);
        
        // SQL Injection check
        if (sqlInjectionEnabled && containsSqlInjection(fullInput)) {
            log.warn("SQL Injection attempt from {}: {}", clientIp, truncate(fullInput));
            return WafResult.blocked("SQL_INJECTION", "Potential SQL injection detected");
        }
        
        // XSS check
        if (xssEnabled && containsXss(fullInput)) {
            log.warn("XSS attempt from {}: {}", clientIp, truncate(fullInput));
            return WafResult.blocked("XSS", "Potential XSS attack detected");
        }
        
        // Path traversal check
        if (pathTraversalEnabled && containsPathTraversal(path + (queryString != null ? queryString : ""))) {
            log.warn("Path traversal attempt from {}: {}", clientIp, path);
            return WafResult.blocked("PATH_TRAVERSAL", "Potential path traversal detected");
        }
        
        return WafResult.allow();
    }
    
    private String buildFullInput(String path, String queryString, String body) {
        StringBuilder sb = new StringBuilder();
        if (path != null) sb.append(path).append(" ");
        if (queryString != null) sb.append(queryString).append(" ");
        if (body != null) sb.append(body);
        return sb.toString();
    }
    
    private boolean containsSqlInjection(String input) {
        for (Pattern p : SQL_PATTERNS) {
            if (p.matcher(input).find()) return true;
        }
        return false;
    }
    
    private boolean containsXss(String input) {
        for (Pattern p : XSS_PATTERNS) {
            if (p.matcher(input).find()) return true;
        }
        return false;
    }
    
    private boolean containsPathTraversal(String input) {
        for (Pattern p : PATH_PATTERNS) {
            if (p.matcher(input).find()) return true;
        }
        return false;
    }
    
    private String truncate(String s) {
        return s.length() > 100 ? s.substring(0, 100) + "..." : s;
    }
    
    public void blockIp(String ip) {
        blockedIps.add(ip);
        log.info("Blocked IP: {}", ip);
    }
    
    public void unblockIp(String ip) {
        blockedIps.remove(ip);
        log.info("Unblocked IP: {}", ip);
    }
    
    public Set<String> getBlockedIps() {
        return Set.copyOf(blockedIps);
    }
    
    public void setFeatureEnabled(String feature, boolean enabled) {
        switch (feature.toLowerCase()) {
            case "sql_injection" -> sqlInjectionEnabled = enabled;
            case "xss" -> xssEnabled = enabled;
            case "path_traversal" -> pathTraversalEnabled = enabled;
        }
    }
    
    public record WafResult(boolean allowed, String code, String message) {
        public static WafResult allow() { return new WafResult(true, null, null); }
        public static WafResult blocked(String code, String message) { return new WafResult(false, code, message); }
    }
}
