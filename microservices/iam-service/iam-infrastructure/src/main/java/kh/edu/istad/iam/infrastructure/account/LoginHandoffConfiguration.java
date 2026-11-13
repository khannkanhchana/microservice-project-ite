package kh.edu.istad.iam.infrastructure.account;

import kh.edu.istad.iam.application.account.LoginHandoffStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoginHandoffConfiguration {

    /** Carries social logins between localhost and the public (ngrok) host used for HTTPS-only callbacks. */
    @Bean
    public LoginHandoffStore loginHandoffStore() {
        return new LoginHandoffStore();
    }
}
