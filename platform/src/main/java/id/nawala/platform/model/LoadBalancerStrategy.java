package id.nawala.platform.model;

public enum LoadBalancerStrategy {
    ROUND_ROBIN,
    WEIGHTED,
    LEAST_CONN,
    IP_HASH,
    RANDOM;
    
    public String getDisplayName() {
        return switch (this) {
            case ROUND_ROBIN -> "Round Robin";
            case WEIGHTED -> "Weighted";
            case LEAST_CONN -> "Least Connections";
            case IP_HASH -> "IP Hash";
            case RANDOM -> "Random";
        };
    }
}
