package kh.edu.istad.iam.infrastructure.account;

/** Who is on the other end of an HTTP request: network address, parsed user agent and coarse location. */
public record ClientInfo(String ipAddress, String userAgent, String browser, String os, String deviceType,
                         String location) {
    public static final ClientInfo UNKNOWN = new ClientInfo(null, null, "Unknown", "Unknown", "unknown", "Unknown");
}
