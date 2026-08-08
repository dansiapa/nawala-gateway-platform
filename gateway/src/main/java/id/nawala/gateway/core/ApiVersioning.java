package id.nawala.gateway.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class ApiVersioning {
    
    private static final Pattern PATH_VERSION_PATTERN = Pattern.compile("^/v(\\d+)(?:\\.\\d+)?(/.*)?$");
    private static final Pattern HEADER_VERSION_PATTERN = Pattern.compile("^v?(\\d+)(?:\\.(\\d+))?$");
    
    public VersionInfo extractVersion(String path, HttpHeaders headers) {
        // 1. Try URL path versioning: /v1/users, /v2/users
        Optional<VersionInfo> pathVersion = extractFromPath(path);
        if (pathVersion.isPresent()) {
            return pathVersion.get();
        }
        
        // 2. Try header versioning: X-API-Version: v1
        String headerVersion = headers.getFirst("X-API-Version");
        if (headerVersion != null) {
            Optional<VersionInfo> hv = parseVersion(headerVersion);
            if (hv.isPresent()) {
                return hv.get().withPath(path);
            }
        }
        
        // 3. Try Accept header versioning: Accept: application/vnd.api.v1+json
        String accept = headers.getFirst("Accept");
        if (accept != null && accept.contains("vnd.")) {
            Optional<VersionInfo> av = extractFromAcceptHeader(accept);
            if (av.isPresent()) {
                return av.get().withPath(path);
            }
        }
        
        // 4. Try query parameter: ?version=1
        // This would need the full URI, handled at controller level
        
        // Default: no version specified
        return new VersionInfo(0, 0, path, VersionSource.NONE);
    }
    
    private Optional<VersionInfo> extractFromPath(String path) {
        Matcher matcher = PATH_VERSION_PATTERN.matcher(path);
        if (matcher.matches()) {
            int major = Integer.parseInt(matcher.group(1));
            String remainingPath = matcher.group(2);
            if (remainingPath == null) remainingPath = "/";
            return Optional.of(new VersionInfo(major, 0, remainingPath, VersionSource.PATH));
        }
        return Optional.empty();
    }
    
    private Optional<VersionInfo> parseVersion(String version) {
        Matcher matcher = HEADER_VERSION_PATTERN.matcher(version.trim());
        if (matcher.matches()) {
            int major = Integer.parseInt(matcher.group(1));
            int minor = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
            return Optional.of(new VersionInfo(major, minor, null, VersionSource.HEADER));
        }
        return Optional.empty();
    }
    
    private Optional<VersionInfo> extractFromAcceptHeader(String accept) {
        // Parse: application/vnd.api.v1+json
        Pattern p = Pattern.compile("vnd\\.[^.]+\\.v(\\d+)");
        Matcher m = p.matcher(accept);
        if (m.find()) {
            int major = Integer.parseInt(m.group(1));
            return Optional.of(new VersionInfo(major, 0, null, VersionSource.ACCEPT_HEADER));
        }
        return Optional.empty();
    }
    
    public String rewritePath(String originalPath, int targetVersion) {
        Matcher matcher = PATH_VERSION_PATTERN.matcher(originalPath);
        if (matcher.matches()) {
            String remaining = matcher.group(2);
            return "/v" + targetVersion + (remaining != null ? remaining : "");
        }
        return "/v" + targetVersion + originalPath;
    }
    
    public record VersionInfo(int major, int minor, String path, VersionSource source) {
        public VersionInfo withPath(String newPath) {
            return new VersionInfo(major, minor, newPath, source);
        }
        
        public boolean hasVersion() {
            return source != VersionSource.NONE;
        }
        
        public String versionString() {
            return minor > 0 ? "v" + major + "." + minor : "v" + major;
        }
    }
    
    public enum VersionSource {
        PATH, HEADER, ACCEPT_HEADER, QUERY_PARAM, NONE
    }
}
