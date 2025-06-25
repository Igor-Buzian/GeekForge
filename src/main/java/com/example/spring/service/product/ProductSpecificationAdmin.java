package com.example.spring.service.product;

import com.example.spring.dto.search.ProductFilterAdminDto;
import com.example.spring.entity.Product;
import com.example.spring.entity.Category;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;
import java.util.Arrays; // Для удобства обработки списка категорий

public class ProductSpecificationAdmin {

    public static Specification<Product> withFilters(ProductFilterAdminDto filterDto) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Фильтрация по имени продукта (регистронезависимая)
            if (filterDto.getName() != null && !filterDto.getName().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + filterDto.getName().toLowerCase() + "%"));
            }

            // Фильтрация по минимальной цене
            if (filterDto.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), filterDto.getMinPrice()));
            }

            // Фильтрация по максимальной цене
            if (filterDto.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), filterDto.getMaxPrice()));
            }

            // Фильтрация по дате создания (после)
            if (filterDto.getCreatedAfter() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("created"), filterDto.getCreatedAfter()));
            }

            // Фильтрация по дате создания (до)
            if (filterDto.getCreatedBefore() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("created"), filterDto.getCreatedBefore()));
            }

            // Фильтрация по дате обновления (после)
            if (filterDto.getUpdatedAfter() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("updated"), filterDto.getUpdatedAfter()));
            }

            // Фильтрация по дате обновления (до)
            if (filterDto.getUpdatedBefore() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("updated"), filterDto.getUpdatedBefore()));
            }

            // Фильтрация по категориям
            if (filterDto.getCategoryNames() != null && !filterDto.getCategoryNames().isEmpty()) {
                // Используем .split(",") для получения массива имен категорий
                List<String> categoryNameList = Arrays.asList(filterDto.getCategoryNames().split(","));

                // Создаем JOIN с таблицей категорий
                Join<Product, Category> categoryJoin = root.join("categories"); // 'categories' - это имя поля в Product

                CriteriaBuilder.In<String> inClause = cb.in(cb.lower(categoryJoin.get("name"))); // Приводим имя категории к нижнему регистру

                for (String name : categoryNameList) {
                    if (!name.trim().isEmpty()) { // Проверяем, что имя не пустое после обрезки пробелов
                        inClause.value(name.trim().toLowerCase()); // Приводим входящее имя к нижнему регистру и обрезаем пробелы
                    }
                }
                predicates.add(inClause);
            }

            // Объединяем все предикаты с помощью логического И (AND)
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}