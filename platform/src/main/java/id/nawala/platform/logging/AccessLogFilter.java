package id.nawala.platform.logging;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Access log filter that records every HTTP request in a dedicated access log.
 * Format follows Combined Log Format extended with response time.
 * Output goes to access.log via logback routing.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AccessLogFilter implements Filter {

    private static final Logger ACCESS_LOG = LoggerFactory.getLogger(AccessLogger.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        long startTime = System.currentTimeMillis();

        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = httpResponse.getStatus();
            String method = httpRequest.getMethod();
            String path = httpRequest.getRequestURI();
            String query = httpRequest.getQueryString();
            String fullPath = query != null ? path + "?" + query : path;

            ACCESS_LOG.info("{} {} {} {}ms", method, fullPath, status, duration);
        }
    }
}
