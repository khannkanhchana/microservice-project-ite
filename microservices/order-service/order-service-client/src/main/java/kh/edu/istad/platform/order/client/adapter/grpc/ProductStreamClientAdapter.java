package kh.edu.istad.platform.order.client.adapter.grpc;

import kh.edu.istad.platform.order.application.dto.QueryProductResult;
import kh.edu.istad.platform.order.application.port.out.client.ProductStreamClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * gRPC adapter that implements the {@link ProductStreamClient} output port.
 *
 * <p>NOTE: The current {@code product.proto} does not define a server-streaming
 * RPC (e.g., {@code rpc ListProducts(...) returns (stream ProductResponse)}),
 * so full streaming is not yet available via gRPC. This adapter returns an empty
 * {@link Flux} as a safe placeholder. Once a streaming RPC is added to the proto,
 * this adapter should be updated to call the generated stub accordingly.</p>
 */
@Slf4j
@Component
public class ProductStreamClientAdapter implements ProductStreamClient {

    @Override
    public Flux<QueryProductResult> listProducts() {
        log.warn(
                "ProductStreamClientAdapter.listProducts() called, but no server-streaming " +
                "RPC is defined in product.proto. Returning empty stream. " +
                "Add 'rpc ListProducts(ListProductsRequest) returns (stream ProductResponse)' " +
                "to the proto and update this adapter."
        );
        return Flux.empty();
    }
}
