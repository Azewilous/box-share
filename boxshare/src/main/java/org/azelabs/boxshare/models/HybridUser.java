package org.azelabs.boxshare.models;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@NullMarked
@RequiredArgsConstructor
public class HybridUser implements UserDetails {
    private final UserModel user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (user.getRole() == null || user.getRole().getType() == null) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().getType().name()));
    }

    @Override
    public @Nullable String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        UUID identity = user.getIdentity();
        if (identity == null) throw new IllegalStateException("UserModel has no identity UUID assigned");
        return identity.toString();
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }


}
