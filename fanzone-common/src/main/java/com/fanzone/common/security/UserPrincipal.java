package com.fanzone.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class UserPrincipal implements UserDetails {

    private final UUID userId;
    private final String email;
    private final String username;
    private final UUID favoriteClubId;
    private final boolean emailVerified;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(UUID userId, String email, String username, UUID favoriteClubId) {
        this(userId, email, username, favoriteClubId, false, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    public UserPrincipal(UUID userId, String email, String username, UUID favoriteClubId, boolean emailVerified) {
        this(userId, email, username, favoriteClubId, emailVerified, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    public UserPrincipal(UUID userId, String email, String username, UUID favoriteClubId,
                         boolean emailVerified, Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.email = email;
        this.username = username;
        this.favoriteClubId = favoriteClubId;
        this.emailVerified = emailVerified;
        this.authorities = authorities;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public UUID getFavoriteClubId() {
        return favoriteClubId;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    /**
     * Requires that the user's email is verified. Throws ForbiddenException if not.
     * Use this in controllers/services that should be restricted to verified users.
     */
    public void requireEmailVerified() {
        if (!emailVerified) {
            throw new com.fanzone.common.exceptions.ForbiddenException(
                    "AUTH_EMAIL_NOT_VERIFIED",
                    "Email verification is required to perform this action");
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return username;
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
