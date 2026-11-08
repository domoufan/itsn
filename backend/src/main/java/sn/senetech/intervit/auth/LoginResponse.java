package sn.senetech.intervit.auth;

public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds, UserProfile user) {
}
