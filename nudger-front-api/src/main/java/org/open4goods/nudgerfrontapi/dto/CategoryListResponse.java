package org.open4goods.nudgerfrontapi.dto;

import java.util.List;

import org.open4goods.nudgerfrontapi.service.CategoryService.CategoryDto;

public record CategoryListResponse(int page,
                                   int size,
                                   long total,
                                   List<CategoryDto> items) {
}
