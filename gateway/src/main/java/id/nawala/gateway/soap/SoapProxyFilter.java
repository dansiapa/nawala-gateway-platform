package id.nawala.gateway.soap;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SoapProxyFilter implements GlobalFilter, Ordered {

    private static final String SOAP_CONTENT_TYPE = "text/xml";
    private static final String SOAP_ACTION_HEADER = "SOAPAction";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String contentType = exchange.getRequest().getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        String soapAction = exchange.getRequest().getHeaders().getFirst(SOAP_ACTION_HEADER);
        
        if (isSoapRequest(contentType, soapAction)) {
            log.debug("SOAP request detected, action: {}", soapAction);
            exchange.getAttributes().put("isSoap", true);
            exchange.getAttributes().put("soapAction", soapAction);
            
            exchange.getRequest().mutate()
                .header("X-SOAP-Gateway", "nawala")
                .build();
        }
        
        return chain.filter(exchange);
    }

    private boolean isSoapRequest(String contentType, String soapAction) {
        return (contentType != null && (contentType.contains(SOAP_CONTENT_TYPE) || 
                contentType.contains("application/soap+xml"))) || soapAction != null;
    }

    @Override
    public int getOrder() {
        return -3;
    }
}
