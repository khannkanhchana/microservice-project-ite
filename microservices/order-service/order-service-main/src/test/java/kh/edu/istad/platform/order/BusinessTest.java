package kh.edu.istad.platform.order;

import kh.edu.istad.platform.order.persistence.repository.BusinessReactiveRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.cloud.consul.discovery.register=false")



public class BusinessTest {
    @Autowired
    private BusinessReactiveRepository businessReactiveRepository;


    @Test
    void testBusiness(){
        var businesses = businessReactiveRepository.findAll()
                .collectList().block(Duration.ofSeconds(10));
        assertNotNull(businesses);
        assertFalse(businesses.isEmpty(), "The business seed migration should supply products");



    }
}
