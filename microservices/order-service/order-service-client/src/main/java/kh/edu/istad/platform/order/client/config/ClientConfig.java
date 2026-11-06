package kh.edu.istad.platform.order.client.config;

import kh.edu.istad.platform.order.client.exception.DownStreamServiceException;
import kh.edu.istad.platform.order.client.httpclient.BusinessHttpClient;
import kh.edu.istad.platform.order.client.httpclient.CustomerHttpClient;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.reactive.function.client.support.WebClientHttpServiceGroupConfigurer;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.service.registry.ImportHttpServices;
import org.springframework.web.service.registry.HttpServiceGroup;



@Configuration

@ImportHttpServices(
        group = "customer-service",
        types = CustomerHttpClient.class,
        clientType = HttpServiceGroup.ClientType.WEB_CLIENT)

@ImportHttpServices(
        group = "business-service",
        types = BusinessHttpClient.class,
        clientType = HttpServiceGroup.ClientType.WEB_CLIENT)

//@RequiredArgsConstructor
public class ClientConfig {

    public WebClientHttpServiceGroupConfigurer webClientHttpServiceGroupConfigurer() {
        return groups -> {
            groups.filterByName("customer-service")
                    .forEachClient((group, clientBuilder) -> {
                        clientBuilder
                                .defaultHeader("User-Agent", "WebClient")
                                .defaultStatusHandler(
                                        HttpStatusCode::isError,
                                        response -> {
                                            return response
                                                    .createException()
                                                    .map(
                                                            ex-> new DownStreamServiceException("customer-service is down",ex.getMessage())
                                                    );
                                        }
                                );

                    });
        };
    }


    //-- use in spring boot version 3 under
//    private final WebClient.Builder webClient;
//    private final ReactorLoadBalancerExchangeFilterFunction loadBalancerFilter;
//
//    @Bean
//    public CustomerHttpClient customerHttpClient() {
//
//        return createClient("http://customer-service", CustomerHttpClient.class);
//    }
//
//    @Bean
//    public BusinessHttpClient businessHttpClient() {
//
//       return createClient("http://business-service", BusinessHttpClient.class);
//    }
//
//
//
//    private <T> T createClient(String url, Class<T> type){
//        WebClient wc = webClient
//                .clone()
//                .filter(loadBalancerFilter)
//                .baseUrl(url)
//                .build();
//
//        return createClient(wc, type);
//    }
//
//    private <T> T createClient(WebClient wc, Class<T> type){
//        HttpServiceProxyFactory factory = HttpServiceProxyFactory
//                .builderFor(WebClientAdapter.create(wc))
//                .build();
//        return factory.createClient(type);
//    }
}
