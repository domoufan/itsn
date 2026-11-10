package sn.senetech.itsn.auth;

public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds, UserProfile user) {
}
