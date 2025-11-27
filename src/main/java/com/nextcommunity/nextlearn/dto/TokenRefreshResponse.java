package com.nextcommunity.nextlearn.dto;

// Réponse retournée contenant access et refresh tokens
public class TokenRefreshResponse {
    private String accessToken;
    private String refreshToken;

    public TokenRefreshResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
    public String getAccessToken() {
        return accessToken;
    }
    public String getRefreshToken() {
        return refreshToken;
    }
}
