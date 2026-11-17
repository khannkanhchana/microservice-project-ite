package kh.edu.istad.iam.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the iam-web React SPA. Its build (index.html + assets/) lives in classpath:/static, and every
 * client-side route forwards to that single index.html so a reload or deep link (e.g. /me/sessions) works.
 * Sign-in itself stays on the JTE page /login.
 */
@Controller
public class SpaController {

    @GetMapping({
            "/",
            "/callback",
            "/forgot-password",
            "/reset-password",
            "/me",
            "/me/**",
            "/admin",
            "/admin/**"
    })
    public String spa() {
        return "forward:/index.html";
    }
}
