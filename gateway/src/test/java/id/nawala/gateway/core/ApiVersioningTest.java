package id.nawala.gateway.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import static org.junit.jupiter.api.Assertions.*;

class ApiVersioningTest {
    
    private ApiVersioning apiVersioning;
    
    @BeforeEach
    void setUp() {
        apiVersioning = new ApiVersioning();
    }
    
    @Test
    void testExtractVersion_FromPath() {
        HttpHeaders headers = new HttpHeaders();
        ApiVersioning.VersionInfo info = apiVersioning.extractVersion("/v1/users", headers);
        
        assertEquals(1, info.major());
        assertEquals("/users", info.path());
        assertEquals(ApiVersioning.VersionSource.PATH, info.source());
    }
    
    @Test
    void testExtractVersion_FromPath_V2() {
        HttpHeaders headers = new HttpHeaders();
        ApiVersioning.VersionInfo info = apiVersioning.extractVersion("/v2/products/123", headers);
        
        assertEquals(2, info.major());
        assertEquals("/products/123", info.path());
    }
    
    @Test
    void testExtractVersion_FromHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-API-Version", "v3");
        
        ApiVersioning.VersionInfo info = apiVersioning.extractVersion("/users", headers);
        
        assertEquals(3, info.major());
        assertEquals(ApiVersioning.VersionSource.HEADER, info.source());
    }
    
    @Test
    void testExtractVersion_FromAcceptHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "application/vnd.api.v2+json");
        
        ApiVersioning.VersionInfo info = apiVersioning.extractVersion("/users", headers);
        
        assertEquals(2, info.major());
        assertEquals(ApiVersioning.VersionSource.ACCEPT_HEADER, info.source());
    }
    
    @Test
    void testExtractVersion_NoVersion() {
        HttpHeaders headers = new HttpHeaders();
        ApiVersioning.VersionInfo info = apiVersioning.extractVersion("/users", headers);
        
        assertFalse(info.hasVersion());
        assertEquals(ApiVersioning.VersionSource.NONE, info.source());
    }
    
    @Test
    void testRewritePath() {
        String rewritten = apiVersioning.rewritePath("/v1/users", 2);
        assertEquals("/v2/users", rewritten);
    }
    
    @Test
    void testRewritePath_NoExistingVersion() {
        String rewritten = apiVersioning.rewritePath("/users", 1);
        assertEquals("/v1/users", rewritten);
    }
    
    @Test
    void testVersionString() {
        ApiVersioning.VersionInfo info = new ApiVersioning.VersionInfo(1, 0, "/users", ApiVersioning.VersionSource.PATH);
        assertEquals("v1", info.versionString());
        
        ApiVersioning.VersionInfo info2 = new ApiVersioning.VersionInfo(2, 1, "/users", ApiVersioning.VersionSource.HEADER);
        assertEquals("v2.1", info2.versionString());
    }
}
