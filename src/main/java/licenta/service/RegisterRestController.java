package licenta.service;

import licenta.jwtservice.UserInfoService;
import licenta.model.User;
import licenta.persistence.IUserSpringRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/register")
public class RegisterRestController {
    @Autowired
    private UserInfoService service;

    private final IUserSpringRepository userSpringRepository;

    @Autowired
    public RegisterRestController(IUserSpringRepository userSpringRepository) {
        this.userSpringRepository = userSpringRepository;
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @PostMapping("/test")
    public String addNewUser(@RequestBody User user) {
        return service.addUser(user);
    }
}
