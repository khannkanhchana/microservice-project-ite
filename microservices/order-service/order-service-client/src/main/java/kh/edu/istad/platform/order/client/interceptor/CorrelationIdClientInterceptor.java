package kh.edu.istad.platform.order.client.interceptor;

// This interceptor intercepts every outgoing gRPC call,
// generates a correlation ID (or reads an existing one),
// and attaches it to the gRPC Metadata.

import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@GrpcGlobalClientInterceptor
public class CorrelationIdClientInterceptor implements ClientInterceptor {

    public static final Metadata.Key<String> CORRELATION_ID_KEY =
            Metadata.Key.of("x-correlation-id",  Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions, Channel next
    ) {
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {

                // Generate a unique Correlation ID for this request
                String correlationId = UUID.randomUUID().toString();
                headers.put(CORRELATION_ID_KEY, correlationId);

                log.info("Client Interceptor: Attached x-correlation-id [{}] to call [{}]",
                        correlationId, method.getFullMethodName());

                super.start(responseListener, headers);

            }
        };
    }
}