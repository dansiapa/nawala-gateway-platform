package id.nawala.gateway.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dedicated security logger for gateway auth events.
 * Routes to security.log via logback.
 */
public final class SecurityLogger {

    private static final Logger INSTANCE = LoggerFactory.getLogger(SecurityLogger.class);

    private SecurityLogger() {}

    public static Logger log() {
        return INSTANCE;
    }
}
