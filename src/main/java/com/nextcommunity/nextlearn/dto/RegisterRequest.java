package com.nextcommunity.nextlearn.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String nom;
    private String prenom;
    private String classe;
    private String email;
    private String password;
    private String userrole;
}
