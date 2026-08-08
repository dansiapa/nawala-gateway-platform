package id.nawala.platform.config;

import id.nawala.platform.service.SetupService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
@Order(1)
@RequiredArgsConstructor
public class SetupRedirectFilter extends OncePerRequestFilter {

    private final SetupService setupService;
    
    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/setup", "/css", "/js", "/img", "/favicon.ico", "/error"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // Allow static resources and setup page
        if (isAllowedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // If setup not completed, redirect to setup
        if (!setupService.isSetupCompleted()) {
            response.sendRedirect("/setup");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
    
    private boolean isAllowedPath(String path) {
        for (String allowed : ALLOWED_PATHS) {
            if (path.equals(allowed) || path.startsWith(allowed + "/")) {
                return true;
            }
        }
        return false;
    }
}
