package id.nawala.platform.sso;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import javax.naming.*;
import javax.naming.directory.*;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SsoService {

    private final SsoRepository ssoRepository;

    public List<SsoConfig> getAllConfigs() {
        return ssoRepository.findAll();
    }

    public Optional<SsoConfig> getDefaultConfig() {
        return ssoRepository.findByIsDefaultTrue();
    }

    public SsoConfig save(SsoConfig config) {
        if (config.isDefault()) {
            ssoRepository.findByIsDefaultTrue().ifPresent(existing -> {
                existing.setDefault(false);
                ssoRepository.save(existing);
            });
        }
        return ssoRepository.save(config);
    }

    public void delete(Long id) {
        ssoRepository.deleteById(id);
    }

    public boolean authenticateLdap(SsoConfig config, String username, String password) {
        if (config.getType() != SsoConfig.SsoType.LDAP) return false;
        
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, config.getLdapUrl());
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            
            String userDn = config.getLdapUserFilter()
                .replace("{username}", username)
                .replace("{baseDn}", config.getLdapBaseDn());
            
            env.put(Context.SECURITY_PRINCIPAL, userDn);
            env.put(Context.SECURITY_CREDENTIALS, password);
            
            DirContext ctx = new InitialDirContext(env);
            ctx.close();
            
            log.info("LDAP auth success for user: {}", username);
            return true;
        } catch (AuthenticationException e) {
            log.warn("LDAP auth failed for user: {}", username);
            return false;
        } catch (Exception e) {
            log.error("LDAP error: {}", e.getMessage());
            return false;
        }
    }

    public String generateSamlRequest(SsoConfig config) {
        // Generate SAML AuthnRequest
        String requestId = "_" + UUID.randomUUID().toString();
        String issueInstant = java.time.Instant.now().toString();
        
        return String.format(
            "<samlp:AuthnRequest xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" " +
            "ID=\"%s\" Version=\"2.0\" IssueInstant=\"%s\" " +
            "AssertionConsumerServiceURL=\"%s\">" +
            "<saml:Issuer xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">%s</saml:Issuer>" +
            "</samlp:AuthnRequest>",
            requestId, issueInstant, config.getSamlAcsUrl(), config.getSamlEntityId()
        );
    }
}
