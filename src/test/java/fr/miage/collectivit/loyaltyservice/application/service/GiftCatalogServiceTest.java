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
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Application Service - GiftCatalogService Unit Tests (SLE-60 / SLE-192, SLE-193)")
class GiftCatalogServiceTest {

  private static final UUID STORE_UID = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
  private static final UUID STORE_UID_2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440020");
  private static final UUID UNKNOWN_STORE_UID =
      UUID.fromString("550e8400-e29b-41d4-a716-446655440999");
  private static final UUID GIFT_UID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
  private static final UUID GIFT_UID_2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
  private static final UUID UNKNOWN_GIFT_UID =
      UUID.fromString("550e8400-e29b-41d4-a716-446655440998");
  private static final UUID MERCHANT_USER_ID =
      UUID.fromString("550e8400-e29b-41d4-a716-446655440042");
  private static final UUID OTHER_MERCHANT_ID =
      UUID.fromString("550e8400-e29b-41d4-a716-446655440099");
  private static final UUID ADMIN_USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440900");

  private LoyaltyGiftPersistencePort persistencePort;
  private StoreVerificationPort storeVerificationPort;
  private GiftImageStoragePort giftImageStoragePort;
  private GiftCatalogService service;

  @BeforeEach
  void setUp() {
    persistencePort = mock(LoyaltyGiftPersistencePort.class);
    storeVerificationPort = mock(StoreVerificationPort.class);
    giftImageStoragePort = mock(GiftImageStoragePort.class);
    service =
        new GiftCatalogService(
            persistencePort,
            storeVerificationPort,
            giftImageStoragePort,
            new org.springframework.transaction.support.TransactionTemplate(
                mock(org.springframework.transaction.PlatformTransactionManager.class)));
  }

  @Test
  @DisplayName("Should successfully create a loyalty gift when caller is store owner")
  void shouldCreateGiftWhenCallerIsStoreOwner() {
    when(storeVerificationPort.findStoreOwnerUid(STORE_UID))
        .thenReturn(Optional.of(MERCHANT_USER_ID));

    GiftCreateCommand command =
        new GiftCreateCommand(
            "Mug ShopLoc", "Mug céramique", 75, 20, "https://cdn.shoploc.fr/mug.png");

    when(persistencePort.save(any(LoyaltyGift.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    LoyaltyGift created = service.createGift(STORE_UID, command, MERCHANT_USER_ID, "MERCHANT");

    assertThat(created).isNotNull();
    assertThat(created.getStoreUid()).isEqualTo(STORE_UID);
    assertThat(created.getName()).isEqualTo("Mug ShopLoc");
    assertThat(created.getPointsCost()).isEqualTo(75);
    assertThat(created.getStock()).isEqualTo(20);
    assertThat(created.isActive()).isTrue();
    verify(persistencePort).save(any(LoyaltyGift.class));
  }

  @Test
  @DisplayName("Should allow ADMIN to create gift without checking owner id")
  void shouldAllowAdminToCreateGift() {
    GiftCreateCommand command = new GiftCreateCommand("Mug ShopLoc", "Mug céramique", 75, 20, null);

    when(persistencePort.save(any(LoyaltyGift.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    LoyaltyGift created = service.createGift(STORE_UID, command, ADMIN_USER_ID, "ADMIN");

    assertThat(created.getName()).isEqualTo("Mug ShopLoc");
  }

  @Test
  @DisplayName("Should reject gift creation when caller is not store owner")
  void shouldRejectCreationWhenNotOwner() {
    when(storeVerificationPort.findStoreOwnerUid(STORE_UID))
        .thenReturn(Optional.of(MERCHANT_USER_ID));

    GiftCreateCommand command = new GiftCreateCommand("Mug ShopLoc", "Mug céramique", 75, 20, null);

    assertThatThrownBy(() -> service.createGift(STORE_UID, command, OTHER_MERCHANT_ID, "MERCHANT"))
        .isInstanceOf(StoreAccessDeniedException.class)
        .hasMessageContaining("you are not the owner of store " + STORE_UID);
  }

  @Test
  @DisplayName("Should reject gift creation when store does not exist")
  void shouldRejectCreationWhenStoreNotFound() {
    when(storeVerificationPort.findStoreOwnerUid(UNKNOWN_STORE_UID)).thenReturn(Optional.empty());

    GiftCreateCommand command = new GiftCreateCommand("Mug ShopLoc", "Mug céramique", 75, 20, null);

    assertThatThrownBy(
            () -> service.createGift(UNKNOWN_STORE_UID, command, MERCHANT_USER_ID, "MERCHANT"))
        .isInstanceOf(StoreNotFoundException.class);
  }

  @Test
  @DisplayName("Should successfully update gift details")
  void shouldUpdateGiftDetails() {
    when(storeVerificationPort.findStoreOwnerUid(STORE_UID))
        .thenReturn(Optional.of(MERCHANT_USER_ID));

    LoyaltyGift existing =
        new LoyaltyGift(
            1L, GIFT_UID, STORE_UID, "Ancien Nom", "Ancienne desc", 50, 10, null, true, null, null);
    when(persistencePort.findByUidAndStoreUidForUpdate(GIFT_UID, STORE_UID))
        .thenReturn(Optional.of(existing));
    when(persistencePort.save(any(LoyaltyGift.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    GiftUpdateCommand updateCommand =
        new GiftUpdateCommand(
            "Nouveau Nom", "Nouvelle desc", 100, "https://cdn.shoploc.fr/img.png");

    LoyaltyGift updated =
        service.updateGift(STORE_UID, GIFT_UID, updateCommand, MERCHANT_USER_ID, "MERCHANT");

    assertThat(updated.getName()).isEqualTo("Nouveau Nom");
    assertThat(updated.getDescription()).isEqualTo("Nouvelle desc");
    assertThat(updated.getPointsCost()).isEqualTo(100);
    assertThat(updated.getImageUrl()).isEqualTo("https://cdn.shoploc.fr/img.png");
  }

  @Test
  @DisplayName("Should adjust gift stock dedicated inventory (SLE-193)")
  void shouldAdjustGiftStock() {
    when(storeVerificationPort.findStoreOwnerUid(STORE_UID))
        .thenReturn(Optional.of(MERCHANT_USER_ID));

    LoyaltyGift existing =
        new LoyaltyGift(1L, GIFT_UID, STORE_UID, "Mug", "Desc", 50, 10, null, true, null, null);
    when(persistencePort.findByUidAndStoreUidForUpdate(GIFT_UID, STORE_UID))
        .thenReturn(Optional.of(existing));
    when(persistencePort.save(any(LoyaltyGift.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    LoyaltyGift result = service.adjustStock(STORE_UID, GIFT_UID, 15, MERCHANT_USER_ID, "MERCHANT");

    assertThat(result.getStock()).isEqualTo(25);
  }

  @Test
  @DisplayName("Should delete/deactivate a gift from the catalog")
  void shouldDeleteGift() {
    when(storeVerificationPort.findStoreOwnerUid(STORE_UID))
        .thenReturn(Optional.of(MERCHANT_USER_ID));

    LoyaltyGift existing =
        new LoyaltyGift(1L, GIFT_UID, STORE_UID, "Mug", "Desc", 50, 10, null, true, null, null);
    when(persistencePort.findByUidAndStoreUidForUpdate(GIFT_UID, STORE_UID))
        .thenReturn(Optional.of(existing));

    service.deleteGift(STORE_UID, GIFT_UID, MERCHANT_USER_ID, "MERCHANT");

    verify(persistencePort).deleteByUid(GIFT_UID);
  }

  @Test
  @DisplayName("Should retrieve active gifts or all gifts for a store")
  void shouldRetrieveGifts() {
    LoyaltyGift active =
        new LoyaltyGift(1L, GIFT_UID, STORE_UID, "Mug", "Desc", 50, 10, null, true, null, null);
    LoyaltyGift inactive =
        new LoyaltyGift(2L, GIFT_UID_2, STORE_UID, "Stylo", "Desc", 20, 0, null, false, null, null);

    when(persistencePort.findByStoreUid(STORE_UID)).thenReturn(List.of(active, inactive));
    when(persistencePort.findActiveByStoreUid(STORE_UID)).thenReturn(List.of(active));

    List<LoyaltyGift> all = service.getGiftsByStore(STORE_UID, true);
    assertThat(all).hasSize(2);

    List<LoyaltyGift> onlyActive = service.getGiftsByStore(STORE_UID, false);
    assertThat(onlyActive).hasSize(1);
    assertThat(onlyActive.get(0).getName()).isEqualTo("Mug");
  }

  @Test
  @DisplayName("Should throw GiftNotFoundException when giftUid does not exist in store")
  void shouldThrowWhenGiftNotFound() {
    when(storeVerificationPort.findStoreOwnerUid(STORE_UID))
        .thenReturn(Optional.of(MERCHANT_USER_ID));
    when(persistencePort.findByUidAndStoreUid(UNKNOWN_GIFT_UID, STORE_UID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getGiftByUid(STORE_UID, UNKNOWN_GIFT_UID))
        .isInstanceOf(GiftNotFoundException.class);
  }

  @Test
  @DisplayName("Should create gift and upload image to S3 when binary stream is provided")
  void shouldCreateGiftWithImageUpload() {
    when(storeVerificationPort.findStoreOwnerUid(STORE_UID))
        .thenReturn(Optional.of(MERCHANT_USER_ID));

    byte[] img = "fake-img".getBytes(StandardCharsets.UTF_8);
    ByteArrayInputStream stream = new ByteArrayInputStream(img);
    when(giftImageStoragePort.storeImage(any(), any(Long.class), any(), any()))
        .thenReturn("http://minio:9000/product-images/uploaded.png");

    GiftCreateCommand command = new GiftCreateCommand("Mug ShopLoc", "Mug céramique", 75, 20, null);

    when(persistencePort.save(any(LoyaltyGift.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    LoyaltyGift created =
        service.createGift(
            STORE_UID,
            command,
            stream,
            img.length,
            "mug.png",
            "image/png",
            MERCHANT_USER_ID,
            "MERCHANT");

    assertThat(created.getImageUrl()).isEqualTo("http://minio:9000/product-images/uploaded.png");
    verify(giftImageStoragePort).storeImage(any(), any(Long.class), any(), any());
    verify(persistencePort).save(any(LoyaltyGift.class));
  }

  @Test
  @DisplayName("Should update gift and replace old image on S3 when new binary stream is provided")
  void shouldUpdateGiftReplacingOldImage() {
    when(storeVerificationPort.findStoreOwnerUid(STORE_UID))
        .thenReturn(Optional.of(MERCHANT_USER_ID));

    LoyaltyGift existing =
        new LoyaltyGift(
            1L,
            GIFT_UID,
            STORE_UID,
            "Old Name",
            "Old Desc",
            50,
            10,
            "http://minio:9000/product-images/old.png",
            true,
            null,
            null);
    when(persistencePort.findByUidAndStoreUidForUpdate(GIFT_UID, STORE_UID))
        .thenReturn(Optional.of(existing));
    when(persistencePort.save(any(LoyaltyGift.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    byte[] newImg = "new-img".getBytes(StandardCharsets.UTF_8);
    ByteArrayInputStream stream = new ByteArrayInputStream(newImg);
    when(giftImageStoragePort.storeImage(any(), any(Long.class), any(), any()))
        .thenReturn("http://minio:9000/product-images/new.png");

    GiftUpdateCommand command = new GiftUpdateCommand("New Name", "New Desc", 60, null);

    LoyaltyGift updated =
        service.updateGift(
            STORE_UID,
            GIFT_UID,
            command,
            stream,
            newImg.length,
            "new.png",
            "image/png",
            MERCHANT_USER_ID,
            "MERCHANT");

    assertThat(updated.getImageUrl()).isEqualTo("http://minio:9000/product-images/new.png");
    verify(giftImageStoragePort).deleteImage("http://minio:9000/product-images/old.png");
    verify(giftImageStoragePort).storeImage(any(), any(Long.class), any(), any());
  }

  @Test
  @DisplayName("Should retrieve binary image resource for existing gift")
  void shouldGetGiftImageSuccessfully() {
    LoyaltyGift existing =
        new LoyaltyGift(
            1L,
            GIFT_UID,
            STORE_UID,
            "Mug",
            "Desc",
            50,
            10,
            "http://minio:9000/product-images/mug.png",
            true,
            null,
            null);
    when(persistencePort.findByUidAndStoreUid(GIFT_UID, STORE_UID))
        .thenReturn(Optional.of(existing));

    byte[] imgBytes = "bytes".getBytes(StandardCharsets.UTF_8);
    when(giftImageStoragePort.downloadImage("http://minio:9000/product-images/mug.png"))
        .thenReturn(Optional.of(new GiftImageResource(imgBytes, "image/png")));

    Optional<GiftImageResource> resource = service.getGiftImage(STORE_UID, GIFT_UID);

    assertThat(resource).isPresent();
    assertThat(resource.get().bytes()).isEqualTo(imgBytes);
  }

  @Test
  @DisplayName("Should return empty when gift has no configured image URL")
  void shouldReturnEmptyWhenGiftHasNoImageUrl() {
    LoyaltyGift existing =
        new LoyaltyGift(1L, GIFT_UID, STORE_UID, "Mug", "Desc", 50, 10, null, true, null, null);
    when(persistencePort.findByUidAndStoreUid(GIFT_UID, STORE_UID))
        .thenReturn(Optional.of(existing));

    Optional<GiftImageResource> resource = service.getGiftImage(STORE_UID, GIFT_UID);

    assertThat(resource).isEmpty();
  }

  @Test
  @DisplayName("Should retrieve public catalog enriched with store name and category")
  void shouldRetrievePublicCatalogEnrichedWithStoreInfo() {
    LoyaltyGift gift1 =
        new LoyaltyGift(
            1L, GIFT_UID, STORE_UID, "Croissant", "Viennoiserie", 50, 20, null, true, null, null);
    LoyaltyGift gift2 =
        new LoyaltyGift(
            2L, GIFT_UID_2, STORE_UID_2, "Livre", "Roman", 100, 5, null, true, null, null);

    when(persistencePort.findAllActive()).thenReturn(List.of(gift1, gift2));
    when(storeVerificationPort.findStoreName(STORE_UID))
        .thenReturn(Optional.of("Boulangerie Paul"));
    when(storeVerificationPort.findStoreCategory(STORE_UID))
        .thenReturn(Optional.of("Alimentation"));
    when(storeVerificationPort.findStoreName(STORE_UID_2)).thenReturn(Optional.of("Librairie"));
    when(storeVerificationPort.findStoreCategory(STORE_UID_2)).thenReturn(Optional.of("Culture"));

    var items = service.getPublicCatalog(null, null);

    assertThat(items).hasSize(2);
    assertThat(items.get(0).name()).isEqualTo("Croissant");
    assertThat(items.get(0).storeName()).isEqualTo("Boulangerie Paul");
    assertThat(items.get(0).category()).isEqualTo("Alimentation");
    assertThat(items.get(1).name()).isEqualTo("Livre");
    assertThat(items.get(1).storeName()).isEqualTo("Librairie");
    assertThat(items.get(1).category()).isEqualTo("Culture");
  }

  @Test
  @DisplayName("Should filter public catalog by category case-insensitively")
  void shouldFilterPublicCatalogByCategory() {
    LoyaltyGift gift1 =
        new LoyaltyGift(
            1L, GIFT_UID, STORE_UID, "Croissant", "Viennoiserie", 50, 20, null, true, null, null);
    LoyaltyGift gift2 =
        new LoyaltyGift(
            2L, GIFT_UID_2, STORE_UID_2, "Livre", "Roman", 100, 5, null, true, null, null);

    when(persistencePort.findAllActive()).thenReturn(List.of(gift1, gift2));
    when(storeVerificationPort.findStoreName(STORE_UID))
        .thenReturn(Optional.of("Boulangerie Paul"));
    when(storeVerificationPort.findStoreCategory(STORE_UID))
        .thenReturn(Optional.of("Alimentation"));
    when(storeVerificationPort.findStoreName(STORE_UID_2)).thenReturn(Optional.of("Librairie"));
    when(storeVerificationPort.findStoreCategory(STORE_UID_2)).thenReturn(Optional.of("Culture"));

    var items = service.getPublicCatalog(null, "culture");

    assertThat(items).hasSize(1);
    assertThat(items.get(0).name()).isEqualTo("Livre");
    assertThat(items.get(0).category()).isEqualTo("Culture");
  }

  @Test
  @DisplayName("Should filter public catalog by storeUid")
  void shouldFilterPublicCatalogByStoreUid() {
    LoyaltyGift gift1 =
        new LoyaltyGift(
            1L, GIFT_UID, STORE_UID, "Croissant", "Viennoiserie", 50, 20, null, true, null, null);

    when(persistencePort.findActiveByStoreUid(STORE_UID)).thenReturn(List.of(gift1));
    when(storeVerificationPort.findStoreName(STORE_UID))
        .thenReturn(Optional.of("Boulangerie Paul"));
    when(storeVerificationPort.findStoreCategory(STORE_UID))
        .thenReturn(Optional.of("Alimentation"));

    var items = service.getPublicCatalog(STORE_UID, null);

    assertThat(items).hasSize(1);
    assertThat(items.get(0).name()).isEqualTo("Croissant");
    assertThat(items.get(0).storeUid()).isEqualTo(STORE_UID);
  }
}
