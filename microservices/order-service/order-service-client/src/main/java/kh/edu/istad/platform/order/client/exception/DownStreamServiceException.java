package kh.edu.istad.platform.order.client.exception;

public class DownStreamServiceException extends RuntimeException {

    private final String service;

    public String getService() {
        return service;
    }


    public DownStreamServiceException(String message, String exMessage) {
        super(message);
        this.service = "unknown";
    }

    public DownStreamServiceException(String service, String message, Throwable cause) {
        super(message, cause);
        this.service = service;
    }

}
