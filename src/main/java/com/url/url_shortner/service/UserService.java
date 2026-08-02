package com.url.url_shortner.service;

import com.url.url_shortner.dto.LoginRequest;
import com.url.url_shortner.models.User;
import com.url.url_shortner.repository.UserRepository;
import com.url.url_shortner.secuirity.jwt.JwtAuthenticationResponse;
import com.url.url_shortner.secuirity.jwt.JwtUtils;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserService {
    private PasswordEncoder passwordEncoder;
    private UserRepository userRepository;
    private AuthenticationManager authenticationManager;
    private JwtUtils jwtutils;

    public JwtAuthenticationResponse registerUser( User user)
    {
        user.setPassword(passwordEncoder.encode(user.getPassword()) );
        User saved = userRepository.save(user);
        // Issue a token straight from the just-saved user so the client is logged in
        // immediately, avoiding a second (read-after-write) round-trip to log in.
        String jwt = jwtutils.generateToken(UserDetailsImpl.build(saved));
        return new JwtAuthenticationResponse(jwt);
    }
    public JwtAuthenticationResponse authenticateUser(LoginRequest loginRequest)
    {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String jwt= jwtutils.generateToken(userDetails);
        return new JwtAuthenticationResponse(jwt);

    }

    public User findByUserName(String name) {
        return userRepository.findByUserName(name).orElseThrow(()-> new RuntimeException("User not found"));
    }
}
