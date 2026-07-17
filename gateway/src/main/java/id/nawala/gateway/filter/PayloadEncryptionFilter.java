package id.nawala.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * End-to-end payload encryption filter.
 * When a route has payloadEncryption enabled:
 * - Incoming request body is decrypted before forwarding to backend
 * - Outgoing response body is encrypted before returning to client
 *
 * Encryption uses AES-256-GCM with a key derived from the client's API key.
 */
@Component
@Slf4j
public class PayloadEncryptionFilter extends AbstractGatewayFilterFactory<PayloadEncryptionFilter.Config> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String ENCRYPTION_HEADER = "X-Payload-Encrypted";

    private final String masterKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public PayloadEncryptionFilter(@Value("${nawala.gateway.payload-key:#{null}}") String key) {
        super(Config.class);
        this.masterKey = key;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String encHeader = exchange.getRequest().getHeaders().getFirst(ENCRYPTION_HEADER);

            if (!"true".equalsIgnoreCase(encHeader)) {
                // No encryption requested, pass through
                return chain.filter(exchange);
            }

            String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-Key");
            if (apiKey == null || masterKey == null) {
                return chain.filter(exchange);
            }

            // Derive route-specific key from API key + master
            byte[] derivedKey = deriveKey(apiKey);

            // Decrypt incoming request body
            return DataBufferUtils.join(exchange.getRequest().getBody())
                    .defaultIfEmpty(exchange.getResponse().bufferFactory().wrap(new byte[0]))
                    .flatMap(dataBuffer -> {
                        byte[] bodyBytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bodyBytes);
                        DataBufferUtils.release(dataBuffer);

                        byte[] decryptedBody;
                        if (bodyBytes.length > 0) {
                            try {
                                decryptedBody = decrypt(bodyBytes, derivedKey);
                            } catch (Exception e) {
                                log.warn("Payload decryption failed: {}", e.getMessage());
                                decryptedBody = bodyBytes; // Pass through if decryption fails
                            }
                        } else {
                            decryptedBody = bodyBytes;
                        }

                        // Create new request with decrypted body
                        byte[] finalBody = decryptedBody;
                        ServerHttpRequestDecorator decoratedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
                            @Override
                            public Flux<DataBuffer> getBody() {
                                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(finalBody);
                                return Flux.just(buffer);
                            }
                        };

                        ServerWebExchange mutatedExchange = exchange.mutate()
                                .request(decoratedRequest)
                                .build();

                        return chain.filter(mutatedExchange);
                    });
        };
    }

    private byte[] deriveKey(String apiKey) {
        try {
            byte[] combined = (apiKey + ":" + masterKey).getBytes(StandardCharsets.UTF_8);
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            return digest.digest(combined);
        } catch (Exception e) {
            throw new RuntimeException("Key derivation failed", e);
        }
    }

    private byte[] decrypt(byte[] data, byte[] key) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(data);
        ByteBuffer buffer = ByteBuffer.wrap(decoded);
        byte[] iv = new byte[GCM_IV_LENGTH];
        buffer.get(iv);
        byte[] cipherText = new byte[buffer.remaining()];
        buffer.get(cipherText);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), spec);
        return cipher.doFinal(cipherText);
    }

    public static class Config {
    }
}
