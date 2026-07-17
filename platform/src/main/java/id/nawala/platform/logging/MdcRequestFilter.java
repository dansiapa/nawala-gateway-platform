package id.nawala.platform.logging;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that populates MDC (Mapped Diagnostic Context) with
 * request-scoped data for structured logging:
 * - traceId: unique per request (UUID)
 * - clientIp: remote address
 * - userId: authenticated username
 * - method: HTTP method
 * - path: request URI
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcRequestFilter implements Filter {

    private static final String TRACE_ID = "traceId";
    private static final String CLIENT_IP = "clientIp";
    private static final String USER_ID = "userId";
    private static final String METHOD = "method";
    private static final String PATH = "path";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // Generate or extract trace ID
            String traceId = httpRequest.getHeader("X-Trace-Id");
            if (traceId == null || traceId.isBlank()) {
                traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            }

            // Populate MDC
            MDC.put(TRACE_ID, traceId);
            MDC.put(CLIENT_IP, getClientIp(httpRequest));
            MDC.put(METHOD, httpRequest.getMethod());
            MDC.put(PATH, httpRequest.getRequestURI());

            // Add authenticated user if available
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                MDC.put(USER_ID, auth.getName());
            }

            // Propagate trace ID in response header
            httpResponse.setHeader("X-Trace-Id", traceId);

            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}
