package kh.edu.istad.iam.infrastructure.social;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import kh.edu.istad.iam.infrastructure.social.GzipTolerantInterceptor.BufferedResponse;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Authorization-code token client for social login that tolerates providers which bend RFC 6749:
 * Telegram (and GitHub) report token errors as HTTP 200 with an {@code error} field, and some
 * providers omit {@code token_type}. Spring's default client fails on both with an opaque
 * "Error while extracting response" message.
 */
public final class SocialTokenResponseClient {
    private SocialTokenResponseClient() { }

    public static OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> create() {
        RestClient restClient = RestClient.builder()
                .configureMessageConverters(converters -> converters
                        .addCustomConverter(new FormHttpMessageConverter())
                        .addCustomConverter(new OAuth2AccessTokenResponseHttpMessageConverter()))
                .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler())
                .requestInterceptor(new TokenResponseNormalizer())
                .build();
        RestClientAuthorizationCodeTokenResponseClient client = new RestClientAuthorizationCodeTokenResponseClient();
        client.setRestClient(restClient);
        return client;
    }

    static final class TokenResponseNormalizer implements ClientHttpRequestInterceptor {
        private static final JsonMapper JSON = JsonMapper.builder().build();
        private static final TypeReference<LinkedHashMap<String, Object>> MAP = new TypeReference<>() { };

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
                throws IOException {
            ClientHttpResponse response = execution.execute(request, body);
            byte[] bytes = GzipTolerantInterceptor.readBody(response);
            if (!response.getStatusCode().is2xxSuccessful()) return new BufferedResponse(response, bytes);

            Map<String, Object> json;
            try {
                json = JSON.readValue(bytes, MAP);
            } catch (JacksonException notJson) {
                return new BufferedResponse(response, bytes);
            }

            Object error = json.get("error");
            if (error != null) {
                response.close();
                Object description = json.get("error_description");
                throw new OAuth2AuthorizationException(new OAuth2Error(error.toString(),
                        "The provider rejected the sign-in (" + error
                                + (description == null ? "" : ": " + description) + "). Please try again.", null));
            }
            if (json.containsKey("access_token") && json.get("token_type") == null) {
                json.put("token_type", "Bearer");
                bytes = JSON.writeValueAsBytes(json);
            }
            return new BufferedResponse(response, bytes);
        }
    }
}
