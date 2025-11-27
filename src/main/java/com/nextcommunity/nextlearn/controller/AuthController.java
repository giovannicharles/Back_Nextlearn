package com.nextcommunity.nextlearn.controller;

import com.nextcommunity.nextlearn.dto.*;
import com.nextcommunity.nextlearn.entity.User;
import com.nextcommunity.nextlearn.repository.UserRepository;
import com.nextcommunity.nextlearn.services.jwt.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("nextlearn/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authManager;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;


    // Création d'un compte admin s'il n'existe pas (à appeler manuellement une fois)
    @PostMapping("/")
    public void createAdminAccount() {
        Optional<User> adminAccount = userRepository.findByUserrole("ADMIN");
        System.out.println("test");
        if (adminAccount.isEmpty()) {
            User newAdminAccount = new User();
            newAdminAccount.setNom("Giovanni");
            newAdminAccount.setPrenom("Charles");
            newAdminAccount.setClasse("Licence 3");
            newAdminAccount.setEmail("ebodegiovanni@gmail.com");
            newAdminAccount.setPassword(encoder.encode("nextlearngiovanni"));
            newAdminAccount.setUserrole("ADMIN");
            userRepo.save(newAdminAccount);
            System.out.println("Admin account created successfully");
        }
    }

    // Connexion - Retourne access et refresh tokens
    @PostMapping(value = "/login", produces = "application/json")
    public ResponseEntity<?> login(@RequestBody AuthRequest req) {
        try {
            // Authentification des identifiants
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
            );

            // Récupération utilisateur
            var user = userRepo.findFirstByEmail(req.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));

            // Génération tokens
            String accessToken = jwtService.generateAccessToken(
                    new org.springframework.security.core.userdetails.User(
                            user.getEmail(),
                            user.getPassword(),
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getUserrole()))
                    ));

            String refreshToken = jwtService.generateRefreshToken(
                    new org.springframework.security.core.userdetails.User(
                            user.getEmail(),
                            user.getPassword(),
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getUserrole()))
                    ));

            //Utilisateur connecté
            UserDto userDto = new UserDto();
            userDto.setId(user.getId_user());
            userDto.setNom(user.getNom());
            userDto.setPrenom(user.getPrenom());
            userDto.setClasse(user.getClasse());
            userDto.setEmail(user.getEmail());

            // Corps de la réponse
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("accessToken", accessToken);
            responseBody.put("refreshToken", refreshToken);
            responseBody.put("role", user.getUserrole());
            responseBody.put("user", userDto);

            return ResponseEntity.ok(responseBody);

        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Identifiants incorrects"));
        }
    }
    // Endpoint pour rafraîchir l'access token en utilisant le refresh token
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(@RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        try {
            String username = jwtService.extractUsername(requestRefreshToken);
            var userDetails = userRepo.findFirstByEmail(username)
                    .map(user -> new org.springframework.security.core.userdetails.User(
                            user.getEmail(),
                            user.getPassword(),
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getUserrole()))
                    )).orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));

            // Vérifie que le refresh token est valide
            if (jwtService.isTokenValid(requestRefreshToken, userDetails)) {
                // Génère un nouveau access token (et optionnellement un nouveau refresh token)
                String newAccessToken = jwtService.generateAccessToken(userDetails);
                String newRefreshToken = jwtService.generateRefreshToken(userDetails);

                return ResponseEntity.ok(new TokenRefreshResponse(newAccessToken, newRefreshToken));
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Refresh token invalide"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Refresh token expiré ou invalide"));
        }
    }
}
