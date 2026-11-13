package kh.edu.istad.iam.infrastructure.social;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * Decompresses gzip response bodies regardless of headers. oauth.telegram.org returns gzip-compressed
 * bodies for roughly half of its requests, even without Accept-Encoding and sometimes without a
 * Content-Encoding header. Spring's default HTTP clients do not decompress, so the JSON parsers fail
 * ("Malformed Jwk set", "Error while extracting response").
 */
final class GzipTolerantInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        ClientHttpResponse response = execution.execute(request, body);
        return new BufferedResponse(response, readBody(response));
    }

    static byte[] readBody(ClientHttpResponse response) throws IOException {
        byte[] bytes;
        try (InputStream in = response.getBody()) {
            bytes = in.readAllBytes();
        }
        return isGzip(bytes) ? gunzip(bytes) : bytes;
    }

    private static boolean isGzip(byte[] bytes) {
        return bytes.length >= 2 && (bytes[0] & 0xff) == 0x1f && (bytes[1] & 0xff) == 0x8b;
    }

    private static byte[] gunzip(byte[] bytes) throws IOException {
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            return in.readAllBytes();
        }
    }

    /** A response whose body has already been read (and possibly decompressed or rewritten). */
    record BufferedResponse(ClientHttpResponse delegate, byte[] body) implements ClientHttpResponse {
        @Override public HttpStatusCode getStatusCode() throws IOException { return delegate.getStatusCode(); }
        @Override public String getStatusText() throws IOException { return delegate.getStatusText(); }
        @Override public InputStream getBody() { return new ByteArrayInputStream(body); }
        @Override public void close() { delegate.close(); }

        @Override
        public HttpHeaders getHeaders() {
            HttpHeaders headers = new HttpHeaders();
            headers.addAll(delegate.getHeaders());
            headers.remove(HttpHeaders.CONTENT_ENCODING);
            headers.setContentLength(body.length);
            return headers;
        }
    }
}
