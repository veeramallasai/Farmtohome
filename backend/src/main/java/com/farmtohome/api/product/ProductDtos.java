package com.farmtohome.api.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public final class ProductDtos {
  private ProductDtos() {}

  public record CreateProductRequest(
      @NotBlank @Size(max = 255) String name,
      @NotBlank @Size(max = 160) String englishName,
      @NotBlank @Size(max = 160) String teluguName,
      @NotBlank @Size(max = 800) String description,
      @NotBlank @Size(max = 80) String category,
      @NotBlank @Size(max = 500) String imageUrl,
      @NotBlank @Size(max = 80) String unit,
      @NotNull @DecimalMin(value = "0.01", message = "must be greater than 0") BigDecimal price,
      @NotNull @DecimalMin(value = "0.01", message = "must be greater than 0") BigDecimal mrp,
      @NotBlank @Size(max = 80) String shopUnit,
      @NotNull @DecimalMin(value = "0.01", message = "must be greater than 0") BigDecimal shopPrice,
      @NotNull @DecimalMin(value = "0.01", message = "must be greater than 0") BigDecimal shopMrp,
      @Min(0) int stockQuantity,
      boolean fresh,
      boolean active) {}

  public record UpdateProductRequest(
      @NotBlank @Size(max = 255) String name,
      @NotBlank @Size(max = 160) String englishName,
      @NotBlank @Size(max = 160) String teluguName,
      @NotBlank @Size(max = 800) String description,
      @NotBlank @Size(max = 80) String category,
      @NotBlank @Size(max = 500) String imageUrl,
      @NotBlank @Size(max = 80) String unit,
      @NotNull @DecimalMin(value = "0.01", message = "must be greater than 0") BigDecimal price,
      @NotNull @DecimalMin(value = "0.01", message = "must be greater than 0") BigDecimal mrp,
      @NotBlank @Size(max = 80) String shopUnit,
      @NotNull @DecimalMin(value = "0.01", message = "must be greater than 0") BigDecimal shopPrice,
      @NotNull @DecimalMin(value = "0.01", message = "must be greater than 0") BigDecimal shopMrp,
      @Min(0) int stockQuantity,
      boolean fresh,
      boolean active) {}
}
