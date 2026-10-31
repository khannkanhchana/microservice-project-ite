package kh.edu.istad.platform.order.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "service")
public class ServicePropConfig {
    private String message;
    private String env;
    private String version;
    private String baseUrl;
}
