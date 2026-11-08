package kh.edu.istad.iam.web;

import gg.jte.springframework.boot.autoconfigure.JteAutoConfiguration;
import gg.jte.springframework.boot.autoconfigure.ServletJteAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ImportAutoConfiguration({JteAutoConfiguration.class, ServletJteAutoConfiguration.class})
public class JteViewConfiguration {
}
