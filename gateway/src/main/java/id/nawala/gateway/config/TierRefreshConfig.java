package id.nawala.gateway.config;

import id.nawala.gateway.filter.TierRateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Periodically refreshes tier rate limits from platform.
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class TierRefreshConfig {

    private final TierRateLimitFilter tierRateLimitFilter;

    @Scheduled(fixedDelay = 60000, initialDelay = 5000)
    public void refreshTiers() {
        tierRateLimitFilter.refreshTiers();
    }
}
