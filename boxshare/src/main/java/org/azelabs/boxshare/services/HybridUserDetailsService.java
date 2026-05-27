package org.azelabs.boxshare.services;

import lombok.RequiredArgsConstructor;
import org.azelabs.boxshare.models.HybridUser;
import org.azelabs.boxshare.repositories.IUserRepository;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@NullMarked
@Service
@RequiredArgsConstructor
public class HybridUserDetailsService implements UserDetailsService {

    private final IUserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            UUID userId = UUID.fromString(username);
            return userRepository.findByIdentity(userId)
                    .map(HybridUser::new)
                    .orElseThrow(() -> new UsernameNotFoundException("Could not find user with id: " + userId));
        } catch (IllegalArgumentException ex) {
            // not a UUID — treat as email
            return userRepository.findByEmail(username)
                    .map(HybridUser::new)
                    .orElseThrow(() -> new UsernameNotFoundException("Could not find user with email: " + username));
        }
    }
}
