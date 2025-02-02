package org.example.gateway.filter;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gateway.support.RateLimiter;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final RateLimiter rateLimiter;
    private final DiscoveryClient discoveryClient;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        URI uri = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);

        if (uri != null) {
            String host = uri.getHost();
            int port = uri.getPort();

            List<String> services = discoveryClient.getServices();
            for (String service : services) {
                List<ServiceInstance> instances = discoveryClient.getInstances(service);

                Optional<ServiceInstance> matchedInstance = instances.stream().filter(
                        instance -> instance.getHost().equals(host) && instance.getPort() == port)
                    .findFirst();

                if (matchedInstance.isPresent()) {
                    String instanceId = matchedInstance.get().getInstanceId();

                    Instant now = Instant.now();
                    long currentTime = now.getEpochSecond();
                    long currentTimeWithoutSeconds = now.truncatedTo(ChronoUnit.MINUTES)
                        .getEpochSecond();

                    try {
                        long result = rateLimiter.checkRequestCount(instanceId, currentTime,
                            currentTimeWithoutSeconds);

                        if (result == -1) {
                            return onError(exchange, "1분당 요청수 초과", HttpStatus.TOO_MANY_REQUESTS);
                        }

                        exchange.getRequest().mutate()
                            .header("requestTime", String.valueOf(currentTime)).build();

                        return chain.filter(exchange)
                            .then(Mono.fromRunnable(() -> log.info("처리율 제한 필터 통과")));
                    } catch (Exception e) {
                        return onError(exchange, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
            }

        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);

        log.error(message);

        return response.setComplete();
    }
}
