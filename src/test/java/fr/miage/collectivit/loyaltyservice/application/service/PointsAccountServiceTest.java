package fr.miage.collectivit.loyaltyservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.miage.collectivit.loyaltyservice.domain.exception.MerchantLoyaltyRateNotFoundException;
import fr.miage.collectivit.loyaltyservice.domain.exception.StoreAccessDeniedException;
import fr.miage.collectivit.loyaltyservice.domain.exception.StoreNotFoundException;
import fr.miage.collectivit.loyaltyservice.domain.model.MerchantLoyaltyRate;
import fr.miage.collectivit.loyaltyservice.domain.port.out.MerchantLoyaltyRatePersistencePort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.StoreVerificationPort;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Application Service - PointsAccountService")
class PointsAccountServiceTest {

  @Mock private MerchantLoyaltyRatePersistencePort persistencePort;
  @Mock private StoreVerificationPort storeVerificationPort;

  @InjectMocks private PointsAccountService service;

  @Test
  @DisplayName("Should create new rate in ADMIN mode without Store Service verification")
  void shouldCreateNewRateAsAdmin() {
    when(persistencePort.findByStoreId(10L)).thenReturn(Optional.empty());
    when(persistencePort.save(any(MerchantLoyaltyRate.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    MerchantLoyaltyRate result =
        service.configureRate(10L, new BigDecimal("2.00"), new BigDecimal("5.0"), 99L, "ADMIN");

    assertThat(result.getStoreId()).isEqualTo(10L);
    assertThat(result.getEuroAmount()).isEqualByComparingTo(new BigDecimal("2.00"));
    assertThat(result.getPointsReward()).isEqualByComparingTo(new BigDecimal("5.0"));
    verify(persistencePort).save(any(MerchantLoyaltyRate.class));
    verify(storeVerificationPort, never()).findStoreOwnerId(any());
  }

  @Test
  @DisplayName("Should allow store owner merchant to configure and update rate")
  void shouldAllowStoreOwnerMerchantToConfigureRate() {
    when(storeVerificationPort.findStoreOwnerId(10L)).thenReturn(Optional.of(42L));

    MerchantLoyaltyRate existing =
        new MerchantLoyaltyRate(10L, new BigDecimal("1.00"), new BigDecimal("1.0"));
    when(persistencePort.findByStoreId(10L)).thenReturn(Optional.of(existing));
    when(persistencePort.save(any(MerchantLoyaltyRate.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    MerchantLoyaltyRate result =
        service.configureRate(10L, new BigDecimal("3.00"), new BigDecimal("4.5"), 42L, "MERCHANT");

    assertThat(result.getStoreId()).isEqualTo(10L);
    assertThat(result.getEuroAmount()).isEqualByComparingTo(new BigDecimal("3.00"));
    assertThat(result.getPointsReward()).isEqualByComparingTo(new BigDecimal("4.5"));
    verify(persistencePort).save(existing);
  }

  @Test
  @DisplayName("Should reject modification when store does not exist (StoreNotFoundException)")
  void shouldThrowWhenStoreDoesNotExist() {
    when(storeVerificationPort.findStoreOwnerId(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service.configureRate(
                    999L, new BigDecimal("2.00"), new BigDecimal("2.0"), 42L, "MERCHANT"))
        .isInstanceOf(StoreNotFoundException.class)
        .hasMessageContaining("999");

    verify(persistencePort, never()).save(any());
  }

  @Test
  @DisplayName(
      "Should reject modification when merchant is not store owner (StoreAccessDeniedException)")
  void shouldThrowWhenMerchantIsNotStoreOwner() {
    when(storeVerificationPort.findStoreOwnerId(10L)).thenReturn(Optional.of(99L));

    assertThatThrownBy(
            () ->
                service.configureRate(
                    10L, new BigDecimal("2.00"), new BigDecimal("2.0"), 42L, "MERCHANT"))
        .isInstanceOf(StoreAccessDeniedException.class)
        .hasMessageContaining("owner");

    verify(persistencePort, never()).save(any());
  }

  @Test
  @DisplayName("Should reject modification when merchant user ID is null")
  void shouldThrowWhenMerchantUserIdIsNull() {
    assertThatThrownBy(
            () ->
                service.configureRate(
                    10L, new BigDecimal("2.00"), new BigDecimal("2.0"), (Long) null, "MERCHANT"))
        .isInstanceOf(StoreAccessDeniedException.class)
        .hasMessageContaining("Missing");

    verify(persistencePort, never()).save(any());
  }

  @Test
  @DisplayName("Should retrieve existing rate by storeId")
  void shouldReturnExistingRate() {
    MerchantLoyaltyRate existing =
        new MerchantLoyaltyRate(20L, new BigDecimal("1.00"), new BigDecimal("2.0"));
    when(persistencePort.findByStoreId(20L)).thenReturn(Optional.of(existing));

    MerchantLoyaltyRate result = service.getRateByStoreId(20L);

    assertThat(result).isNotNull();
    assertThat(result.getStoreId()).isEqualTo(20L);
  }

  @Test
  @DisplayName("Should throw exception when rate is not found for storeId")
  void shouldThrowWhenRateNotFound() {
    when(persistencePort.findByStoreId(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getRateByStoreId(999L))
        .isInstanceOf(MerchantLoyaltyRateNotFoundException.class)
        .hasMessageContaining("999");
  }
}
