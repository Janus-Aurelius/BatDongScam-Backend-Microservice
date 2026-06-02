package com.se361.iam_service.security;

import com.se361.iam_service.entity.User;
import com.se361.iam_service.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String emailOrId) throws UsernameNotFoundException {
        User user = tryLoadByEmail(emailOrId);
        if (user == null) user = tryLoadById(emailOrId);
        if (user == null)
            throw new UsernameNotFoundException("User not found: " + emailOrId);
        return JwtUserDetails.create(user);
    }

    private User tryLoadByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    private User tryLoadById(String id) {
        try {
            return userRepository.findById(UUID.fromString(id)).orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
