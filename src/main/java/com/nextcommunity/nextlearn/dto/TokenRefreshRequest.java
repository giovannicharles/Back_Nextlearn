package com.nextcommunity.nextlearn.dto;

// Requête envoyée pour demander un nouveau access token via refresh token
public class TokenRefreshRequest {
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}

