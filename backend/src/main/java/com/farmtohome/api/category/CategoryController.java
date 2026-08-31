package com.farmtohome.api.category;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/categories", "/v1/categories", "/api/v1/catalog/categories", "/v1/catalog/categories"})
public class CategoryController {

  private final CategoryRepository categoryRepository;

  public CategoryController(CategoryRepository categoryRepository) {
    this.categoryRepository = categoryRepository;
  }

  @GetMapping
  public List<CategoryEntity> listCategories() {
    return categoryRepository.findByOrderByNameAsc();
  }
}
