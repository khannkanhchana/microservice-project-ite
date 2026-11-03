package kh.edu.istad.platform.order.client.httpclient;


import kh.edu.istad.platform.order.application.dto.QueryBusinessResult;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import reactor.core.publisher.Flux;

@HttpExchange("/api/businesses")
public interface BusinessHttpClient {
    @GetExchange
    Flux<QueryBusinessResult> queryBusinesses();
}
