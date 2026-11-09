package kh.edu.istad.iam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
public class IamServiceApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context =
                SpringApplication.run(IamServiceApplication.class, args);

        Environment env = context.getEnvironment();

        String location = env.getProperty("gg.jte.template-location");
        String suffix = env.getProperty("gg.jte.template-suffix");

        System.out.println("\n========== JTE PATH DEBUG ==========");
        System.out.println("Working directory: " + System.getProperty("user.dir"));
        System.out.println("Template location: " + location);
        System.out.println("Template suffix: " + suffix);

        if (location != null && suffix != null) {
            Path templatePath = Path.of(
                    location,
                    "login" + suffix
            ).toAbsolutePath();

            System.out.println("Resolved path: " + templatePath);
            System.out.println("File exists: " + Files.exists(templatePath));
        }

        System.out.println("====================================\n");
    }
}