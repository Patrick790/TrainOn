package licenta.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@ConditionalOnProperty(name = "spring.mvc.throw-exception-if-no-handler-found", havingValue = "true")
public class FrontendController {

    /**
     * Servește aplicația React pentru toate rutele care NU sunt API endpoints
     * Acoperă toate rutele React Router + rutele pentru forgot password
     */
    @RequestMapping(value = {
            "/",
            "/login",
            "/register",
            "/user-home",
            "/search",
            "/profile-creation",
            "/profile",
            "/reservations",
            "/fcfs-reservation",
            "/reset-password",
            "/reset-password/**",        // ADĂUGAT pentru reset cu token
            "/forgot-password",          // ADĂUGAT pentru forgot password
            "/payment",
            "/payment-success",
            "/payment-cancel",
            "/privacy",
            "/terms",
            "/hall-admin-dashboard",
            "/hall-admin-dashboard/**",
            "/admin-dashboard",
            "/admin-dashboard/**",
            "/edit-hall/**",
            "/add-hall",                 // Pentru adăugare sală nouă
            "/admin-profile",
            "/app-admin-profile",
            "/sportsHalls/*/reviews",
            "/hall/**",
            "/booking/**",
            "/verify/**"
    })
    public String forwardToReact() {
        return "forward:/index.html";
    }
}