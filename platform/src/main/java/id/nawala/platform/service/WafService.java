package id.nawala.platform.service;

import id.nawala.platform.model.WafRule;
import java.util.List;

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

    record WafInspectionResult(
            boolean blocked,
            String matchedRule,
            String action,
            String reason
    ) {}
}
