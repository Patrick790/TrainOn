package licenta.config;

import licenta.filter.JwtAuthFilter;
import licenta.jwtservice.UserInfoService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter authFilter;
    private final UserInfoService userInfoService;

    public SecurityConfig(JwtAuthFilter authFilter, @Lazy UserInfoService userInfoService) {
        this.authFilter = authFilter;
        this.userInfoService = userInfoService;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return userInfoService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/user-home", "/static/**", "/*.js", "/*.css", "/*.ico", "/*.png", "/*.jpg", "/*.gif",
                                "/login", "/register", "/search", "/profile-creation", "/profile", "/reservations",
                                "/fcfs-reservation", "/reset-password", "/payment", "/payment-success", "/payment-cancel",
                                "/privacy", "/terms", "/hall-admin-dashboard/**", "/admin-dashboard/**", "/edit-hall/**",
                                "/admin-profile", "/app-admin-profile", "/sportsHalls/*/reviews").permitAll()
                        .requestMatchers("/login", "/login/**", "/register/**").permitAll()
                        // ADĂUGAT: Endpoint-uri pentru resetarea parolei
                        .requestMatchers("/forgot-password").permitAll()
                        .requestMatchers("/reset-password/**").permitAll()
                        .requestMatchers("/reset-password").permitAll()

                        .requestMatchers(HttpMethod.OPTIONS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/sportsHalls").permitAll()
                        .requestMatchers(HttpMethod.GET, "/sportsHalls/cities").permitAll()
                        .requestMatchers(HttpMethod.GET, "/sportsHalls/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/images/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/feedbacks").permitAll()
                        .requestMatchers(HttpMethod.GET, "/feedbacks/**").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/sportsHalls/**").hasAnyAuthority("hall_admin", "admin")
                        .requestMatchers(HttpMethod.POST, "/sportsHalls", "/sportsHalls/**").hasAnyAuthority("hall_admin", "admin")
                        .requestMatchers(HttpMethod.DELETE, "/sportsHalls/**").hasAnyAuthority("hall_admin", "admin")
                        .requestMatchers(HttpMethod.DELETE, "/images/**").hasAnyAuthority("hall_admin", "admin")

                        // Configurare pentru rezervari
                        .requestMatchers(HttpMethod.GET, "/reservations").permitAll()
                        .requestMatchers(HttpMethod.GET, "/reservations/maintenance/**").permitAll()
                        .requestMatchers("/reservations/**").hasAnyAuthority("user", "admin", "hall_admin")

                        // Configurare pentru hall-schedules - programul salilor
                        .requestMatchers(HttpMethod.GET, "/schedules/hall/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/schedules/**").hasAnyAuthority("hall_admin", "admin")
                        .requestMatchers(HttpMethod.PUT, "/schedules/**").hasAnyAuthority("hall_admin", "admin")
                        .requestMatchers(HttpMethod.DELETE, "/schedules/**").hasAnyAuthority("hall_admin", "admin")

                        // Adaugare pentru profilurile de rezervare - doar utilizatorii autentificati pot accesa
                        .requestMatchers("/reservationProfiles/**").hasAnyAuthority("user", "admin", "hall_admin")

                        // Configurare pentru gestionarea cardurilor
                        .requestMatchers("/card-payment-methods/**").hasAnyAuthority("user", "admin", "hall_admin")

                        // Adaugare pentru gestionarea inregistrarilor de echipe
                        .requestMatchers("/admin/team-registrations/**").hasAuthority("admin")
                        .requestMatchers("/users").hasAnyAuthority("user", "admin", "hall_admin")
                        .requestMatchers("/users/**").hasAnyAuthority("user", "admin", "hall_admin")

                        .requestMatchers(HttpMethod.POST, "/payment/stripe/webhook").permitAll()
                        .requestMatchers(HttpMethod.GET, "/payment/bt/status/**").permitAll()
                        .requestMatchers("/payment/**").hasAnyAuthority("user", "admin", "hall_admin")

                        // Endpoint-uri pentru generarea automata de rezervari
                        .requestMatchers("/booking-prioritization/generate").permitAll()
                        .requestMatchers("/booking-prioritization/status").permitAll()

                        .requestMatchers("/booking-prioritization/diagnostic").hasAuthority("admin")
                        .requestMatchers("/booking-prioritization/test-generation").hasAuthority("admin")
                        .requestMatchers("/booking-prioritization/**").hasAuthority("admin")

                        .requestMatchers("/admin/logs/**").hasAuthority("admin")

                        // Endpoint pentru scheduler (doar pentru admini)
                        .requestMatchers("/admin/scheduler/**").hasAuthority("admin")

                        // Endpoint pentru FCFS status
                        .requestMatchers("/fcfs/**").permitAll()

                        // Endpoint pentru health check
                        .requestMatchers("/health", "/", "/api", "/ping").permitAll()

                        .anyRequest().authenticated()
                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider());

        http.addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService());
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "https://trainonapp.onrender.com"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public StandardServletMultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }
}