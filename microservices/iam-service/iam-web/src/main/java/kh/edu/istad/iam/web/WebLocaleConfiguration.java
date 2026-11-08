package kh.edu.istad.iam.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Locale;

@Configuration
public class WebLocaleConfiguration implements WebMvcConfigurer {
    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver resolver = new SessionLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        return resolver;
    }

    @Bean
    public HandlerInterceptor languageSwitcher(LocaleResolver localeResolver) {
        return new HandlerInterceptor() {
            @Override
            public boolean preHandle(jakarta.servlet.http.HttpServletRequest request,
                                     jakarta.servlet.http.HttpServletResponse response,
                                     Object handler) throws Exception {
                String language = request.getParameter("lang");
                if (language == null) return true;
                if (!language.equals("en") && !language.equals("km")) {
                    response.sendError(400);
                    return false;
                }
                Locale locale = Locale.forLanguageTag(language);
                localeResolver.setLocale(request, response, locale);
                org.springframework.context.i18n.LocaleContextHolder.setLocale(locale);
                return true;
            }
        };
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(languageSwitcher(localeResolver()));
    }
}
