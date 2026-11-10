package sn.senetech.itsn.auth;

import sn.senetech.itsn.domain.Role;
import sn.senetech.itsn.domain.User;

public record UserProfile(Long id, String fullName, String email, Role role, Long siteId, String siteName) {
    public static UserProfile from(User u) {
        return new UserProfile(u.getId(), u.getFullName(), u.getEmail(), u.getRole(),
            u.getSite() == null ? null : u.getSite().getId(),
            u.getSite() == null ? null : u.getSite().getName());
    }
}
