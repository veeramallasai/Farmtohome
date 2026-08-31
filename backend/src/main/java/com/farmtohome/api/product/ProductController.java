package com.farmtohome.api.product;

import com.farmtohome.api.common.ApiException;
import com.farmtohome.api.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/products", "/v1/products"})
public class ProductController {
  private final ProductRepository products;
  private final ProductService productService;

  public ProductController(ProductRepository products, ProductService productService) {
    this.products = products;
    this.productService = productService;
  }

  @GetMapping
  ApiResponse<List<ProductView>> list(
      @RequestParam(defaultValue = "") String category,
      @RequestParam(defaultValue = "home") String shoppingMode,
      @RequestParam(defaultValue = "200") int limit) {
    List<ProductEntity> values = category.isBlank()
        ? products.findByActiveTrueOrderByCreatedAtDesc()
        : products.findByActiveTrueAndCategoryIgnoreCaseOrderByCreatedAtDesc(category.trim());
    int safeLimit = Math.max(1, Math.min(limit, 250));
    List<ProductView> data = values.stream()
        .limit(safeLimit)
        .map(value -> ProductView.from(value, shoppingMode))
        .toList();
    return ApiResponse.ok(data);
  }

  @GetMapping("/{id}")
  ApiResponse<ProductView> one(
      @PathVariable String id,
      @RequestParam(defaultValue = "home") String shoppingMode) {
    ProductEntity product = products.findById(id)
        .filter(ProductEntity::isActive)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found."));
    return ApiResponse.ok(ProductView.from(product, shoppingMode));
  }

  @PostMapping
  ApiResponse<ProductView> create(
      @Valid @RequestBody ProductDtos.CreateProductRequest request) {
    return ApiResponse.ok(productService.create(request), "Product created.");
  }

  @PutMapping("/{id}")
  ApiResponse<ProductView> update(
      @PathVariable String id,
      @Valid @RequestBody ProductDtos.UpdateProductRequest request) {
    return ApiResponse.ok(productService.update(id, request), "Product updated.");
  }

  @DeleteMapping("/{id}")
  ApiResponse<Map<String, String>> delete(@PathVariable String id) {
    productService.delete(id);
    return ApiResponse.ok(Map.of("id", id), "Product deleted.");
  }
}
