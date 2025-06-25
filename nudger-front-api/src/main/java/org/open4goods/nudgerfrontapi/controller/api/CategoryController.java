package org.open4goods.nudgerfrontapi.controller.api;

import org.open4goods.nudgerfrontapi.dto.CategoryListResponse;
import org.open4goods.nudgerfrontapi.dto.CategoryRequest;
import org.open4goods.nudgerfrontapi.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/categories")
    public CategoryListResponse categories(CategoryRequest request) {
        int page = request.page() == null ? 0 : request.page();
        int size = request.size() == null ? 10 : request.size();
        var all = categoryService.listRootCategories(request.include());
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        return new CategoryListResponse(page, size, all.size(), all.subList(from, to));
    }

    @GetMapping("/categories/{id}")
    public CategoryService.CategoryDto category(@PathVariable int id, CategoryRequest request) {
        return categoryService.getCategory(id, request.include());
    }
}
