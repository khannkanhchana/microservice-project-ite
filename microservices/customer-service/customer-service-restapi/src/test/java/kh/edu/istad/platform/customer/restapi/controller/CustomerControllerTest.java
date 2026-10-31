package kh.edu.istad.platform.customer.restapi.controller;

import kh.edu.istad.platform.customer.domain.dto.InitiateCustomerResult;
import kh.edu.istad.platform.customer.domain.usecase.*;
import kh.edu.istad.platform.customer.restapi.dto.*;
import kh.edu.istad.platform.customer.restapi.exception.RestAdvisor;
import kh.edu.istad.platform.customer.restapi.mapper.CustomerWebMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CustomerControllerTest {
    private final InitiateCustomerUseCase create = mock(InitiateCustomerUseCase.class);
    private final GetAllCustomersUseCase getAll = mock(GetAllCustomersUseCase.class);
    private final QueryCustomersUseCase query = mock(QueryCustomersUseCase.class);
    private final CustomerWebMapper mapper =
            new kh.edu.istad.platform.customer.restapi.mapper.CustomerWebMapperImpl();
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToController(new CustomerController(create, mapper, getAll, query))
                .controllerAdvice(new RestAdvisor()).build();
    }

    @Test
    void getAllReturnsCustomersWithoutPageWrapper() {
        var customer = kh.edu.istad.platform.customer.domain.entity.Customer.Builder.builder()
                .id(new kh.edu.istad.common.domain.valueobject.CustomerId(UUID.randomUUID()))
                .username("chana")
                .email(new kh.edu.istad.platform.customer.domain.valueobject.Email("c@example.com"))
                .phoneNumber(new kh.edu.istad.platform.customer.domain.valueobject.PhoneNumber("012345678"))
                .build();
        when(getAll.execute()).thenReturn(Flux.just(customer));
        client.get().uri("/api/customers")
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$[0].username").isEqualTo("chana")
                .jsonPath("$[0].content").doesNotExist()
                .jsonPath("$[0].page").doesNotExist();
        verify(getAll).execute();
    }

    @Test
    void queryPassesOptionalFiltersAndReturnsArray() {
        var customer = kh.edu.istad.platform.customer.domain.entity.Customer.Builder.builder()
                .id(new kh.edu.istad.common.domain.valueobject.CustomerId(UUID.randomUUID()))
                .username("chana")
                .email(new kh.edu.istad.platform.customer.domain.valueobject.Email("c@example.com"))
                .phoneNumber(new kh.edu.istad.platform.customer.domain.valueobject.PhoneNumber("012345678"))
                .build();
        when(query.execute("chana", "c@example.com", null)).thenReturn(Flux.just(customer));
        client.get().uri("/api/customers/query?username=chana&email=c@example.com")
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$[0].username").isEqualTo("chana")
                .jsonPath("$[0].email").isEqualTo("c@example.com")
                .jsonPath("$[0].page").doesNotExist();
        verify(query).execute("chana", "c@example.com", null);
    }

    @Test
    void createReturns201() {
        UUID id = UUID.randomUUID();
        var result = new InitiateCustomerResult(id, "chana", "Chan", "Na", "c@example.com", "012345678");
        when(create.execute(any())).thenReturn(Mono.just(result));
        client.post().uri("/api/customers")
                .header("Content-Type", "application/json")
                .bodyValue("""
                        {"username":"chana","familyName":"Chan","givenName":"Na",
                         "email":"c@example.com","phoneNumber":"012345678"}
                        """)
                .exchange().expectStatus().isCreated()
                .expectBody().jsonPath("$.customerId").isEqualTo(id.toString());
    }

    @Test
    void invalidBodyReturns400BeforeCallingUseCase() {
        client.post().uri("/api/customers")
                .header("Content-Type", "application/json").bodyValue("{}")
                .exchange().expectStatus().isBadRequest()
                .expectBody().jsonPath("$.errors.username").exists();
        verifyNoInteractions(create);
    }

    @Test
    void malformedJsonReturns400() {
        client.post().uri("/api/customers")
                .header("Content-Type", "application/json").bodyValue("{")
                .exchange().expectStatus().isBadRequest();
        verifyNoInteractions(create);
    }

    @Test
    void patchEndpointsAreRemoved() {
        client.patch().uri("/api/customers/" + UUID.randomUUID())
                .exchange().expectStatus().isNotFound();
        client.patch().uri("/api/customers/" + UUID.randomUUID() + "/deactivate")
                .exchange().expectStatus().isNotFound();
    }
}
