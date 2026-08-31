package com.farmtohome.api.product;

import com.farmtohome.api.common.ApiException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
  private final ProductRepository products;

  public ProductService(ProductRepository products) {
    this.products = products;
  }

  @Transactional
  public ProductView create(ProductDtos.CreateProductRequest request) {
    validatePricing(
        request.price(), request.mrp(), request.shopPrice(), request.shopMrp(),
        request.stockQuantity());
    ProductEntity product = new ProductEntity();
    product.setId(generateId(request.name()));
    apply(
        product,
        request.name(), request.englishName(), request.teluguName(), request.description(),
        request.category(), request.imageUrl(), request.unit(), request.price(), request.mrp(),
        request.shopUnit(), request.shopPrice(), request.shopMrp(), request.stockQuantity(),
        request.fresh(), request.active());
    Instant now = Instant.now();
    product.setRating(BigDecimal.ZERO);
    product.setReviewCount(0);
    product.setAvailable(true);
    product.setDeleted(false);
    product.setCreatedAt(now);
    product.setUpdatedAt(now);
    return ProductView.from(products.save(product), "home");
  }

  @Transactional
  public ProductView update(String id, ProductDtos.UpdateProductRequest request) {
    validatePricing(
        request.price(), request.mrp(), request.shopPrice(), request.shopMrp(),
        request.stockQuantity());
    ProductEntity product = products.findForUpdate(id);
    if (product == null || product.isDeleted()) {
      throw new ApiException(HttpStatus.NOT_FOUND, "Product not found.");
    }
    apply(
        product,
        request.name(), request.englishName(), request.teluguName(), request.description(),
        request.category(), request.imageUrl(), request.unit(), request.price(), request.mrp(),
        request.shopUnit(), request.shopPrice(), request.shopMrp(), request.stockQuantity(),
        request.fresh(), request.active());
    product.setUpdatedAt(Instant.now());
    return ProductView.from(products.save(product), "home");
  }

  @Transactional
  public void delete(String id) {
    ProductEntity product = products.findForUpdate(id);
    if (product == null || product.isDeleted()) {
      throw new ApiException(HttpStatus.NOT_FOUND, "Product not found.");
    }
    product.setDeleted(true);
    product.setActive(false);
    product.setAvailable(false);
    product.setUpdatedAt(Instant.now());
    products.save(product);
  }

  private void apply(
      ProductEntity product,
      String name,
      String englishName,
      String teluguName,
      String description,
      String category,
      String imageUrl,
      String unit,
      BigDecimal price,
      BigDecimal mrp,
      String shopUnit,
      BigDecimal shopPrice,
      BigDecimal shopMrp,
      int stockQuantity,
      boolean fresh,
      boolean active) {
    product.setName(text(name));
    product.setEnglishName(text(englishName));
    product.setTeluguName(text(teluguName));
    product.setDescription(text(description));
    product.setCategory(text(category).toLowerCase(Locale.ROOT));
    product.setImageUrl(text(imageUrl));
    product.setUnit(text(unit));
    product.setPrice(price);
    product.setMrp(mrp);
    product.setShopUnit(text(shopUnit));
    product.setShopPrice(shopPrice);
    product.setShopMrp(shopMrp);
    product.setStockQuantity(stockQuantity);
    product.setFresh(fresh);
    product.setActive(active);
  }

  private void validatePricing(
      BigDecimal price,
      BigDecimal mrp,
      BigDecimal shopPrice,
      BigDecimal shopMrp,
      int stockQuantity) {
    if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Price must be positive.");
    }
    if (mrp == null || mrp.compareTo(price) < 0) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "MRP must be greater than or equal to price.");
    }
    if (shopPrice == null || shopPrice.compareTo(BigDecimal.ZERO) <= 0) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Shop price must be positive.");
    }
    if (shopMrp == null || shopMrp.compareTo(shopPrice) < 0) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST, "Shop MRP must be greater than or equal to shop price.");
    }
    if (stockQuantity < 0) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Stock quantity cannot be negative.");
    }
  }

  private String generateId(String name) {
    String base = text(name).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_")
        .replaceAll("^_+|_+$", "");
    if (base.isBlank()) {
      base = "product";
    }
    String candidate = base;
    int suffix = 0;
    while (products.existsById(candidate)) {
      suffix += 1;
      candidate = base + "_" + suffix;
    }
    if (candidate.length() > 100) {
      candidate = candidate.substring(0, 100) + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
    return candidate;
  }

  private static String text(String value) {
    return value == null ? "" : value.trim();
  }
}
