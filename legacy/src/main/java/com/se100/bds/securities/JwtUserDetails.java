package com.se100.bds.securities;

import com.se100.bds.models.entities.user.User;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Data
public final class JwtUserDetails implements UserDetails {
    private String id;

    private String email;

    private String username;

    private String password;

    private Collection<? extends GrantedAuthority> authorities;

    private JwtUserDetails(final String id, final String email, final String password,
                           final Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.username = email;
        this.password = password;
        this.authorities = authorities;
    }

    public static JwtUserDetails create(final User user) {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));

        return new JwtUserDetails(user.getId().toString(), user.getEmail(), user.getPassword(), authorities);
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
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
