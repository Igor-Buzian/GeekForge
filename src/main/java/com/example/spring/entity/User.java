package com.example.spring.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.validation.constraints.Email;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a user entity in the application, implementing Spring Security's UserDetails interface.
 * This class maps to the "users" table in the database and stores user-related information,
 * including authentication details, roles, and account status.
 */
@Data
@Entity
@Table(name = "users")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    private boolean enabled = true;

    @Column(name = "created_at", updatable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    public LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "failed_attempts", nullable = false, columnDefinition = "integer default 0")
    private int failedAttempts;

    @Column(name = "lock_time", columnDefinition = "timestamp")
    private LocalDateTime lockTime;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    /**
     * Returns the authorities granted to the user. In this case, it returns the roles associated with the user.
     *
     * @return A collection of GrantedAuthority objects representing the user's roles.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles;
    }

    /**
     * Indicates whether the user's account has expired.
     * An expired account cannot be authenticated.
     *
     * @return true if the user's account is valid (i.e., not expired), false otherwise.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true; // Account expiration is not currently implemented.
    }

    /**
     * Indicates whether the user's credentials (password) has expired.
     * Expired credentials prevent authentication.
     *
     * @return true if the user's credentials are valid (i.e., not expired), false otherwise.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Credential expiration is not currently implemented.
    }

    /**
     * Indicates whether the user is enabled or disabled.
     * A disabled user cannot be authenticated.
     *
     * @return true if the user is enabled, false otherwise.
     */
    @Override
    public boolean isEnabled() {
        return this.enabled;
    }
}