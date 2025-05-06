package licenta.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import licenta.jwtservice.JwtService;
import licenta.jwtservice.UserInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);
    private final JwtService jwtService;
    private final UserInfoService userDetailsService;

    @Autowired
    public JwtAuthFilter(JwtService jwtService, @Lazy UserInfoService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Permitem cererile către endpoint-urile publice să treacă fără verificare JWT
        return path.startsWith("/login") ||
                path.startsWith("/register") ||
                "OPTIONS".equals(method) || // Add support for OPTIONS requests
                (path.startsWith("/sportsHalls") && "GET".equals(method));

        // Nu am adăugat excepții pentru /reservationProfiles deoarece
        // toate operațiile pe profiluri de rezervare necesită autentificare
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Log pentru debugging
        logger.info("Processing request: {} {}", request.getMethod(), request.getRequestURI());

        String authHeader = request.getHeader("Authorization");
        logger.info("Authorization header: {}", authHeader != null ? "present" : "missing");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);
            String username = jwtService.extractUsername(token);
            logger.info("Extracted username: {}", username);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Log pentru debugging
                logger.info("User authorities: {}", userDetails.getAuthorities());

                if (jwtService.validateToken(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.info("Successfully authenticated user: {}", username);
                } else {
                    logger.warn("Token validation failed for user: {}", username);
                }
            }
        } catch (Exception e) {
            logger.error("Error processing JWT token: " + e.getMessage(), e);
            // Nu blocăm cererea în caz de eroare, dar nu setăm autentificarea
        }

        filterChain.doFilter(request, response);
    }
}