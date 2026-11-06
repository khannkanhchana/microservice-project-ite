package kh.edu.istad.platform.product.server.interceptor;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@GrpcGlobalServerInterceptor
public class CorrelationIdServerInterceptor implements ServerInterceptor {

    public static final Metadata.Key<String> CORRELATION_ID_KEY =
            Metadata.Key.of("x-correlation-id", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next
    ) {

        String correlationId = headers.get(CORRELATION_ID_KEY);
        if (correlationId == null) {
            correlationId = "Unknown";
        }

        log.info("Correlation Id: {}", correlationId);

        return next.startCall(call, headers);
    }
}