package kh.edu.istad.platform.order;


import kh.edu.istad.platform.order.application.dto.QueryProductResult;

import kh.edu.istad.platform.order.domain.port.out.client.ProductStreamClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class ProductStreamController {
    private final ProductStreamClient productStreamClient;

    public ProductStreamController(ProductStreamClient productStreamClient) {
        this.productStreamClient = productStreamClient;
    }


    //    NDJSON (Newline Delimited JSON) ជា Format ដែលបញ្ជូន JSON Object មួយក្នុងមួយបន្ទាត់។
    @GetMapping(value = "/api/products/stream", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<QueryProductResult> listProducts() {
        return productStreamClient.listProducts();
    }
}

