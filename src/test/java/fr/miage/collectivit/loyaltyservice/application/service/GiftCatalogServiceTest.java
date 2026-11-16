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
import fr.miage.collectivit.loyaltyservice.domain.model.LoyaltyGift;
import fr.miage.collectivit.loyaltyservice.domain.port.in.GiftCreateCommand;
import fr.miage.collectivit.loyaltyservice.domain.port.in.GiftUpdateCommand;
import fr.miage.collectivit.loyaltyservice.domain.port.out.LoyaltyGiftPersistencePort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.StoreVerificationPort;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Application Service - GiftCatalogService Unit Tests (SLE-60 / SLE-192, SLE-193)")
class GiftCatalogServiceTest {

  private LoyaltyGiftPersistencePort persistencePort;
  private StoreVerificationPort storeVerificationPort;
  private GiftCatalogService service;

  @BeforeEach
  void setUp() {
    persistencePort = mock(LoyaltyGiftPersistencePort.class);
    storeVerificationPort = mock(StoreVerificationPort.class);
    service = new GiftCatalogService(persistencePort, storeVerificationPort);
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
}
