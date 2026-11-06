package fr.miage.collectivit.loyaltyservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.miage.collectivit.loyaltyservice.domain.exception.MerchantLoyaltyRateNotFoundException;
import fr.miage.collectivit.loyaltyservice.domain.model.MerchantLoyaltyRate;
import fr.miage.collectivit.loyaltyservice.domain.port.out.MerchantLoyaltyRatePersistencePort;
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

  @InjectMocks private PointsAccountService service;

  @Test
  @DisplayName("Devrait créer un nouveau barème s'il n'existe pas encore")
  void shouldCreateNewRateIfNotExists() {
    when(persistencePort.findByStoreId(10L)).thenReturn(Optional.empty());
    when(persistencePort.save(any(MerchantLoyaltyRate.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    MerchantLoyaltyRate result = service.configureRate(10L, new BigDecimal("2.00"), 5);

    assertThat(result.getStoreId()).isEqualTo(10L);
    assertThat(result.getEuroAmount()).isEqualByComparingTo(new BigDecimal("2.00"));
    assertThat(result.getPointsReward()).isEqualTo(5);
    verify(persistencePort).save(any(MerchantLoyaltyRate.class));
  }

  @Test
  @DisplayName("Devrait mettre à jour le barème s'il existe déjà")
  void shouldUpdateExistingRate() {
    MerchantLoyaltyRate existing = new MerchantLoyaltyRate(10L, new BigDecimal("1.00"), 1);
    when(persistencePort.findByStoreId(10L)).thenReturn(Optional.of(existing));
    when(persistencePort.save(any(MerchantLoyaltyRate.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    MerchantLoyaltyRate result = service.configureRate(10L, new BigDecimal("3.00"), 4);

    assertThat(result.getStoreId()).isEqualTo(10L);
    assertThat(result.getEuroAmount()).isEqualByComparingTo(new BigDecimal("3.00"));
    assertThat(result.getPointsReward()).isEqualTo(4);
    verify(persistencePort).save(existing);
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
