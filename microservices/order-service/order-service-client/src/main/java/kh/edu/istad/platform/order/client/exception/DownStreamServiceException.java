package kh.edu.istad.platform.order.client.exception;

public class DownStreamServiceException extends RuntimeException {

    private String service;

    public String getService() {
        return service;
    }


    public DownStreamServiceException(String message, String exMessage) {
        super(message);
    }

}
