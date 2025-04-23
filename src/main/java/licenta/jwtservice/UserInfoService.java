package licenta.jwtservice;

import licenta.model.User;
import licenta.persistence.IUserSpringRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Date;
import java.util.Optional;

@Service
public class UserInfoService implements UserDetailsService {
    @Autowired
    private IUserSpringRepository repository;

    private final PasswordEncoder encoder;

    @Autowired
    public UserInfoService(@Lazy PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> userDetail = repository.findByEmail(username);
        return userDetail.map(UserInfoDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    public String addUser(User user) {
        user.setPassword(encoder.encode(user.getPassword()));
        user.setUserType("user");

        // Asigurăm că data creării este setată
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(new Date());
        }

        // Set account status based on whether it's a team registration
        if (user.getTeamType() != null && !user.getTeamType().isEmpty()) {
            // If it's a team registration, set status to "pending"
            user.setAccountStatus("pending");
        } else {
            // For regular users, set status to "verified"
            user.setAccountStatus("verified");
        }

        repository.save(user);

        if ("pending".equals(user.getAccountStatus())) {
            return "Team registration submitted successfully. Your account is pending approval.";
        } else {
            return "User added successfully";
        }
    }
}