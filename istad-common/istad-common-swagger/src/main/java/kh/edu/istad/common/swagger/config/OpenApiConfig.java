package kh.edu.istad.common.swagger.config;


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.autoconfigure.AutoConfiguration;


@AutoConfiguration
@OpenAPIDefinition(
        info = @Info(
                title = "ISTAD-Platform-API",
                version = "1.0",
                description = "API for ISTAD Platform"
        )

)
public class OpenApiConfig {

}
