package com.example.spring.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Set;

/**
 * Represents a permission entity in the application.
 * This class maps to the "permissions" table in the database and defines
 * specific actions or resources that can be controlled. Permissions are typically
 * granted to roles, which are then assigned to users.
 */
@Entity
@Data
@Table(name = "permissions")
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @Column(nullable = false, unique = true)
    String name;

    @Column(nullable = false)
    String description;

}