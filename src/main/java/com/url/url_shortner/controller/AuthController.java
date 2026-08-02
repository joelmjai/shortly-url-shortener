package com.url.url_shortner.controller;

import com.url.url_shortner.dto.LoginRequest;
import com.url.url_shortner.dto.RegisterRequest;
import com.url.url_shortner.models.User;
import com.url.url_shortner.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {
    private UserService userService;

    //Long url --> short url

    @PostMapping("/public/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest longinRequest){
        return ResponseEntity.ok(userService.authenticateUser(longinRequest));

    }
    @PostMapping("/public/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest registerRequest){
        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setPassword(registerRequest.getPassword());
        user.setUserName(registerRequest.getUsername());
        user.setRole("ROLE_USER");
        // Returns a JWT so the client is authenticated right away (no separate login call needed).
        return ResponseEntity.ok(userService.registerUser(user));
    }

}
