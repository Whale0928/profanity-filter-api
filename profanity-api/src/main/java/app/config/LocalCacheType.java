package app.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LocalCacheType {
    REQUEST_FILTER("request_filter", 60 * 60 * 24, 1000),
    REQUEST_CLIENT_INFO("request_client_info", 60 * 60 * 24, 100); //

    private final String cacheName;
    private final int secsToExpireAfterWrite;
    private final int entryMaxSize;
}
