package kh.edu.istad.platform.order.exception;

public class GlobalExceptionHandler extends RuntimeException {

    private String service;


    public GlobalExceptionHandler(String message) {
        super(message);
    }

}




