package com.farmtohome.api.cart;

import com.farmtohome.api.common.ApiException;
import com.farmtohome.api.product.ProductEntity;
import com.farmtohome.api.product.ProductRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {
  private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
  private final CartRepository carts;
  private final CartItemRepository items;
  private final ProductRepository products;

  public CartService(
      CartRepository carts,
      CartItemRepository items,
      ProductRepository products) {
    this.carts = carts;
    this.items = items;
    this.products = products;
  }

  @Transactional(readOnly = true)
  public CartDtos.Cart view(String uid) {
    CartEntity cart = carts.findById(uid).orElse(new CartEntity(uid, "home"));
    return build(uid, cart, items.findByOwnerUidOrderByUpdatedAtDesc(uid));
  }

  @Transactional
  public CartDtos.Cart add(String uid, CartDtos.AddItemRequest request) {
    String mode = "shop".equalsIgnoreCase(request.shoppingMode()) ? "shop" : "home";
    ProductEntity product = products.findById(request.productId().trim())
        .filter(ProductEntity::isActive)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product is unavailable."));
    if (product.getStockQuantity() < request.quantity()) {
      throw new ApiException(HttpStatus.CONFLICT, "Requested quantity is not in stock.");
    }
    CartEntity cart = carts.findById(uid).orElseGet(() -> new CartEntity(uid, mode));
    List<CartItemEntity> currentItems = items.findByOwnerUidOrderByUpdatedAtDesc(uid);
    if (!currentItems.isEmpty() && !cart.getShoppingMode().equals(mode)) {
      throw new ApiException(HttpStatus.CONFLICT, "Clear the current cart before changing shopping mode.");
    }
    cart.setShoppingMode(mode);
    cart.touch();
    carts.save(cart);

    String defaultUnit = mode.equals("shop") ? product.getShopUnit() : product.getUnit();
    String unit = request.unit() == null || request.unit().isBlank()
        ? defaultUnit : request.unit().trim();
    String itemKey = request.itemId() == null || request.itemId().isBlank()
        ? product.getId() + "_" + mode
        : request.itemId().trim();
    if (itemKey.length() > 180) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid cart item.");
    }
    CartItemEntity item = items.findByOwnerUidAndItemKey(uid, itemKey)
        .orElseGet(() -> new CartItemEntity(uid, itemKey, product.getId(), mode, unit, 0));
    int quantity = Math.min(99, item.getQuantity() + request.quantity());
    if (quantity > product.getStockQuantity()) {
      throw new ApiException(HttpStatus.CONFLICT, "Requested quantity is not in stock.");
    }
    item.setQuantity(quantity);
    items.save(item);
    return view(uid);
  }

  @Transactional
  public CartDtos.Cart quantity(String uid, String itemKey, int quantity) {
    CartItemEntity item = items.findByOwnerUidAndItemKey(uid, itemKey)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cart item not found."));
    if (quantity <= 0) {
      items.delete(item);
    } else {
      ProductEntity product = products.findById(item.getProductId())
          .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product is unavailable."));
      if (quantity > product.getStockQuantity()) {
        throw new ApiException(HttpStatus.CONFLICT, "Only " + product.getStockQuantity() + " available.");
      }
      item.setQuantity(Math.min(quantity, 99));
      items.save(item);
    }
    carts.findById(uid).ifPresent(value -> { value.touch(); carts.save(value); });
    return view(uid);
  }

  @Transactional
  public CartDtos.Cart remove(String uid, String itemKey) {
    items.deleteByOwnerUidAndItemKey(uid, itemKey);
    carts.findById(uid).ifPresent(value -> { value.touch(); carts.save(value); });
    return view(uid);
  }

  @Transactional
  public CartDtos.Cart clear(String uid) {
    items.deleteByOwnerUid(uid);
    CartEntity cart = carts.findById(uid).orElseGet(() -> new CartEntity(uid, "home"));
    cart.touch();
    carts.save(cart);
    return view(uid);
  }

  private CartDtos.Cart build(String uid, CartEntity cart, List<CartItemEntity> cartItems) {
    Map<String, ProductEntity> productMap = products.findAllById(
            cartItems.stream().map(CartItemEntity::getProductId).toList()).stream()
        .collect(Collectors.toMap(ProductEntity::getId, Function.identity()));
    List<CartDtos.Item> result = new ArrayList<>();
    BigDecimal subtotal = ZERO;
    BigDecimal mrpTotal = ZERO;
    int itemCount = 0;
    for (CartItemEntity item : cartItems) {
      ProductEntity product = productMap.get(item.getProductId());
      if (product == null || !product.isActive()) continue;
      boolean shop = "shop".equals(item.getShoppingMode());
      BigDecimal price = shop ? product.getShopPrice() : product.getPrice();
      BigDecimal mrp = shop ? product.getShopMrp() : product.getMrp();
      subtotal = subtotal.add(price.multiply(BigDecimal.valueOf(item.getQuantity())));
      mrpTotal = mrpTotal.add(mrp.multiply(BigDecimal.valueOf(item.getQuantity())));
      itemCount += item.getQuantity();
      result.add(new CartDtos.Item(
          item.getItemKey(), product.getId(), product.getName(), product.getImageUrl(),
          product.getCategory(), item.getUnit(), item.getShoppingMode(), price, mrp,
          item.getQuantity(), ""));
    }
    BigDecimal total = subtotal.max(ZERO);
    return new CartDtos.Cart(
        uid, cart.getShoppingMode(), result,
        money(subtotal), money(mrpTotal.subtract(subtotal).max(ZERO)), money(total),
        itemCount, cart.getUpdatedAt() == null ? Instant.now() : cart.getUpdatedAt());
  }

  public static BigDecimal money(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP);
  }
}
