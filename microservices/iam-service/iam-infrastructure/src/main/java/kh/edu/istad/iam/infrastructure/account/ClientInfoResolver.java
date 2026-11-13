package kh.edu.istad.iam.infrastructure.account;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts IP address, browser, operating system, device type and an approximate location from a request.
 * Location lookup against a public GeoIP service is opt-in ({@code iam.account.geoip.enabled=true});
 * otherwise private addresses resolve to "Local network" and public ones to "Unknown".
 */
@Component
public class ClientInfoResolver {
    private static final Logger log = LoggerFactory.getLogger(ClientInfoResolver.class);
    private static final Pattern EDGE = Pattern.compile("Edg(?:e|A|iOS)?/([\\d.]+)");
    private static final Pattern OPERA = Pattern.compile("(?:OPR|Opera)/([\\d.]+)");
    private static final Pattern SAMSUNG = Pattern.compile("SamsungBrowser/([\\d.]+)");
    private static final Pattern CHROME = Pattern.compile("(?:Chrome|CriOS)/([\\d.]+)");
    private static final Pattern FIREFOX = Pattern.compile("(?:Firefox|FxiOS)/([\\d.]+)");
    private static final Pattern SAFARI = Pattern.compile("Version/([\\d.]+).*Safari/");
    private static final Pattern ANDROID = Pattern.compile("Android ([\\d.]+)");
    private static final Pattern IOS = Pattern.compile("(?:iPhone|CPU) OS ([\\d_]+)");
    private static final Pattern MAC = Pattern.compile("Mac OS X ([\\d_]+)");
    private static final Pattern JSON_STRING = Pattern.compile("\"(city|country)\"\\s*:\\s*\"([^\"]*)\"");

    private final boolean geoIpEnabled;
    private final Map<String, String> locationCache = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

    public ClientInfoResolver(@Value("${iam.account.geoip.enabled:false}") boolean geoIpEnabled) {
        this.geoIpEnabled = geoIpEnabled;
    }

    public ClientInfo current() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return resolve(attributes.getRequest());
        }
        return ClientInfo.UNKNOWN;
    }

    public ClientInfo resolve(HttpServletRequest request) {
        String ip = clientIp(request);
        String userAgent = truncate(request.getHeader("User-Agent"), 512);
        String ua = userAgent == null ? "" : userAgent;
        return new ClientInfo(ip, userAgent, browser(ua), os(ua), deviceType(ua), location(ip));
    }

    static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return truncate(forwarded.split(",")[0].trim(), 64);
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return truncate(realIp.trim(), 64);
        return truncate(request.getRemoteAddr(), 64);
    }

    static String browser(String ua) {
        if (ua.isBlank()) return "Unknown";
        Matcher m;
        if ((m = EDGE.matcher(ua)).find()) return "Edge " + major(m.group(1));
        if ((m = OPERA.matcher(ua)).find()) return "Opera " + major(m.group(1));
        if ((m = SAMSUNG.matcher(ua)).find()) return "Samsung Internet " + major(m.group(1));
        if ((m = FIREFOX.matcher(ua)).find()) return "Firefox " + major(m.group(1));
        if ((m = CHROME.matcher(ua)).find()) return "Chrome " + major(m.group(1));
        if ((m = SAFARI.matcher(ua)).find()) return "Safari " + major(m.group(1));
        if (ua.contains("PostmanRuntime")) return "Postman";
        if (ua.startsWith("curl/")) return "curl";
        return "Other";
    }

    static String os(String ua) {
        if (ua.isBlank()) return "Unknown";
        Matcher m;
        if (ua.contains("Windows NT 10.0")) return "Windows 10/11";
        if (ua.contains("Windows")) return "Windows";
        if ((m = ANDROID.matcher(ua)).find()) return "Android " + major(m.group(1));
        if ((m = IOS.matcher(ua)).find()) {
            return (ua.contains("iPad") ? "iPadOS " : "iOS ") + major(m.group(1).replace('_', '.'));
        }
        if ((m = MAC.matcher(ua)).find()) return "macOS " + m.group(1).replace('_', '.');
        if (ua.contains("CrOS")) return "ChromeOS";
        if (ua.contains("Linux")) return "Linux";
        return "Other";
    }

    static String deviceType(String ua) {
        if (ua.isBlank()) return "unknown";
        if (ua.contains("iPad") || ua.contains("Tablet") || (ua.contains("Android") && !ua.contains("Mobile"))) {
            return "tablet";
        }
        if (ua.contains("Mobi") || ua.contains("iPhone")) return "mobile";
        if (ua.contains("PostmanRuntime") || ua.startsWith("curl/")) return "api";
        return "desktop";
    }

    String location(String ip) {
        if (ip == null || ip.isBlank()) return "Unknown";
        if (isPrivate(ip)) return "Local network";
        if (!geoIpEnabled) return "Unknown";
        return locationCache.computeIfAbsent(ip, this::lookup);
    }

    private String lookup(String ip) {
        try {
            HttpRequest request = HttpRequest.newBuilder(
                            URI.create("http://ip-api.com/json/" + ip + "?fields=status,country,city"))
                    .timeout(Duration.ofSeconds(2)).GET().build();
            String body = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
            if (!body.contains("\"success\"")) return "Unknown";
            String city = null;
            String country = null;
            Matcher m = JSON_STRING.matcher(body);
            while (m.find()) {
                if (m.group(2).isBlank()) continue;
                if ("city".equals(m.group(1))) city = m.group(2); else country = m.group(2);
            }
            if (city == null && country == null) return "Unknown";
            return city == null ? country : country == null ? city : city + ", " + country;
        } catch (Exception exception) {
            log.debug("GeoIP lookup failed for {}", ip, exception);
            return "Unknown";
        }
    }

    static boolean isPrivate(String ip) {
        try {
            if (!ip.matches("[0-9a-fA-F:.]+")) return false;
            String lower = ip.toLowerCase(Locale.ROOT);
            InetAddress address = InetAddress.getByName(ip);
            return address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()
                    || address.isAnyLocalAddress() || lower.startsWith("fc") || lower.startsWith("fd");
        } catch (Exception exception) {
            return false;
        }
    }

    private static String major(String version) {
        int dot = version.indexOf('.');
        return dot < 0 ? version : version.substring(0, dot);
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
