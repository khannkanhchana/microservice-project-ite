package kh.edu.istad.platform.order;



import kh.edu.istad.platform.order.client.port.out.client.CustomerServiceClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;

@RestController
public class WebFluxController {

    private final CustomerServiceClient customerServiceClient;

    public WebFluxController(CustomerServiceClient customerServiceClient) {
        this.customerServiceClient = customerServiceClient;
    }

    @GetMapping("customers")
    public Flux<String> getCustomers(){
        return customerServiceClient.getCustomerNames();
    }


    @GetMapping("/flux")
    public Flux<String> getFluxData() {
        return Flux.just("A", "B", "C", "D", "E", "F")
                .log()
                .delayElements(Duration.ofSeconds(1));
    }

}