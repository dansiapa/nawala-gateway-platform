package id.nawala.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WafFilterTest {
    
    private WafFilter wafFilter;
    
    @BeforeEach
    void setUp() {
        wafFilter = new WafFilter();
    }
    
    @Test
    void testCheck_NormalRequest() {
        WafFilter.WafResult result = wafFilter.check(
            "192.168.1.1", "/api/users", "name=john", null, null
        );
        
        assertTrue(result.allowed());
    }
    
    @Test
    void testCheck_SqlInjection() {
        // Test SQL injection in query
        WafFilter.WafResult result = wafFilter.check(
            "192.168.1.1", "/api/users", "id=1' OR '1'='1", null, null
        );
        
        assertFalse(result.allowed());
        assertEquals("SQL_INJECTION", result.code());
    }
    
    @Test
    void testCheck_SqlInjection_Union() {
        WafFilter.WafResult result = wafFilter.check(
            "192.168.1.1", "/api/users", null, 
            "SELECT * FROM users UNION SELECT * FROM passwords", null
        );
        
        assertFalse(result.allowed());
        assertEquals("SQL_INJECTION", result.code());
    }
    
    @Test
    void testCheck_XssAttack() {
        WafFilter.WafResult result = wafFilter.check(
            "192.168.1.1", "/api/comment", null,
            "<script>alert('xss')</script>", null
        );
        
        assertFalse(result.allowed());
        assertEquals("XSS", result.code());
    }
    
    @Test
    void testCheck_XssOnclick() {
        WafFilter.WafResult result = wafFilter.check(
            "192.168.1.1", "/api/comment", null,
            "<img src=x onerror=alert('xss')>", null
        );
        
        assertFalse(result.allowed());
        assertEquals("XSS", result.code());
    }
    
    @Test
    void testCheck_PathTraversal() {
        WafFilter.WafResult result = wafFilter.check(
            "192.168.1.1", "/api/files/../../../etc/passwd", null, null, null
        );
        
        assertFalse(result.allowed());
        assertEquals("PATH_TRAVERSAL", result.code());
    }
    
    @Test
    void testCheck_BlockedIp() {
        String blockedIp = "10.0.0.1";
        wafFilter.blockIp(blockedIp);
        
        WafFilter.WafResult result = wafFilter.check(
            blockedIp, "/api/users", null, null, null
        );
        
        assertFalse(result.allowed());
        assertEquals("IP_BLOCKED", result.code());
    }
    
    @Test
    void testBlockAndUnblockIp() {
        String ip = "10.0.0.2";
        
        // Initially not blocked
        assertTrue(wafFilter.check(ip, "/", null, null, null).allowed());
        
        // Block
        wafFilter.blockIp(ip);
        assertFalse(wafFilter.check(ip, "/", null, null, null).allowed());
        assertTrue(wafFilter.getBlockedIps().contains(ip));
        
        // Unblock
        wafFilter.unblockIp(ip);
        assertTrue(wafFilter.check(ip, "/", null, null, null).allowed());
        assertFalse(wafFilter.getBlockedIps().contains(ip));
    }
    
    @Test
    void testDisableFeature() {
        // Disable SQL injection check
        wafFilter.setFeatureEnabled("sql_injection", false);
        
        WafFilter.WafResult result = wafFilter.check(
            "192.168.1.1", "/api/users", "id=1' OR '1'='1", null, null
        );
        
        // Should allow since SQL injection check is disabled
        assertTrue(result.allowed());
    }
}
