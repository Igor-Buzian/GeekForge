package com.example.spring.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, length = 255, unique = true)
    private  String name;

    @Column(nullable = false, length = 255, unique = true)
    private  String slug;

    @ManyToMany(mappedBy = "categories", fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude // Рекомендуется для предотвращения StackOverflowError
    @ToString.Exclude // Рекомендуется для предотвращения StackOverflowError
    private Set<Product> product = new HashSet<>();
}
