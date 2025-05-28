package com.example.spring.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a role entity in the application, implementing Spring Security's GrantedAuthority interface.
 * This class maps to the "roles" table in the database and defines the various roles
 * a user can have within the system, along with their associated permissions.
 */
@Entity
@Data
@Table(name="roles")
public class Role implements GrantedAuthority {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @Column(nullable = false,unique = true)
    String name;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    Set<Permission> permissions= new HashSet<>();

    /**
     * Returns the authority string for this role.
     * This method is part of the GrantedAuthority interface and is used by Spring Security
     * to determine the user's permissions.
     *
     * @return The name of the role as a string.
     */
    @Override
    public String getAuthority() {
        return name;
    }
}