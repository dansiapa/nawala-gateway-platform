package id.nawala.platform.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Security filter that protects /internal/** endpoints with a shared secret.
 * The gateway must send X-Internal-Secret header matching the configured secret.
 * This provides defense-in-depth alongside network isolation.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class InternalApiSecurityFilter extends OncePerRequestFilter {

    private static final String INTERNAL_PATH_PREFIX = "/internal/";
    private static final String SECRET_HEADER = "X-Internal-Secret";

    @Value("${nawala.internal.secret:NawalaInternalSecretKey2024!}")
    private String internalSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (path.startsWith(INTERNAL_PATH_PREFIX)) {
            String providedSecret = request.getHeader(SECRET_HEADER);
            if (providedSecret == null || !providedSecret.equals(internalSecret)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Unauthorized internal access\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
