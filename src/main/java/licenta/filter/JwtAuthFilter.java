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


        return path.startsWith("/login") ||
                path.startsWith("/register") ||
                // ADĂUGAT: Excludem endpoint-urile pentru resetarea parolei
                path.equals("/forgot-password") ||
                path.startsWith("/reset-password") ||

                "OPTIONS".equals(method) ||
                // Doar GET requests pentru sportsHalls sunt excluse de la filtrare
                (path.startsWith("/sportsHalls") && "GET".equals(method)) ||
                path.equals("/sportsHalls/cities") ||
                (path.startsWith("/schedules/hall/") && "GET".equals(method)) ||
                // ACTUALIZAT - Permite doar anumite endpoint-uri de rezervări fără autentificare
                (path.equals("/reservations") && "GET".equals(method)) || // Doar listarea generală
                (path.startsWith("/reservations/maintenance") && "GET".equals(method)) || // Mentenanța
                (path.startsWith("/images") && "GET".equals(method)) ||
                path.startsWith("/payment/stripe/webhook") ||
                (path.startsWith("/feedbacks") && "GET".equals(method));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Log pentru debugging
        logger.info("Processing request: {} {}", request.getMethod(), request.getRequestURI());

        String authHeader = request.getHeader("Authorization");
        logger.info("Authorization header: {}", authHeader != null ? "present" : "missing");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("No valid Authorization header found");
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
                logger.info("Request path: {}, method: {}", request.getRequestURI(), request.getMethod());

                if (jwtService.validateToken(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.info("Successfully authenticated user: {} with authorities: {}", username, userDetails.getAuthorities());
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