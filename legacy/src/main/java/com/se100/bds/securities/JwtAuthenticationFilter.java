package com.se100.bds.securities;

import com.se100.bds.services.domains.user.UserService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Profile("!mvcIT")
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    @Override
    protected final void doFilterInternal(@NonNull final HttpServletRequest request,
                                          @NonNull final HttpServletResponse response,
                                          @NonNull final FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String token = jwtTokenProvider.extractJwtFromRequest(request);

            if (StringUtils.hasText(token)) {
                // Try to validate the token
                try {
                    //! DEBUG: Mock tokens for testing purpose
                    Map<String, String> mockTokenToEmail = Map.of(
                            "admin", "admin@example.com",
                            "agent1", "agent1@example.com",
                            "customer1", "customer1@example.com"
                    );

                    if (mockTokenToEmail.containsKey(token)) {
                        String email = mockTokenToEmail.get(token);
                        UserDetails user = userService.loadUserByEmail(email);

                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        filterChain.doFilter(request, response);
                        return;
                    }

                    if (jwtTokenProvider.validateToken(token, request)) {
                        String id = jwtTokenProvider.getUserIdFromToken(token);
                        UserDetails user = userService.loadUserById(id);

                        if (Objects.nonNull(user)) {
                            UsernamePasswordAuthenticationToken auth =
                                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        }
                    }
                } catch (ExpiredJwtException e) {
                    log.warn("Access token expired for request: {}", request.getRequestURI());
                    // Mark the request as having an expired token
                    request.setAttribute("expired", "Access token expired. Please use refresh token.");
                }
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }
}
