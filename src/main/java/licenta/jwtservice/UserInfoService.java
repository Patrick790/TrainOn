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
        repository.save(user);
        return "User added successfully";
    }
}