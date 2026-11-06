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
  @DisplayName("Devrait créer un nouveau barème en mode ADMIN sans vérification Store Service")
  void shouldCreateNewRateAsAdmin() {
    when(persistencePort.findByStoreId(10L)).thenReturn(Optional.empty());
    when(persistencePort.save(any(MerchantLoyaltyRate.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    MerchantLoyaltyRate result =
        service.configureRate(10L, new BigDecimal("2.00"), 5, 99L, "ADMIN");

    assertThat(result.getStoreId()).isEqualTo(10L);
    assertThat(result.getEuroAmount()).isEqualByComparingTo(new BigDecimal("2.00"));
    assertThat(result.getPointsReward()).isEqualTo(5);
    verify(persistencePort).save(any(MerchantLoyaltyRate.class));
    verify(storeVerificationPort, never()).storeExists(any());
  }

  @Test
  @DisplayName("Devrait autoriser le commerçant propriétaire à mettre à jour son barème")
  void shouldAllowStoreOwnerMerchantToConfigureRate() {
    when(storeVerificationPort.storeExists(10L)).thenReturn(true);
    when(storeVerificationPort.isStoreOwnedBy(10L, 42L)).thenReturn(true);

    MerchantLoyaltyRate existing = new MerchantLoyaltyRate(10L, new BigDecimal("1.00"), 1);
    when(persistencePort.findByStoreId(10L)).thenReturn(Optional.of(existing));
    when(persistencePort.save(any(MerchantLoyaltyRate.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    MerchantLoyaltyRate result =
        service.configureRate(10L, new BigDecimal("3.00"), 4, 42L, "MERCHANT");

    assertThat(result.getStoreId()).isEqualTo(10L);
    assertThat(result.getEuroAmount()).isEqualByComparingTo(new BigDecimal("3.00"));
    assertThat(result.getPointsReward()).isEqualTo(4);
    verify(persistencePort).save(existing);
  }

  @Test
  @DisplayName(
      "Devrait refuser la modification si le commerce n'existe pas (StoreNotFoundException)")
  void shouldThrowWhenStoreDoesNotExist() {
    when(storeVerificationPort.storeExists(999L)).thenReturn(false);

    assertThatThrownBy(
            () -> service.configureRate(999L, new BigDecimal("2.00"), 2, 42L, "MERCHANT"))
        .isInstanceOf(StoreNotFoundException.class)
        .hasMessageContaining("999");

    verify(persistencePort, never()).save(any());
  }

  @Test
  @DisplayName(
      "Devrait refuser la modification si le commerçant n'est pas le propriétaire (StoreAccessDeniedException)")
  void shouldThrowWhenMerchantIsNotStoreOwner() {
    when(storeVerificationPort.storeExists(10L)).thenReturn(true);
    when(storeVerificationPort.isStoreOwnedBy(10L, 99L)).thenReturn(false);

    assertThatThrownBy(() -> service.configureRate(10L, new BigDecimal("2.00"), 2, 99L, "MERCHANT"))
        .isInstanceOf(StoreAccessDeniedException.class)
        .hasMessageContaining("propriétaire");

    verify(persistencePort, never()).save(any());
  }

  @Test
  @DisplayName("Devrait refuser la modification si l'identifiant commerçant est nul")
  void shouldThrowWhenMerchantUserIdIsNull() {
    assertThatThrownBy(
            () -> service.configureRate(10L, new BigDecimal("2.00"), 2, (Long) null, "MERCHANT"))
        .isInstanceOf(StoreAccessDeniedException.class)
        .hasMessageContaining("manquant");

    verify(persistencePort, never()).save(any());
  }

  @Test
  @DisplayName("Devrait récupérer un barème existant par storeId")
  void shouldReturnExistingRate() {
    MerchantLoyaltyRate existing = new MerchantLoyaltyRate(20L, new BigDecimal("1.00"), 2);
    when(persistencePort.findByStoreId(20L)).thenReturn(Optional.of(existing));

    MerchantLoyaltyRate result = service.getRateByStoreId(20L);

    assertThat(result).isNotNull();
    assertThat(result.getStoreId()).isEqualTo(20L);
  }

  @Test
  @DisplayName("Devrait lever une exception si le barème n'existe pas")
  void shouldThrowWhenRateNotFound() {
    when(persistencePort.findByStoreId(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getRateByStoreId(999L))
        .isInstanceOf(MerchantLoyaltyRateNotFoundException.class)
        .hasMessageContaining("999");
  }
}
