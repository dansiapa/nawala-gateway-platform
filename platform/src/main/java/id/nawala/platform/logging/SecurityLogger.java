package id.nawala.platform.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dedicated security logger for authentication events, threat detection,
 * and access control decisions. Routes to security.log via logback.
 *
 * Usage:
 *   SecurityLogger.log().info("Login success user={}", username);
 *   SecurityLogger.log().warn("Brute force detected ip={}", ip);
 */
public final class SecurityLogger {

    private static final Logger INSTANCE = LoggerFactory.getLogger(SecurityLogger.class);

    private SecurityLogger() {}

    public static Logger log() {
        return INSTANCE;
    }
}
