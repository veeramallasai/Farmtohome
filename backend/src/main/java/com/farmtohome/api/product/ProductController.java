package com.farmtohome.api.product;

import com.farmtohome.api.common.ApiException;
import com.farmtohome.api.common.ApiResponse;
import java.math.BigDecimal;
import java.time.Instant;
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
  ApiResponse<ProductView> create(@RequestBody Map<String, Object> body) {
    String id = stringValue(body, "id", null);
    if (id == null || id.isBlank()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Product id is required.");
    }
    if (products.existsById(id.trim())) {
      throw new ApiException(HttpStatus.CONFLICT, "Product already exists.");
    }
    ProductEntity product = new ProductEntity();
    product.setId(id.trim());
    applyRequest(product, body);
    Instant now = Instant.now();
    product.setCreatedAt(now);
    product.setUpdatedAt(now);
    ProductEntity saved = products.save(product);
    return ApiResponse.ok(ProductView.from(saved, "home"), "Product created.");
  }

  @PutMapping("/{id}")
  ApiResponse<ProductView> update(@PathVariable String id, @RequestBody Map<String, Object> body) {
    ProductEntity product = products.findById(id)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found."));
    applyRequest(product, body);
    product.setUpdatedAt(Instant.now());
    ProductEntity saved = products.save(product);
    return ApiResponse.ok(ProductView.from(saved, "home"), "Product updated.");
  }

  @DeleteMapping("/{id}")
  ApiResponse<Map<String, String>> delete(@PathVariable String id) {
    ProductEntity product = products.findById(id)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found."));
    products.delete(product);
    return ApiResponse.ok(Map.of("id", id), "Product deleted.");
  }

  private void applyRequest(ProductEntity product, Map<String, Object> body) {
    product.setName(stringValue(body, "name", product.getName()));
    product.setEnglishName(stringValue(body, "englishName", product.getEnglishName()));
    product.setTeluguName(stringValue(body, "teluguName", product.getTeluguName()));
    product.setDescription(stringValue(body, "description", product.getDescription()));
    product.setCategory(stringValue(body, "category", product.getCategory()));
    product.setImageUrl(stringValue(body, "imageUrl", product.getImageUrl()));
    product.setUnit(stringValue(body, "unit", product.getUnit()));
    product.setPrice(decimalValue(body, "price", product.getPrice()));
    product.setMrp(decimalValue(body, "mrp", product.getMrp()));
    product.setShopUnit(stringValue(body, "shopUnit", product.getShopUnit()));
    product.setShopPrice(decimalValue(body, "shopPrice", product.getShopPrice()));
    product.setShopMrp(decimalValue(body, "shopMrp", product.getShopMrp()));
    product.setStockQuantity(intValue(body, "stockQuantity", product.getStockQuantity()));
    product.setActive(boolValue(body, "active", product.isActive()));
    product.setFresh(boolValue(body, "fresh", product.isFresh()));
    product.setAvailable(boolValue(body, "available", product.isAvailable()));
    product.setDeleted(boolValue(body, "deleted", product.isDeleted()));
    product.setRating(decimalValue(body, "rating", product.getRating()));
    product.setReviewCount(intValue(body, "reviewCount", product.getReviewCount()));
  }

  private String stringValue(Map<String, Object> body, String key, String fallback) {
    Object value = body.get(key);
    return value == null ? fallback : String.valueOf(value);
  }

  private BigDecimal decimalValue(Map<String, Object> body, String key, BigDecimal fallback) {
    Object value = body.get(key);
    if (value == null) {
      return fallback == null ? BigDecimal.ZERO : fallback;
    }
    return new BigDecimal(String.valueOf(value));
  }

  private int intValue(Map<String, Object> body, String key, int fallback) {
    Object value = body.get(key);
    if (value instanceof Number number) {
      return number.intValue();
    }
    if (value != null) {
      return Integer.parseInt(String.valueOf(value));
    }
    return fallback;
  }

  private boolean boolValue(Map<String, Object> body, String key, boolean fallback) {
    Object value = body.get(key);
    if (value instanceof Boolean bool) {
      return bool;
    }
    if (value != null) {
      return Boolean.parseBoolean(String.valueOf(value));
    }
    return fallback;
  }
}
