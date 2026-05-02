package com.se100.bds.securities;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se100.bds.dtos.responses.error.ErrorResponse;
import com.se100.bds.exceptions.AppExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    @Override
    public final void commence(final HttpServletRequest request,
                               final HttpServletResponse response,
                               final AuthenticationException e
    ) throws IOException {
        final String expired = (String) request.getAttribute("expired");
        final String unsupported = (String) request.getAttribute("unsupported");
        final String invalid = (String) request.getAttribute("invalid");
        final String illegal = (String) request.getAttribute("illegal");
        final String notfound = (String) request.getAttribute("notfound");

        final String message;

        if (expired != null) {
            message = "Token is expired";
        } else if (unsupported != null) {
            message = "Unsupported token, must be JWT";
        } else if (invalid != null) {
            message = "Invalid token";
        } else if (illegal != null) {
            message = "Illegal token, must be Bearer token";
        } else if (notfound != null) {
            message = "Token not found";
        } else {
            message = "Unauthorized";
        }

        log.error("Could not set user authentication in security context. Error: {}", message);
        ResponseEntity<ErrorResponse> responseEntity = new AppExceptionHandler()
                .handleBadCredentialsException(new BadCredentialsException(message));

        ErrorResponse errorResponse = responseEntity.getBody();
        if (errorResponse != null) {
            errorResponse.setStatusCode(HttpServletResponse.SC_UNAUTHORIZED);
        }
        response.getWriter().write(objectMapper.writeValueAsString(responseEntity.getBody()));
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    }
}