package com.farmtohome.api.product;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record ProductRequest(
    String id,
    @NotBlank String name,
    String englishName,
    String teluguName,
    String description,
    @NotBlank String category,
    String imageUrl,
    @NotBlank String unit,
    BigDecimal price,
    BigDecimal mrp,
    String shopUnit,
    BigDecimal shopPrice,
    BigDecimal shopMrp,
    Integer stockQuantity,
    Boolean active,
    Boolean fresh,
    Boolean available,
    BigDecimal rating,
    Integer reviewCount) {}
