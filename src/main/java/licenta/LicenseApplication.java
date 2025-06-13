package licenta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class LicenseApplication {

    public static void main(String[] args) {
        SpringApplication.run(LicenseApplication.class, args);
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // Obține allowed origins din environment variable sau folosește default
                String allowedOriginsEnv = System.getenv("CORS_ORIGINS");
                String[] allowedOrigins;

                if (allowedOriginsEnv != null && !allowedOriginsEnv.isEmpty()) {
                    // Production: folosește CORS_ORIGINS din environment
                    allowedOrigins = allowedOriginsEnv.split(",");
                } else {
                    // Development: folosește valorile default
                    allowedOrigins = new String[]{
                            "http://localhost:3000",
                            "https://trainon.onrender.com"
                    };
                }

                registry.addMapping("/**")
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}