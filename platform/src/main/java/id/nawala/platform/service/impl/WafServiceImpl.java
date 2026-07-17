package id.nawala.platform.service.impl;

import id.nawala.platform.logging.SecurityLogger;
import id.nawala.platform.model.WafRule;
import id.nawala.platform.repository.WafRuleRepository;
import id.nawala.platform.service.WafService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class WafServiceImpl implements WafService {

    private final WafRuleRepository wafRuleRepository;

    // Built-in patterns
    private static final Pattern SQL_INJECTION = Pattern.compile(
            "(?i)(union\\s+select|drop\\s+table|insert\\s+into|delete\\s+from|update\\s+.*set|" +
            "or\\s+1\\s*=\\s*1|'\\s*or\\s*'|\"\\s*or\\s*\"|;\\s*--)", Pattern.CASE_INSENSITIVE);

    private static final Pattern XSS = Pattern.compile(
            "(?i)(<script|javascript:|on\\w+\\s*=|<iframe|<object|<embed|alert\\s*\\(|" +
            "document\\.cookie|document\\.write|eval\\s*\\()", Pattern.CASE_INSENSITIVE);

    private static final Pattern PATH_TRAVERSAL = Pattern.compile(
            "(\\.\\./|\\.\\.\\\\|%2e%2e|%252e%252e)", Pattern.CASE_INSENSITIVE);

    @Override
    @Transactional
    public WafRule createRule(String name, String ruleType, String pattern,
                             String action, String targetField, Long routeId,
                             int priority, String description) {
        WafRule rule = WafRule.builder()
                .name(name).ruleType(ruleType).pattern(pattern)
                .action(action).targetField(targetField).routeId(routeId)
                .priority(priority).description(description).active(true)
                .build();
        return wafRuleRepository.save(rule);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WafRule> getAllActiveRules() {
        return wafRuleRepository.findByActiveTrueOrderByPriorityAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WafRule> getRulesForRoute(Long routeId) {
        return wafRuleRepository.findByRouteIdAndActiveTrueOrderByPriorityAsc(routeId);
    }

    @Override
    @Transactional
    public WafInspectionResult inspect(String path, String queryString,
                                        String body, Map<String, String> headers, Long routeId) {
        String fullInput = buildInput(path, queryString, body, headers);

        // Built-in checks
        if (SQL_INJECTION.matcher(fullInput).find()) {
            SecurityLogger.log().warn("WAF BLOCKED sql_injection path={}", path);
            return new WafInspectionResult(true, "BUILT_IN_SQL_INJECTION", "BLOCK", "SQL injection pattern detected");
        }
        if (XSS.matcher(fullInput).find()) {
            SecurityLogger.log().warn("WAF BLOCKED xss path={}", path);
            return new WafInspectionResult(true, "BUILT_IN_XSS", "BLOCK", "XSS pattern detected");
        }
        if (PATH_TRAVERSAL.matcher(path + (queryString != null ? queryString : "")).find()) {
            SecurityLogger.log().warn("WAF BLOCKED path_traversal path={}", path);
            return new WafInspectionResult(true, "BUILT_IN_PATH_TRAVERSAL", "BLOCK", "Path traversal detected");
        }

        // Custom rules
        List<WafRule> rules = routeId != null
                ? wafRuleRepository.findByRouteIdAndActiveTrueOrderByPriorityAsc(routeId)
                : wafRuleRepository.findByRouteIdIsNullAndActiveTrueOrderByPriorityAsc();

        for (WafRule rule : rules) {
            String target = getTargetContent(rule.getTargetField(), path, queryString, body, headers);
            if (rule.getPattern() != null && Pattern.compile(rule.getPattern(), Pattern.CASE_INSENSITIVE).matcher(target).find()) {
                rule.setMatchCount(rule.getMatchCount() + 1);
                wafRuleRepository.save(rule);
                SecurityLogger.log().warn("WAF {} rule={} path={}", rule.getAction(), rule.getName(), path);
                if ("BLOCK".equals(rule.getAction())) {
                    return new WafInspectionResult(true, rule.getName(), rule.getAction(), rule.getDescription());
                }
            }
        }

        return new WafInspectionResult(false, null, null, null);
    }

    @Override
    @Transactional
    public void deleteRule(Long ruleId) {
        wafRuleRepository.deleteById(ruleId);
    }

    @Override
    @Transactional
    public void toggleRule(Long ruleId, boolean active) {
        wafRuleRepository.findById(ruleId).ifPresent(r -> {
            r.setActive(active);
            wafRuleRepository.save(r);
        });
    }

    private String buildInput(String path, String query, String body, Map<String, String> headers) {
        StringBuilder sb = new StringBuilder();
        if (path != null) sb.append(path).append(" ");
        if (query != null) sb.append(query).append(" ");
        if (body != null) sb.append(body).append(" ");
        if (headers != null) headers.values().forEach(v -> sb.append(v).append(" "));
        return sb.toString();
    }

    private String getTargetContent(String field, String path, String query, String body, Map<String, String> headers) {
        if (field == null || "ALL".equals(field)) return buildInput(path, query, body, headers);
        return switch (field) {
            case "PATH" -> path != null ? path : "";
            case "QUERY_PARAM" -> query != null ? query : "";
            case "BODY" -> body != null ? body : "";
            case "HEADER" -> headers != null ? String.join(" ", headers.values()) : "";
            default -> buildInput(path, query, body, headers);
        };
    }
}
