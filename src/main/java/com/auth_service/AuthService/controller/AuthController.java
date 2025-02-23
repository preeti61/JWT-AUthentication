package com.auth_service.AuthService.controller;

import com.auth_service.AuthService.DTO.AuthRequest;
import com.auth_service.AuthService.DTO.AuthResponse;
import com.auth_service.AuthService.service.CustomUserDetailsService;
import com.auth_service.AuthService.util.JWTUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    CustomUserDetailsService userDetailsService;
    @Autowired
    private JWTUtil jwtService;

    @Autowired
    PasswordEncoder passwordEncoder;
    @PostMapping("/token")
    public ResponseEntity<AuthResponse> generateToken(@RequestParam String username, @RequestParam String password) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (passwordEncoder.matches(password, userDetails.getPassword())) {
            return ResponseEntity.ok(new AuthResponse(jwtService.generateToken(username)));
        } else {
            throw new RuntimeException("Invalid username or password");
        }
    }
    @GetMapping("/login")
    public ResponseEntity<?> check() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(principal instanceof  UserDetails) {
            return ResponseEntity.ok(((UserDetails) principal).getUsername());
        }  else {
            return ResponseEntity.ok(principal.toString()); // If principal is a string (e.g., JWT token subject)
        }
    }
}
