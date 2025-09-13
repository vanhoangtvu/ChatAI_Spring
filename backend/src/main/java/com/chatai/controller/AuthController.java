package com.chatai.controller;

import com.chatai.dto.auth.JwtResponse;
import com.chatai.dto.auth.LoginRequest;
import com.chatai.dto.auth.MessageResponse;
import com.chatai.dto.auth.SignupRequest;
import com.chatai.entity.Role;
import com.chatai.entity.User;
import com.chatai.repository.RoleRepository;
import com.chatai.repository.UserRepository;
import com.chatai.security.JwtUtils;
import com.chatai.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;
    
    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);
        
        UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());
        
        // Get user entity for additional info
        User user = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
        
        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                user != null ? user.getFullName() : null,
                roles,
                user != null ? user.getDailyRequestLimit() : 100,
                user != null ? user.getRequestsUsedToday() : 0));
    }
    
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(MessageResponse.error("Username is already taken!"));
        }
        
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(MessageResponse.error("Email is already in use!"));
        }
        
        // Create new user's account
        User user = User.builder()
                .username(signUpRequest.getUsername())
                .email(signUpRequest.getEmail())
                .fullName(signUpRequest.getFullName())
                .password(encoder.encode(signUpRequest.getPassword()))
                .isActive(true)
                .requestsUsedToday(0)
                .dailyRequestLimit(100)
                .lastRequestReset(LocalDateTime.now())
                .build();
        
        Set<Role> roles = new HashSet<>();
        
        // All users get USER role by default
        Role userRole = roleRepository.findByName(Role.RoleName.USER)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        roles.add(userRole);
        
        user.setRoles(roles);
        userRepository.save(user);
        
        log.info("User registered successfully: {}", user.getUsername());
        
        return ResponseEntity.ok(MessageResponse.success("User registered successfully!"));
    }
    
    @PostMapping("/signout")
    public ResponseEntity<?> logoutUser() {
        return ResponseEntity.ok(MessageResponse.success("You've been signed out!"));
    }
    
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken() {
        // If we reach this point, the JWT filter has already validated the token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return ResponseEntity.ok(MessageResponse.success("Token is valid"));
        }
        return ResponseEntity.status(401).body(MessageResponse.error("Token is invalid"));
    }
}
