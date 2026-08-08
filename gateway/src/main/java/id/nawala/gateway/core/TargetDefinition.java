package id.nawala.gateway.core;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TargetDefinition {
    private Long id;
    private String url;
    private int weight;
    private boolean healthy;
}
