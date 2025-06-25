package com.example.spring.service.product;

import com.example.spring.dto.search.ProductFilterUserDto;
import com.example.spring.entity.Category;
import com.example.spring.entity.Product;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProductSpecificationUser {

    public static Specification<Product> withFilter(ProductFilterUserDto filterUserDto) {
        return (root, query, cb) -> {
           List<Predicate> predicates = new ArrayList<>();

           if(filterUserDto.getName()!=null && !filterUserDto.getName().isEmpty()){
               predicates.add(cb.like(cb.lower(root.get("name")), "%"+filterUserDto.getName()+"%"));
           }

            if(filterUserDto.getMinPrice()!=null){
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), filterUserDto.getMinPrice()));
            }

            if(filterUserDto.getMaxPrice()!=null){
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), filterUserDto.getMaxPrice()));
            }

           if(filterUserDto.getCategoryNames()!=null && !filterUserDto.getCategoryNames().isEmpty()){

               List<String> categories = Arrays.asList(filterUserDto.getCategoryNames().split(","));

               Join<Product, Category> categoryJoin = root.join("categories");

               CriteriaBuilder.In<String> cbCategory = cb.in(cb.lower(categoryJoin.get("name")));
              for (String category : categories){
                  if(!category.trim().isEmpty()){
                      cbCategory.value(category.trim());
                  }
              }

            predicates.add(cbCategory);

           }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
