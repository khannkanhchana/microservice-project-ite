package kh.edu.istad.iam.restapi;

public class UseCaseUnavailableException extends RuntimeException {
    public UseCaseUnavailableException(String useCaseName) {
        super("No application service is currently registered for " + useCaseName + ".");
    }
}
