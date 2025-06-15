package licenta.jwtservice;

import licenta.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class UserInfoDetails implements UserDetails {
    private String username;
    private String password;
    private List<GrantedAuthority> authorities;
    private boolean accountNonLocked = true;

    public UserInfoDetails(User user) {
        this.username = user.getEmail();
        this.password = user.getPassword();

        List<String> authList = new ArrayList<>();
        if (user.getUserType() != null) {
            if (user.getUserType().contains(",")) {
                String[] types = user.getUserType().split(",");
                for (String type : types) {
                    authList.add(type.trim());
                }
            } else {
                authList.add(user.getUserType().trim());
            }
        }

        if (user.getAccountStatus() != null && user.getAccountStatus().equals("rejected")) {
            this.accountNonLocked = false;
        }

        this.authorities = authList.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}