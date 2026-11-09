package kh.edu.istad.iam.web;


import gg.jte.springframework.boot.autoconfigure.JteViewResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class JteViewResolverConfiguration {

    @Autowired
    public void configureJteViewResolver(JteViewResolver jteViewResolver) {
        jteViewResolver.setOrder(Ordered.HIGHEST_PRECEDENCE);
    }
}