package id.nawala.platform.service;

import id.nawala.platform.model.WafRule;
import java.util.List;
import java.util.Set;

/**
 * WAF (Web Application Firewall) service.
 */
public interface WafService {

    WafRule createRule(String name, String ruleType, String pattern,
                      String action, String targetField, Long routeId,
                      int priority, String description);

    List<WafRule> getAllActiveRules();

    List<WafRule> getRulesForRoute(Long routeId);

    WafInspectionResult inspect(String path, String queryString,
                                 String body, java.util.Map<String, String> headers, Long routeId);

    void deleteRule(Long ruleId);

    void toggleRule(Long ruleId, boolean active);
    
    // === Additional methods for UI ===
    boolean isEnabled();
    
    void toggleFeature(String feature, boolean enabled);
    
    Set<String> getBlockedIps();
    
    void blockIp(String ip, String reason);
    
    void unblockIp(String ip);

    record WafInspectionResult(
            boolean blocked,
            String matchedRule,
            String action,
            String reason
    ) {}
}
