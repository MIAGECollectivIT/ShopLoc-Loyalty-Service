package fr.miage.collectivit.loyaltyservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.miage.collectivit.loyaltyservice.domain.exception.InsufficientLoyaltyPointsException;
import fr.miage.collectivit.loyaltyservice.domain.exception.PriorPurchaseRequiredException;
import fr.miage.collectivit.loyaltyservice.domain.model.LoyaltyGift;
import fr.miage.collectivit.loyaltyservice.domain.model.UserPointsAccount;
import fr.miage.collectivit.loyaltyservice.domain.port.out.LoyaltyGiftPersistencePort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.SalesTransactionPort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.UserPointsPersistencePort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.UserVfpPersistencePort;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.CreateLoyaltyHoldRequest;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.CreateLoyaltyHoldRequest.HoldItemRequest;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.LoyaltyHoldResponse;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.LoyaltyPointsHoldItemJpaEntity;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.LoyaltyPointsHoldJpaEntity;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.LoyaltyPointsHoldJpaEntity.HoldStatus;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository.SpringDataLoyaltyPointsHoldRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoyaltyPointsHoldServiceTest {

  @Mock private LoyaltyGiftPersistencePort loyaltyGiftPersistencePort;
  @Mock private UserPointsPersistencePort userPointsPersistencePort;
  @Mock private UserVfpPersistencePort userVfpPersistencePort;
  @Mock private SalesTransactionPort salesTransactionPort;
  @Mock private SpringDataLoyaltyPointsHoldRepository holdRepository;

  private LoyaltyPointsHoldService service;

  private final UUID storeUid = UUID.randomUUID();
  private final UUID giftUid = UUID.randomUUID();
  private final String userId = "user-123";

  @BeforeEach
  void setUp() {
    service =
        new LoyaltyPointsHoldService(
            loyaltyGiftPersistencePort,
            userPointsPersistencePort,
            userVfpPersistencePort,
            salesTransactionPort,
            holdRepository);
  }

  @Test
  void createHoldSucceedsWithSalesFallbackAndDecrementsStock() {
    LoyaltyGift gift =
        new LoyaltyGift(
            1L,
            giftUid,
            storeUid,
            "Mug",
            "Desc",
            100,
            5,
            null,
            true,
            false,
            Instant.now(),
            Instant.now());
    UserPointsAccount account =
        new UserPointsAccount(
            1L, userId, "CLI-1", new BigDecimal("250.0"), Instant.now(), Instant.now());

    when(holdRepository.findByOrderReference("CMD-1")).thenReturn(Optional.empty());
    when(userPointsPersistencePort.hasPriorPurchaseInStore(userId, storeUid)).thenReturn(false);
    when(salesTransactionPort.hasCustomerPurchasedInStore(userId, storeUid)).thenReturn(true);
    when(userPointsPersistencePort.findAccountByUserIdForUpdate(userId))
        .thenReturn(Optional.of(account));
    when(holdRepository.sumTotalPointsByUserIdAndStatus(userId, HoldStatus.ACTIVE))
        .thenReturn(new BigDecimal("50.0"));
    when(userVfpPersistencePort.findByUserId(userId)).thenReturn(Optional.empty());
    when(loyaltyGiftPersistencePort.findByUidAndStoreUidForUpdate(giftUid, storeUid))
        .thenReturn(Optional.of(gift));
    when(holdRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    LoyaltyHoldResponse response =
        service.createHold(
            new CreateLoyaltyHoldRequest(
                "CMD-1",
                userId,
                Instant.now().plusSeconds(1800),
                List.of(new HoldItemRequest(storeUid, giftUid, 2))));

    assertThat(response.totalPoints()).isEqualByComparingTo("200.0");
    assertThat(gift.getStock()).isEqualTo(3);
    verify(loyaltyGiftPersistencePort).save(gift);
  }

  @Test
  void createHoldThrowsWhenNoPriorPurchaseAnywhere() {
    when(holdRepository.findByOrderReference("CMD-2")).thenReturn(Optional.empty());
    when(userPointsPersistencePort.hasPriorPurchaseInStore(userId, storeUid)).thenReturn(false);
    when(salesTransactionPort.hasCustomerPurchasedInStore(userId, storeUid)).thenReturn(false);

    assertThatThrownBy(
            () ->
                service.createHold(
                    new CreateLoyaltyHoldRequest(
                        "CMD-2",
                        userId,
                        Instant.now().plusSeconds(1800),
                        List.of(new HoldItemRequest(storeUid, giftUid, 1)))))
        .isInstanceOf(PriorPurchaseRequiredException.class);

    verify(holdRepository, never()).save(any());
  }

  @Test
  void createHoldThrowsWhenActiveHoldsReduceAvailablePoints() {
    LoyaltyGift gift =
        new LoyaltyGift(
            1L,
            giftUid,
            storeUid,
            "Mug",
            "Desc",
            100,
            5,
            null,
            true,
            false,
            Instant.now(),
            Instant.now());
    UserPointsAccount account =
        new UserPointsAccount(
            1L, userId, "CLI-1", new BigDecimal("150.0"), Instant.now(), Instant.now());

    when(holdRepository.findByOrderReference("CMD-3")).thenReturn(Optional.empty());
    when(userPointsPersistencePort.hasPriorPurchaseInStore(userId, storeUid)).thenReturn(true);
    when(userPointsPersistencePort.findAccountByUserIdForUpdate(userId))
        .thenReturn(Optional.of(account));
    when(holdRepository.sumTotalPointsByUserIdAndStatus(userId, HoldStatus.ACTIVE))
        .thenReturn(new BigDecimal("100.0"));
    when(userVfpPersistencePort.findByUserId(userId)).thenReturn(Optional.empty());
    when(loyaltyGiftPersistencePort.findByUidAndStoreUidForUpdate(giftUid, storeUid))
        .thenReturn(Optional.of(gift));

    assertThatThrownBy(
            () ->
                service.createHold(
                    new CreateLoyaltyHoldRequest(
                        "CMD-3",
                        userId,
                        Instant.now().plusSeconds(1800),
                        List.of(new HoldItemRequest(storeUid, giftUid, 1)))))
        .isInstanceOf(InsufficientLoyaltyPointsException.class);
  }

  @Test
  void releaseHoldRestoresGiftStockAndMarksReleased() {
    LoyaltyGift gift =
        new LoyaltyGift(
            1L,
            giftUid,
            storeUid,
            "Mug",
            "Desc",
            100,
            3,
            null,
            true,
            false,
            Instant.now(),
            Instant.now());
    LoyaltyPointsHoldJpaEntity hold =
        LoyaltyPointsHoldJpaEntity.builder()
            .orderReference("CMD-4")
            .userId(userId)
            .totalPoints(new BigDecimal("200.0"))
            .status(HoldStatus.ACTIVE)
            .expiresAt(Instant.now().plusSeconds(1800))
            .items(
                List.of(
                    LoyaltyPointsHoldItemJpaEntity.builder()
                        .storeUid(storeUid)
                        .giftUid(giftUid)
                        .giftName("Mug")
                        .unitPointsCost(100)
                        .quantity(2)
                        .build()))
            .build();

    when(holdRepository.findByOrderReferenceForUpdate("CMD-4")).thenReturn(Optional.of(hold));
    when(loyaltyGiftPersistencePort.findByUidAndStoreUidForUpdate(giftUid, storeUid))
        .thenReturn(Optional.of(gift));

    service.releaseHold("CMD-4");

    assertThat(gift.getStock()).isEqualTo(5);
    assertThat(hold.getStatus()).isEqualTo(HoldStatus.RELEASED);
    verify(holdRepository).save(hold);
  }
}
