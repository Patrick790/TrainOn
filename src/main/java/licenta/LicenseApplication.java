package licenta;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling // Adăugat pentru a asigura că @Scheduled funcționează
public class LicenseApplication {

    /**
     * Setează fusul orar default pentru întreaga aplicație Java (JVM).
     * Acest lucru asigură că operațiuni precum Calendar.getInstance()
     * funcționează la fel pe orice server, indiferent de locația lui.
     */
    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Bucharest"));
    }

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
                            "https://trainonapp.onrender.com"  // Noul URL frontend
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