package com.example.spring.service;

import com.example.spring.entity.Category;
import com.example.spring.entity.Product;
import com.example.spring.entity.User;
import com.example.spring.repository.CategoryRepository;
import com.example.spring.repository.ProductRepository;
import com.example.spring.repository.RoleRepository;
import com.example.spring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Service class for managing user-related operations, primarily focusing on
 * retrieving user details and roles from the database.
 */
@RequiredArgsConstructor
@Service
public class RepositoryService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private  final CategoryRepository categoryRepository;
    private  final ProductRepository productRepository;

    /**
     * Loads a user by their email address. This method is typically used by Spring Security
     * for authentication purposes.
     *
     * @param email The email address of the user to load.
     * @return The User entity corresponding to the given email.
     * @throws UsernameNotFoundException if a user with the specified email is not found.
     */
    public User loadLogin(String email){
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Not found user with name " + email));
    }

    /**
     * Loads a role by its name.
     *
     * @param roleName The name of the role to load (e.g., "ROLE_USER", "ROLE_ADMIN").
     * @return The name of the role if found.
     * @throws UsernameNotFoundException if a role with the specified name is not found.
     */
    public String loadRole(String roleName){
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new UsernameNotFoundException("Role not found with name: " + roleName))
                .getName();
    }

    public Category loadCategoryByName(String name){
        return  categoryRepository.findByName(name)
                .orElseThrow(()-> new UsernameNotFoundException("Category not found with name: "+name));
    }

    public  Product loadProductByName(String name){
        return  productRepository.findByName(name)
                .orElseThrow(()-> new UsernameNotFoundException("Category not found with name: "+name));
    }

    public  Product loadProductById(Long id){
        return  productRepository.findById(id)
                .orElseThrow(()-> new UsernameNotFoundException("Product not found with name: "+id));
    }

}