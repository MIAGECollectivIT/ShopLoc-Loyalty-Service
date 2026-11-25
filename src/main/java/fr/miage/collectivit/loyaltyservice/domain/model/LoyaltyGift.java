package fr.miage.collectivit.loyaltyservice.domain.model;

import fr.miage.collectivit.loyaltyservice.domain.exception.InsufficientGiftStockException;
import fr.miage.collectivit.loyaltyservice.domain.exception.InvalidGiftException;
import java.time.Instant;
import java.util.UUID;

/**
 * Pure domain model representing an item in a partner merchant's loyalty gift catalog (Story
 * SLE-60).
 */
@SuppressWarnings("PMD.NullAssignment")
public class LoyaltyGift {

  public static final int MAX_GIFT_STOCK = 1_000_000;

  private Long id;
  private UUID uid;
  private final UUID storeUid;
  private String name;
  private String description;
  private Integer pointsCost;
  private Integer stock;
  private String imageUrl;
  private boolean active;
  private boolean vfpExclusive;
  private Instant createdAt;
  private Instant updatedAt;

  @SuppressWarnings("PMD.ExcessiveParameterList")
  public LoyaltyGift(
      UUID storeUid,
      String name,
      String description,
      Integer pointsCost,
      Integer stock,
      String imageUrl) {
    this(storeUid, name, description, pointsCost, stock, imageUrl, false);
  }

  @SuppressWarnings("PMD.ExcessiveParameterList")
  public LoyaltyGift(
      UUID storeUid,
      String name,
      String description,
      Integer pointsCost,
      Integer stock,
      String imageUrl,
      boolean vfpExclusive) {
    this(
        null,
        UUID.randomUUID(),
        storeUid,
        name,
        description,
        pointsCost,
        stock,
        imageUrl,
        true,
        vfpExclusive,
        Instant.now(),
        Instant.now());
  }

  @SuppressWarnings("PMD.ExcessiveParameterList")
  public LoyaltyGift(
      Long id,
      UUID uid,
      UUID storeUid,
      String name,
      String description,
      Integer pointsCost,
      Integer stock,
      String imageUrl,
      boolean active,
      Instant createdAt,
      Instant updatedAt) {
    this(
        id,
        uid,
        storeUid,
        name,
        description,
        pointsCost,
        stock,
        imageUrl,
        active,
        false,
        createdAt,
        updatedAt);
  }

  @SuppressWarnings("PMD.ExcessiveParameterList")
  public LoyaltyGift(
      Long id,
      UUID uid,
      UUID storeUid,
      String name,
      String description,
      Integer pointsCost,
      Integer stock,
      String imageUrl,
      boolean active,
      boolean vfpExclusive,
      Instant createdAt,
      Instant updatedAt) {
    validateStoreUid(storeUid);
    validateName(name);
    validatePointsCost(pointsCost);
    validateStock(stock);

    this.id = id;
    this.uid = uid != null ? uid : UUID.randomUUID();
    this.storeUid = storeUid;
    this.name = name.trim();
    this.description = description != null ? description.trim() : null;
    this.pointsCost = pointsCost;
    this.stock = stock != null ? stock : 0;
    this.imageUrl = imageUrl != null && !imageUrl.isBlank() ? imageUrl.trim() : null;
    this.active = active;
    this.vfpExclusive = vfpExclusive;
    this.createdAt = createdAt != null ? createdAt : Instant.now();
    this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
  }

  public void update(
      String newName, String newDescription, Integer newPointsCost, String newImageUrl) {
    update(newName, newDescription, newPointsCost, newImageUrl, this.vfpExclusive);
  }

  public void update(
      String newName,
      String newDescription,
      Integer newPointsCost,
      String newImageUrl,
      boolean newVfpExclusive) {
    validateName(newName);
    validatePointsCost(newPointsCost);

    this.name = newName.trim();
    this.description = newDescription != null ? newDescription.trim() : null;
    this.pointsCost = newPointsCost;
    this.imageUrl = newImageUrl != null && !newImageUrl.isBlank() ? newImageUrl.trim() : null;
    this.vfpExclusive = newVfpExclusive;
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

  private void validateStoreUid(UUID storeUid) {
    if (storeUid == null) {
      throw new InvalidGiftException("Store UID is mandatory.");
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

  public UUID getUid() {
    return uid;
  }

  public void setUid(UUID uid) {
    this.uid = uid;
  }

  public UUID getStoreUid() {
    return storeUid;
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

  public boolean isVfpExclusive() {
    return vfpExclusive;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
