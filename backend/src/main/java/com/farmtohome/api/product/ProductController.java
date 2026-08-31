package com.farmtohome.api.product;

import com.farmtohome.api.common.ApiException;
import com.farmtohome.api.common.ApiResponse;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

  public ProductController(ProductRepository products) {
    this.products = products;
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
  ApiResponse<ProductView> create(@Valid @RequestBody ProductRequest request) {
    String id = (request.id() == null || request.id().isBlank())
        ? UUID.randomUUID().toString()
        : request.id().trim();
    if (products.existsById(id)) {
      throw new ApiException(HttpStatus.CONFLICT, "Product already exists.");
    }
    ProductEntity product = new ProductEntity();
    product.setId(id);
    Instant now = Instant.now();
    product.setCreatedAt(now);
    product.setUpdatedAt(now);
    apply(product, request);
    return ApiResponse.ok(ProductView.from(products.save(product), "home"), "Product created.");
  }

  @PutMapping("/{id}")
  ApiResponse<ProductView> update(
      @PathVariable String id,
      @Valid @RequestBody ProductRequest request) {
    ProductEntity product = products.findById(id)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found."));
    apply(product, request);
    product.setUpdatedAt(Instant.now());
    return ApiResponse.ok(ProductView.from(products.save(product), "home"), "Product updated.");
  }

  @DeleteMapping("/{id}")
  ApiResponse<Map<String, String>> delete(@PathVariable String id) {
    ProductEntity product = products.findById(id)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found."));
    products.delete(product);
    return ApiResponse.ok(Map.of("id", id), "Product deleted.");
  }

  private void apply(ProductEntity product, ProductRequest request) {
    product.setName(text(request.name()));
    product.setEnglishName(request.englishName() == null || request.englishName().isBlank()
        ? text(request.name())
        : text(request.englishName()));
    product.setTeluguName(text(request.teluguName()));
    product.setDescription(text(request.description()));
    product.setCategory(text(request.category()));
    product.setImageUrl(text(request.imageUrl()));
    product.setUnit(text(request.unit()));
    product.setPrice(request.price() == null ? BigDecimal.ZERO : request.price());
    product.setMrp(request.mrp() == null ? BigDecimal.ZERO : request.mrp());
    product.setShopUnit(request.shopUnit() == null || request.shopUnit().isBlank()
        ? text(request.unit())
        : text(request.shopUnit()));
    product.setShopPrice(request.shopPrice() == null
        ? (request.price() == null ? BigDecimal.ZERO : request.price())
        : request.shopPrice());
    product.setShopMrp(request.shopMrp() == null
        ? (request.mrp() == null ? BigDecimal.ZERO : request.mrp())
        : request.shopMrp());
    product.setStockQuantity(request.stockQuantity() == null ? 0 : request.stockQuantity());
    product.setActive(request.active() == null || request.active());
    product.setFresh(request.fresh() != null && request.fresh());
    product.setAvailable(request.available() == null || request.available());
    product.setRating(request.rating() == null ? BigDecimal.ZERO : request.rating());
    product.setReviewCount(request.reviewCount() == null ? 0 : request.reviewCount());
  }

  private static String text(String value) {
    return value == null ? "" : value.trim();
  }
}
