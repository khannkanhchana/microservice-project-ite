package kh.edu.istad.platform.product.exception;


import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;

import java.util.NoSuchElementException;

@GrpcAdvice
public class GrpcExceptionAdvice {
    @GrpcExceptionHandler(IllegalArgumentException.class)
    public StatusRuntimeException handleInvalidArgument(IllegalArgumentException ex) {
        return Status.INVALID_ARGUMENT
                .withDescription(ex.getMessage())
                .asRuntimeException();
    }
    @GrpcExceptionHandler(NoSuchElementException.class)
    public StatusRuntimeException handleNotFound(NoSuchElementException ex) {
        return Status.NOT_FOUND
                .withDescription(ex.getMessage())
                .asRuntimeException();
    }
    @GrpcExceptionHandler(Exception.class)
    public StatusRuntimeException handleGeneralException(Exception ex) {
        return Status.INTERNAL
                .withDescription("Internal server error: " + ex.getMessage())
                .asRuntimeException();
    }
}