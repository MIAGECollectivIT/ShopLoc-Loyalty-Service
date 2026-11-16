package fr.miage.collectivit.loyaltyservice.domain.model;

import fr.miage.collectivit.loyaltyservice.domain.exception.InsufficientGiftStockException;
import fr.miage.collectivit.loyaltyservice.domain.exception.InvalidGiftException;
import java.time.Instant;

/**
 * Pure domain model representing an item in a partner merchant's loyalty gift catalog (Story
 * SLE-60).
 */
@SuppressWarnings("PMD.NullAssignment")
public class LoyaltyGift {

  public static final int MAX_GIFT_STOCK = 1_000_000;

  private Long id;
  private final Long storeId;
  private String name;
  private String description;
  private Integer pointsCost;
  private Integer stock;
  private String imageUrl;
  private boolean active;
  private Instant createdAt;
  private Instant updatedAt;

  public LoyaltyGift(
      Long storeId,
      String name,
      String description,
      Integer pointsCost,
      Integer stock,
      String imageUrl) {
    this(
        null,
        storeId,
        name,
        description,
        pointsCost,
        stock,
        imageUrl,
        true,
        Instant.now(),
        Instant.now());
  }

  public LoyaltyGift(
      Long id,
      Long storeId,
      String name,
      String description,
      Integer pointsCost,
      Integer stock,
      String imageUrl,
      boolean active,
      Instant createdAt,
      Instant updatedAt) {
    validateStoreId(storeId);
    validateName(name);
    validatePointsCost(pointsCost);
    validateStock(stock);

    this.id = id;
    this.storeId = storeId;
    this.name = name.trim();
    this.description = description != null ? description.trim() : null;
    this.pointsCost = pointsCost;
    this.stock = stock != null ? stock : 0;
    this.imageUrl = imageUrl != null && !imageUrl.isBlank() ? imageUrl.trim() : null;
    this.active = active;
    this.createdAt = createdAt != null ? createdAt : Instant.now();
    this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
  }

  public void update(
      String newName, String newDescription, Integer newPointsCost, String newImageUrl) {
    validateName(newName);
    validatePointsCost(newPointsCost);

    this.name = newName.trim();
    this.description = newDescription != null ? newDescription.trim() : null;
    this.pointsCost = newPointsCost;
    this.imageUrl = newImageUrl != null && !newImageUrl.isBlank() ? newImageUrl.trim() : null;
    this.updatedAt = Instant.now();
  }

  public void adjustStock(int delta) {
    long newStockLong = (long) this.stock + delta;
    if (newStockLong < 0) {
      throw new InsufficientGiftStockException(
          String.format(
              "Insufficient gift stock: current is %d, requested deduction is %d.",
              this.stock, Math.abs(delta)));
    }
    if (newStockLong > MAX_GIFT_STOCK) {
      throw new InvalidGiftException(
          String.format("Gift stock cannot exceed %d units.", MAX_GIFT_STOCK));
    }
    this.stock = (int) newStockLong;
    this.updatedAt = Instant.now();
  }

  public void deactivate() {
    this.active = false;
    this.updatedAt = Instant.now();
  }

  public void activate() {
    this.active = true;
    this.updatedAt = Instant.now();
  }

  public boolean hasAvailableStock() {
    return this.stock > 0;
  }

  private void validateStoreId(Long storeId) {
    if (storeId == null) {
      throw new InvalidGiftException("Store ID is mandatory.");
    }
    if (storeId <= 0) {
      throw new InvalidGiftException("Store ID must be strictly positive.");
    }
  }

  private void validateName(String name) {
    if (name == null || name.isBlank()) {
      throw new InvalidGiftException("Gift name is mandatory.");
    }
  }

  private void validatePointsCost(Integer pointsCost) {
    if (pointsCost == null || pointsCost <= 0) {
      throw new InvalidGiftException("Points cost must be strictly positive (> 0).");
    }
  }

  private void validateStock(Integer stock) {
    if (stock != null && stock < 0) {
      throw new InvalidGiftException("Stock cannot be negative.");
    }
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getStoreId() {
    return storeId;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public Integer getPointsCost() {
    return pointsCost;
  }

  public Integer getStock() {
    return stock;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public boolean isActive() {
    return active;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
