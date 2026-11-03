
package kh.edu.istad.platform.order;

import kh.edu.istad.platform.order.application.dto.QueryCustomerResult;
import kh.edu.istad.platform.order.application.dto.QueryBusinessResult;
import kh.edu.istad.platform.order.client.httpclient.BusinessHttpClient;
import kh.edu.istad.platform.order.client.port.out.client.CustomerServiceClient;
import kh.edu.istad.platform.order.config.ServicePropConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@RequiredArgsConstructor
@RefreshScope
@RestController
public class WebFluxController {

    private final CustomerServiceClient customerServiceClient;
    private final BusinessHttpClient businessHttpClient;
    private final ServicePropConfig servicePropConfig;

    @Value("${global.version:unknown}")
    private String globalVersion;

    @GetMapping("/customers")
    public Flux<QueryCustomerResult> getCustomers() {
        return customerServiceClient.getCustomerNames();
    }

    @GetMapping("/businesses")
    public Flux<QueryBusinessResult> getBusinesses() {
        return businessHttpClient.queryBusinesses();
    }

    @GetMapping("/flux")
    public Flux<String> getFluxData() {
        return Flux.just("A", "B", "C", "D", "E", "F")
                .log()
                .delayElements(Duration.ofSeconds(1));
    }

    @GetMapping("/consul/config")
    public Mono<Map<String, Object>> getConsulConfig() {
        return Mono.just(Map.of(
                "globalVersion", globalVersion,
                "env", servicePropConfig.getEnv() != null
                        ? servicePropConfig.getEnv() : "not-configured",
                "version", servicePropConfig.getVersion() != null
                        ? servicePropConfig.getVersion() : "not-configured",
                "message", servicePropConfig.getMessage() != null
                        ? servicePropConfig.getMessage() : "not-configured",
                "baseUrl", servicePropConfig.getBaseUrl() != null
                        ? servicePropConfig.getBaseUrl() : "not-configured"
        ));
    }
}
