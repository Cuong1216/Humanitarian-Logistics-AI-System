package com.humanitarian.logistics.core.config;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // TODO: Replace with actual database user lookup. For now, returning dummy UserDetails.
        return new org.springframework.security.core.userdetails.User(
                username, 
                "", 
                Collections.emptyList()
        );
    }
}
