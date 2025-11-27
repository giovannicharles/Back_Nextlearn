package com.nextcommunity.nextlearn.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Data
@Table(name = "Users")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id_user;
    private String nom;
    private String prenom;
    private String classe;
    private String email;
    private String password;
    private String userrole;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Utiliser getUserrole() pour éviter les erreurs
        String role = getUserrole();
        return role != null
                ? List.of(new SimpleGrantedAuthority(role))
                : List.of(); // ou tu peux throw une exception si tu veux forcer l'existence d’un rôle
    }

    @Override
    public String getUsername() {
        return email;
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
