package fr.miage.collectivit.loyaltyservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.miage.collectivit.loyaltyservice.domain.exception.DuplicatePhysicalRedemptionException;
import fr.miage.collectivit.loyaltyservice.domain.exception.GiftNotFoundException;
import fr.miage.collectivit.loyaltyservice.domain.exception.InsufficientLoyaltyPointsException;
import fr.miage.collectivit.loyaltyservice.domain.exception.InvalidGiftException;
import fr.miage.collectivit.loyaltyservice.domain.exception.StoreAccessDeniedException;
import fr.miage.collectivit.loyaltyservice.domain.model.LoyaltyGift;
import fr.miage.collectivit.loyaltyservice.domain.model.PhysicalGiftRedemptionResult;
import fr.miage.collectivit.loyaltyservice.domain.model.PointsTransaction;
import fr.miage.collectivit.loyaltyservice.domain.model.PointsTransactionType;
import fr.miage.collectivit.loyaltyservice.domain.model.UserPointsAccount;
import fr.miage.collectivit.loyaltyservice.domain.port.in.RedeemPhysicalGiftsCommand;
import fr.miage.collectivit.loyaltyservice.domain.port.in.RedeemPhysicalGiftsCommand.GiftSelectionItem;
import fr.miage.collectivit.loyaltyservice.domain.port.out.LoyaltyGiftPersistencePort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.StoreVerificationPort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.UserPointsPersistencePort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test - PhysicalGiftRedemptionService (SLE-43)")
class PhysicalGiftRedemptionServiceTest {

  private static final UUID STORE_UID = UUID.fromString("9b6ae7d0-7fa9-4f15-a47d-56d809984cba");
  private static final UUID OWNER_UID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final Instant FIXED_NOW = Instant.parse("2026-11-24T20:00:00Z");

  @Mock private LoyaltyGiftPersistencePort loyaltyGiftPersistencePort;
  @Mock private UserPointsPersistencePort userPointsPersistencePort;
  @Mock private StoreVerificationPort storeVerificationPort;

  private PhysicalGiftRedemptionService service;

  @BeforeEach
  void setUp() {
    service =
        new PhysicalGiftRedemptionService(
            loyaltyGiftPersistencePort,
            userPointsPersistencePort,
            storeVerificationPort,
            null,
            Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    lenient()
        .when(storeVerificationPort.findStoreOwnerUid(STORE_UID))
        .thenReturn(Optional.of(OWNER_UID));
    lenient()
        .when(storeVerificationPort.findStoreName(STORE_UID))
        .thenReturn(Optional.of("Burger King Lille"));
  }

  @Test
  @DisplayName("Should redeem multiple gifts with quantities without checking stock")
  void shouldRedeemMultipleGiftsIgnoringStock() {
    UUID gift1Uid = UUID.randomUUID();
    UUID gift2Uid = UUID.randomUUID();
    // Note: stock is 0 for gift1, and it must still succeed for physical redemption.
    LoyaltyGift gift1 =
        new LoyaltyGift(
            1L, gift1Uid, STORE_UID, "Mug ShopLoc", null, 30, 0, null, true, FIXED_NOW, FIXED_NOW);
    LoyaltyGift gift2 =
        new LoyaltyGift(
            2L, gift2Uid, STORE_UID, "Tote Bag", null, 50, 2, null, true, FIXED_NOW, FIXED_NOW);

    when(loyaltyGiftPersistencePort.findByUidAndStoreUid(gift1Uid, STORE_UID))
        .thenReturn(Optional.of(gift1));
    when(loyaltyGiftPersistencePort.findByUidAndStoreUid(gift2Uid, STORE_UID))
        .thenReturn(Optional.of(gift2));

    UserPointsAccount existing =
        new UserPointsAccount(
            10L, "cust-uuid-1", "CL95001", new BigDecimal("150.0"), FIXED_NOW, FIXED_NOW);
    when(userPointsPersistencePort.findAccountByUserIdForUpdate("cust-uuid-1"))
        .thenReturn(Optional.of(existing));
    when(userPointsPersistencePort.saveAccount(any())).thenAnswer(inv -> inv.getArgument(0));
    when(userPointsPersistencePort.saveTransaction(eq(10L), any()))
        .thenAnswer(inv -> inv.getArgument(1));

    RedeemPhysicalGiftsCommand command =
        new RedeemPhysicalGiftsCommand(
            "cust-uuid-1",
            "CL95001",
            List.of(new GiftSelectionItem(gift1Uid, 2), new GiftSelectionItem(gift2Uid, 1)));

    PhysicalGiftRedemptionResult result =
        service.redeemPhysicalGifts(STORE_UID, command, OWNER_UID, "MERCHANT");

    assertThat(result.pointsDeducted()).isEqualByComparingTo("110.0");
    assertThat(result.remainingBalance()).isEqualByComparingTo("40.0");
    assertThat(result.storeName()).isEqualTo("Burger King Lille");
    assertThat(result.items()).hasSize(2);
    assertThat(result.transactionReference()).startsWith("GIFT-PHYS-");

    ArgumentCaptor<PointsTransaction> txCaptor = ArgumentCaptor.forClass(PointsTransaction.class);
    verify(userPointsPersistencePort).saveTransaction(eq(10L), txCaptor.capture());
    PointsTransaction savedTx = txCaptor.getValue();
    assertThat(savedTx.type()).isEqualTo(PointsTransactionType.REDEEM);
    assertThat(savedTx.sourceType()).isEqualTo("GIFT_REDEEM_PHYSICAL");
    assertThat(savedTx.points()).isEqualByComparingTo("110.0");
    assertThat(savedTx.balanceAfter()).isEqualByComparingTo("40.0");
    assertThat(savedTx.description()).contains("2x Mug ShopLoc", "1x Tote Bag");

    verify(loyaltyGiftPersistencePort, never()).save(any());
  }

  @Test
  @DisplayName("Should reject when customer balance is insufficient")
  void shouldRejectWhenInsufficientPoints() {
    UUID giftUid = UUID.randomUUID();
    LoyaltyGift gift =
        new LoyaltyGift(
            1L, giftUid, STORE_UID, "Casquette", null, 100, 5, null, true, FIXED_NOW, FIXED_NOW);
    when(loyaltyGiftPersistencePort.findByUidAndStoreUid(giftUid, STORE_UID))
        .thenReturn(Optional.of(gift));

    UserPointsAccount existing =
        new UserPointsAccount(
            10L, "cust-uuid-1", "CL95001", new BigDecimal("80.0"), FIXED_NOW, FIXED_NOW);
    when(userPointsPersistencePort.findAccountByUserIdForUpdate("cust-uuid-1"))
        .thenReturn(Optional.of(existing));

    RedeemPhysicalGiftsCommand command =
        new RedeemPhysicalGiftsCommand(
            "cust-uuid-1", "CL95001", List.of(new GiftSelectionItem(giftUid, 1)));

    assertThatThrownBy(() -> service.redeemPhysicalGifts(STORE_UID, command, OWNER_UID, "MERCHANT"))
        .isInstanceOf(InsufficientLoyaltyPointsException.class)
        .hasMessageContaining("80.0")
        .hasMessageContaining("100.0");

    verify(userPointsPersistencePort, never()).saveAccount(any());
    verify(userPointsPersistencePort, never()).saveTransaction(any(), any());
  }

  @Test
  @DisplayName("Should reject when customer has no points account")
  void shouldRejectWhenAccountMissing() {
    UUID giftUid = UUID.randomUUID();
    LoyaltyGift gift =
        new LoyaltyGift(
            1L, giftUid, STORE_UID, "Casquette", null, 50, 5, null, true, FIXED_NOW, FIXED_NOW);
    when(loyaltyGiftPersistencePort.findByUidAndStoreUid(giftUid, STORE_UID))
        .thenReturn(Optional.of(gift));
    when(userPointsPersistencePort.findAccountByClientCodeForUpdate("CL95001"))
        .thenReturn(Optional.empty());

    RedeemPhysicalGiftsCommand command =
        new RedeemPhysicalGiftsCommand(null, "CL95001", List.of(new GiftSelectionItem(giftUid, 1)));

    assertThatThrownBy(() -> service.redeemPhysicalGifts(STORE_UID, command, OWNER_UID, "MERCHANT"))
        .isInstanceOf(InsufficientLoyaltyPointsException.class)
        .hasMessageContaining("0.0");
  }

  @Test
  @DisplayName("Should reject inactive gift")
  void shouldRejectInactiveGift() {
    UUID giftUid = UUID.randomUUID();
    LoyaltyGift inactiveGift =
        new LoyaltyGift(
            1L, giftUid, STORE_UID, "Ancien lot", null, 20, 5, null, false, FIXED_NOW, FIXED_NOW);
    when(loyaltyGiftPersistencePort.findByUidAndStoreUid(giftUid, STORE_UID))
        .thenReturn(Optional.of(inactiveGift));

    RedeemPhysicalGiftsCommand command =
        new RedeemPhysicalGiftsCommand(
            "cust-uuid-1", "CL95001", List.of(new GiftSelectionItem(giftUid, 1)));

    assertThatThrownBy(() -> service.redeemPhysicalGifts(STORE_UID, command, OWNER_UID, "MERCHANT"))
        .isInstanceOf(InvalidGiftException.class);
  }

  @Test
  @DisplayName("Should reject missing gift")
  void shouldRejectMissingGift() {
    UUID giftUid = UUID.randomUUID();
    when(loyaltyGiftPersistencePort.findByUidAndStoreUid(giftUid, STORE_UID))
        .thenReturn(Optional.empty());

    RedeemPhysicalGiftsCommand command =
        new RedeemPhysicalGiftsCommand(
            "cust-uuid-1", "CL95001", List.of(new GiftSelectionItem(giftUid, 1)));

    assertThatThrownBy(() -> service.redeemPhysicalGifts(STORE_UID, command, OWNER_UID, "MERCHANT"))
        .isInstanceOf(GiftNotFoundException.class);
  }

  @Test
  @DisplayName("Should reject non-owner merchant")
  void shouldRejectNonOwnerMerchant() {
    UUID otherMerchant = UUID.randomUUID();
    RedeemPhysicalGiftsCommand command =
        new RedeemPhysicalGiftsCommand(
            "cust-uuid-1", "CL95001", List.of(new GiftSelectionItem(UUID.randomUUID(), 1)));

    assertThatThrownBy(
            () -> service.redeemPhysicalGifts(STORE_UID, command, otherMerchant, "MERCHANT"))
        .isInstanceOf(StoreAccessDeniedException.class);
  }

  @Test
  @DisplayName("Should reject mismatched clientCode for identified account")
  void shouldRejectMismatchedClientCode() {
    UUID giftUid = UUID.randomUUID();
    LoyaltyGift gift =
        new LoyaltyGift(
            1L, giftUid, STORE_UID, "Mug", null, 20, 5, null, true, FIXED_NOW, FIXED_NOW);
    when(loyaltyGiftPersistencePort.findByUidAndStoreUid(giftUid, STORE_UID))
        .thenReturn(Optional.of(gift));

    UserPointsAccount existing =
        new UserPointsAccount(
            10L, "cust-uuid-1", "CL95001", new BigDecimal("100.0"), FIXED_NOW, FIXED_NOW);
    when(userPointsPersistencePort.findAccountByUserIdForUpdate("cust-uuid-1"))
        .thenReturn(Optional.of(existing));

    RedeemPhysicalGiftsCommand command =
        new RedeemPhysicalGiftsCommand(
            "cust-uuid-1", "CL99999", List.of(new GiftSelectionItem(giftUid, 1)));

    assertThatThrownBy(() -> service.redeemPhysicalGifts(STORE_UID, command, OWNER_UID, "MERCHANT"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("code fidélité");

    verify(userPointsPersistencePort, never()).saveAccount(any());
  }

  @Test
  @DisplayName("Should reject duplicate idempotencyKey with DuplicatePhysicalRedemptionException")
  void shouldRejectDuplicateIdempotencyKey() {
    UUID giftUid = UUID.randomUUID();
    LoyaltyGift gift =
        new LoyaltyGift(
            1L, giftUid, STORE_UID, "Mug", null, 20, 5, null, true, FIXED_NOW, FIXED_NOW);
    when(loyaltyGiftPersistencePort.findByUidAndStoreUid(giftUid, STORE_UID))
        .thenReturn(Optional.of(gift));

    UserPointsAccount existing =
        new UserPointsAccount(
            10L, "cust-uuid-1", "CL95001", new BigDecimal("100.0"), FIXED_NOW, FIXED_NOW);
    when(userPointsPersistencePort.findAccountByUserIdForUpdate("cust-uuid-1"))
        .thenReturn(Optional.of(existing));
    when(userPointsPersistencePort.existsTransactionByReference("GIFT-PHYS-REQ-123"))
        .thenReturn(true);

    RedeemPhysicalGiftsCommand command =
        new RedeemPhysicalGiftsCommand(
            "cust-uuid-1", "CL95001", List.of(new GiftSelectionItem(giftUid, 1)), "req-123");

    assertThatThrownBy(() -> service.redeemPhysicalGifts(STORE_UID, command, OWNER_UID, "MERCHANT"))
        .isInstanceOf(DuplicatePhysicalRedemptionException.class)
        .hasMessageContaining("GIFT-PHYS-REQ-123");

    verify(userPointsPersistencePort, never()).saveAccount(any());
  }
}
