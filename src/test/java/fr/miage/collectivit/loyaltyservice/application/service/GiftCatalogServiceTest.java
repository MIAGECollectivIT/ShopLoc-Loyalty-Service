package fr.miage.collectivit.loyaltyservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.miage.collectivit.loyaltyservice.domain.exception.GiftNotFoundException;
import fr.miage.collectivit.loyaltyservice.domain.exception.StoreAccessDeniedException;
import fr.miage.collectivit.loyaltyservice.domain.exception.StoreNotFoundException;
import fr.miage.collectivit.loyaltyservice.domain.model.GiftImageResource;
import fr.miage.collectivit.loyaltyservice.domain.model.LoyaltyGift;
import fr.miage.collectivit.loyaltyservice.domain.port.in.GiftCreateCommand;
import fr.miage.collectivit.loyaltyservice.domain.port.in.GiftUpdateCommand;
import fr.miage.collectivit.loyaltyservice.domain.port.out.GiftImageStoragePort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.LoyaltyGiftPersistencePort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.StoreVerificationPort;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Application Service - GiftCatalogService Unit Tests (SLE-60 / SLE-192, SLE-193)")
class GiftCatalogServiceTest {

  private LoyaltyGiftPersistencePort persistencePort;
  private StoreVerificationPort storeVerificationPort;
  private GiftImageStoragePort giftImageStoragePort;
  private GiftCatalogService service;

  @BeforeEach
  void setUp() {
    persistencePort = mock(LoyaltyGiftPersistencePort.class);
    storeVerificationPort = mock(StoreVerificationPort.class);
    giftImageStoragePort = mock(GiftImageStoragePort.class);
    service = new GiftCatalogService(persistencePort, storeVerificationPort, giftImageStoragePort);
  }

  @Test
  @DisplayName("Should successfully create a loyalty gift when caller is store owner")
  void shouldCreateGiftWhenCallerIsStoreOwner() {
    Long storeId = 10L;
    Long merchantUserId = 42L;
    when(storeVerificationPort.findStoreOwnerId(storeId)).thenReturn(Optional.of(merchantUserId));

    GiftCreateCommand command =
        new GiftCreateCommand(
            "Mug ShopLoc", "Mug céramique", 75, 20, "https://cdn.shoploc.fr/mug.png");

    when(persistencePort.save(any(LoyaltyGift.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    LoyaltyGift created = service.createGift(storeId, command, merchantUserId, "MERCHANT");

    assertThat(created).isNotNull();
    assertThat(created.getStoreId()).isEqualTo(storeId);
    assertThat(created.getName()).isEqualTo("Mug ShopLoc");
    assertThat(created.getPointsCost()).isEqualTo(75);
    assertThat(created.getStock()).isEqualTo(20);
    assertThat(created.isActive()).isTrue();
    verify(persistencePort).save(any(LoyaltyGift.class));
  }

  @Test
  @DisplayName("Should allow ADMIN to create gift without checking owner id")
  void shouldAllowAdminToCreateGift() {
    Long storeId = 10L;
    GiftCreateCommand command = new GiftCreateCommand("Mug ShopLoc", "Mug céramique", 75, 20, null);

    when(persistencePort.save(any(LoyaltyGift.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    LoyaltyGift created = service.createGift(storeId, command, 999L, "ADMIN");

    assertThat(created.getName()).isEqualTo("Mug ShopLoc");
  }

  @Test
  @DisplayName("Should reject gift creation when caller is not store owner")
  void shouldRejectCreationWhenNotOwner() {
    Long storeId = 10L;
    Long trueOwnerId = 42L;
    Long otherMerchantId = 99L;
    when(storeVerificationPort.findStoreOwnerId(storeId)).thenReturn(Optional.of(trueOwnerId));

    GiftCreateCommand command = new GiftCreateCommand("Mug ShopLoc", "Mug céramique", 75, 20, null);

    assertThatThrownBy(() -> service.createGift(storeId, command, otherMerchantId, "MERCHANT"))
        .isInstanceOf(StoreAccessDeniedException.class)
        .hasMessageContaining("you are not the owner of store 10");
  }

  @Test
  @DisplayName("Should reject gift creation when store does not exist")
  void shouldRejectCreationWhenStoreNotFound() {
    Long storeId = 999L;
    when(storeVerificationPort.findStoreOwnerId(storeId)).thenReturn(Optional.empty());

    GiftCreateCommand command = new GiftCreateCommand("Mug ShopLoc", "Mug céramique", 75, 20, null);

    assertThatThrownBy(() -> service.createGift(storeId, command, 42L, "MERCHANT"))
        .isInstanceOf(StoreNotFoundException.class);
  }

  @Test
  @DisplayName("Should successfully update gift details")
  void shouldUpdateGiftDetails() {
    Long storeId = 10L;
    Long giftId = 1L;
    Long merchantUserId = 42L;
    when(storeVerificationPort.findStoreOwnerId(storeId)).thenReturn(Optional.of(merchantUserId));

    LoyaltyGift existing =
        new LoyaltyGift(
            giftId, storeId, "Ancien Nom", "Ancienne desc", 50, 10, null, true, null, null);
    when(persistencePort.findByIdAndStoreId(giftId, storeId)).thenReturn(Optional.of(existing));
    when(persistencePort.save(any(LoyaltyGift.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    GiftUpdateCommand updateCommand =
        new GiftUpdateCommand(
            "Nouveau Nom", "Nouvelle desc", 100, "https://cdn.shoploc.fr/img.png");

    LoyaltyGift updated =
        service.updateGift(storeId, giftId, updateCommand, merchantUserId, "MERCHANT");

    assertThat(updated.getName()).isEqualTo("Nouveau Nom");
    assertThat(updated.getDescription()).isEqualTo("Nouvelle desc");
    assertThat(updated.getPointsCost()).isEqualTo(100);
    assertThat(updated.getImageUrl()).isEqualTo("https://cdn.shoploc.fr/img.png");
  }

  @Test
  @DisplayName("Should adjust gift stock dedicated inventory (SLE-193)")
  void shouldAdjustGiftStock() {
    Long storeId = 10L;
    Long giftId = 1L;
    Long merchantUserId = 42L;
    when(storeVerificationPort.findStoreOwnerId(storeId)).thenReturn(Optional.of(merchantUserId));

    LoyaltyGift existing =
        new LoyaltyGift(giftId, storeId, "Mug", "Desc", 50, 10, null, true, null, null);
    when(persistencePort.findByIdAndStoreId(giftId, storeId)).thenReturn(Optional.of(existing));
    when(persistencePort.save(any(LoyaltyGift.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    LoyaltyGift result = service.adjustStock(storeId, giftId, 15, merchantUserId, "MERCHANT");

    assertThat(result.getStock()).isEqualTo(25);
  }

  @Test
  @DisplayName("Should delete/deactivate a gift from the catalog")
  void shouldDeleteGift() {
    Long storeId = 10L;
    Long giftId = 1L;
    Long merchantUserId = 42L;
    when(storeVerificationPort.findStoreOwnerId(storeId)).thenReturn(Optional.of(merchantUserId));

    LoyaltyGift existing =
        new LoyaltyGift(giftId, storeId, "Mug", "Desc", 50, 10, null, true, null, null);
    when(persistencePort.findByIdAndStoreId(giftId, storeId)).thenReturn(Optional.of(existing));

    service.deleteGift(storeId, giftId, merchantUserId, "MERCHANT");

    verify(persistencePort).deleteById(giftId);
  }

  @Test
  @DisplayName("Should retrieve active gifts or all gifts for a store")
  void shouldRetrieveGifts() {
    Long storeId = 10L;
    LoyaltyGift active =
        new LoyaltyGift(1L, storeId, "Mug", "Desc", 50, 10, null, true, null, null);
    LoyaltyGift inactive =
        new LoyaltyGift(2L, storeId, "Stylo", "Desc", 20, 0, null, false, null, null);

    when(persistencePort.findByStoreId(storeId)).thenReturn(List.of(active, inactive));
    when(persistencePort.findActiveByStoreId(storeId)).thenReturn(List.of(active));

    List<LoyaltyGift> all = service.getGiftsByStore(storeId, true);
    assertThat(all).hasSize(2);

    List<LoyaltyGift> onlyActive = service.getGiftsByStore(storeId, false);
    assertThat(onlyActive).hasSize(1);
    assertThat(onlyActive.get(0).getName()).isEqualTo("Mug");
  }

  @Test
  @DisplayName("Should throw GiftNotFoundException when giftId does not exist in store")
  void shouldThrowWhenGiftNotFound() {
    Long storeId = 10L;
    Long giftId = 999L;
    when(storeVerificationPort.findStoreOwnerId(storeId)).thenReturn(Optional.of(42L));
    when(persistencePort.findByIdAndStoreId(giftId, storeId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getGiftById(storeId, giftId))
        .isInstanceOf(GiftNotFoundException.class);
  }

  @Test
  @DisplayName("Should create gift and upload image to S3 when binary stream is provided")
  void shouldCreateGiftWithImageUpload() {
    Long storeId = 10L;
    Long merchantUserId = 42L;
    when(storeVerificationPort.findStoreOwnerId(storeId)).thenReturn(Optional.of(merchantUserId));

    byte[] img = "fake-img".getBytes(StandardCharsets.UTF_8);
    ByteArrayInputStream stream = new ByteArrayInputStream(img);
    when(giftImageStoragePort.storeImage(any(), any(Long.class), any(), any()))
        .thenReturn("http://minio:9000/product-images/uploaded.png");

    GiftCreateCommand command = new GiftCreateCommand("Mug ShopLoc", "Mug céramique", 75, 20, null);

    when(persistencePort.save(any(LoyaltyGift.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    LoyaltyGift created =
        service.createGift(
            storeId,
            command,
            stream,
            img.length,
            "mug.png",
            "image/png",
            merchantUserId,
            "MERCHANT");

    assertThat(created.getImageUrl()).isEqualTo("http://minio:9000/product-images/uploaded.png");
    verify(giftImageStoragePort).storeImage(any(), any(Long.class), any(), any());
    verify(persistencePort).save(any(LoyaltyGift.class));
  }

  @Test
  @DisplayName("Should update gift and replace old image on S3 when new binary stream is provided")
  void shouldUpdateGiftReplacingOldImage() {
    Long storeId = 10L;
    Long giftId = 1L;
    Long merchantUserId = 42L;
    when(storeVerificationPort.findStoreOwnerId(storeId)).thenReturn(Optional.of(merchantUserId));

    LoyaltyGift existing =
        new LoyaltyGift(
            giftId,
            storeId,
            "Old Name",
            "Old Desc",
            50,
            10,
            "http://minio:9000/product-images/old.png",
            true,
            null,
            null);
    when(persistencePort.findByIdAndStoreId(giftId, storeId)).thenReturn(Optional.of(existing));
    when(persistencePort.save(any(LoyaltyGift.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    byte[] newImg = "new-img".getBytes(StandardCharsets.UTF_8);
    ByteArrayInputStream stream = new ByteArrayInputStream(newImg);
    when(giftImageStoragePort.storeImage(any(), any(Long.class), any(), any()))
        .thenReturn("http://minio:9000/product-images/new.png");

    GiftUpdateCommand command = new GiftUpdateCommand("New Name", "New Desc", 60, null);

    LoyaltyGift updated =
        service.updateGift(
            storeId,
            giftId,
            command,
            stream,
            newImg.length,
            "new.png",
            "image/png",
            merchantUserId,
            "MERCHANT");

    assertThat(updated.getImageUrl()).isEqualTo("http://minio:9000/product-images/new.png");
    verify(giftImageStoragePort).deleteImage("http://minio:9000/product-images/old.png");
    verify(giftImageStoragePort).storeImage(any(), any(Long.class), any(), any());
  }

  @Test
  @DisplayName("Should retrieve binary image resource for existing gift")
  void shouldGetGiftImageSuccessfully() {
    Long storeId = 10L;
    Long giftId = 1L;
    LoyaltyGift existing =
        new LoyaltyGift(
            giftId,
            storeId,
            "Mug",
            "Desc",
            50,
            10,
            "http://minio:9000/product-images/mug.png",
            true,
            null,
            null);
    when(persistencePort.findByIdAndStoreId(giftId, storeId)).thenReturn(Optional.of(existing));

    byte[] imgBytes = "bytes".getBytes(StandardCharsets.UTF_8);
    when(giftImageStoragePort.downloadImage("http://minio:9000/product-images/mug.png"))
        .thenReturn(Optional.of(new GiftImageResource(imgBytes, "image/png")));

    Optional<GiftImageResource> resource = service.getGiftImage(storeId, giftId);

    assertThat(resource).isPresent();
    assertThat(resource.get().bytes()).isEqualTo(imgBytes);
  }

  @Test
  @DisplayName("Should return empty when gift has no configured image URL")
  void shouldReturnEmptyWhenGiftHasNoImageUrl() {
    Long storeId = 10L;
    Long giftId = 1L;
    LoyaltyGift existing =
        new LoyaltyGift(giftId, storeId, "Mug", "Desc", 50, 10, null, true, null, null);
    when(persistencePort.findByIdAndStoreId(giftId, storeId)).thenReturn(Optional.of(existing));

    Optional<GiftImageResource> resource = service.getGiftImage(storeId, giftId);

    assertThat(resource).isEmpty();
  }

  @Test
  @DisplayName("Should create gift and upload image to S3 when binary stream is provided")
  void shouldCreateGiftWithImageUpload() {
    Long storeId = 10L;
    Long merchantUserId = 42L;
    when(storeVerificationPort.findStoreOwnerId(storeId)).thenReturn(Optional.of(merchantUserId));

    byte[] img = "fake-img".getBytes(StandardCharsets.UTF_8);
    ByteArrayInputStream stream = new ByteArrayInputStream(img);
    when(giftImageStoragePort.storeImage(any(), any(Long.class), any(), any()))
        .thenReturn("http://minio:9000/product-images/uploaded.png");

    GiftCreateCommand command =
        new GiftCreateCommand("Mug ShopLoc", "Mug céramique", 75, 20, null);

    when(persistencePort.save(any(LoyaltyGift.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    LoyaltyGift created =
        service.createGift(
            storeId, command, stream, img.length, "mug.png", "image/png", merchantUserId, "MERCHANT");

    assertThat(created.getImageUrl()).isEqualTo("http://minio:9000/product-images/uploaded.png");
    verify(giftImageStoragePort).storeImage(any(), any(Long.class), any(), any());
    verify(persistencePort).save(any(LoyaltyGift.class));
  }

  @Test
  @DisplayName("Should update gift and replace old image on S3 when new binary stream is provided")
  void shouldUpdateGiftReplacingOldImage() {
    Long storeId = 10L;
    Long giftId = 1L;
    Long merchantUserId = 42L;
    when(storeVerificationPort.findStoreOwnerId(storeId)).thenReturn(Optional.of(merchantUserId));

    LoyaltyGift existing =
        new LoyaltyGift(
            giftId, storeId, "Old Name", "Old Desc", 50, 10, "http://minio:9000/product-images/old.png", true, null, null);
    when(persistencePort.findByIdAndStoreId(giftId, storeId)).thenReturn(Optional.of(existing));
    when(persistencePort.save(any(LoyaltyGift.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    byte[] newImg = "new-img".getBytes(StandardCharsets.UTF_8);
    ByteArrayInputStream stream = new ByteArrayInputStream(newImg);
    when(giftImageStoragePort.storeImage(any(), any(Long.class), any(), any()))
        .thenReturn("http://minio:9000/product-images/new.png");

    GiftUpdateCommand command = new GiftUpdateCommand("New Name", "New Desc", 60, null);

    LoyaltyGift updated =
        service.updateGift(
            storeId, giftId, command, stream, newImg.length, "new.png", "image/png", merchantUserId, "MERCHANT");

    assertThat(updated.getImageUrl()).isEqualTo("http://minio:9000/product-images/new.png");
    verify(giftImageStoragePort).deleteImage("http://minio:9000/product-images/old.png");
    verify(giftImageStoragePort).storeImage(any(), any(Long.class), any(), any());
  }

  @Test
  @DisplayName("Should retrieve binary image resource for existing gift")
  void shouldGetGiftImageSuccessfully() {
    Long storeId = 10L;
    Long giftId = 1L;
    LoyaltyGift existing =
        new LoyaltyGift(
            giftId, storeId, "Mug", "Desc", 50, 10, "http://minio:9000/product-images/mug.png", true, null, null);
    when(persistencePort.findByIdAndStoreId(giftId, storeId)).thenReturn(Optional.of(existing));

    byte[] imgBytes = "bytes".getBytes(StandardCharsets.UTF_8);
    when(giftImageStoragePort.downloadImage("http://minio:9000/product-images/mug.png"))
        .thenReturn(Optional.of(new GiftImageResource(imgBytes, "image/png")));

    Optional<GiftImageResource> resource = service.getGiftImage(storeId, giftId);

    assertThat(resource).isPresent();
    assertThat(resource.get().bytes()).isEqualTo(imgBytes);
  }

  @Test
  @DisplayName("Should return empty when gift has no configured image URL")
  void shouldReturnEmptyWhenGiftHasNoImageUrl() {
    Long storeId = 10L;
    Long giftId = 1L;
    LoyaltyGift existing =
        new LoyaltyGift(giftId, storeId, "Mug", "Desc", 50, 10, null, true, null, null);
    when(persistencePort.findByIdAndStoreId(giftId, storeId)).thenReturn(Optional.of(existing));

    Optional<GiftImageResource> resource = service.getGiftImage(storeId, giftId);

    assertThat(resource).isEmpty();
  }
}
