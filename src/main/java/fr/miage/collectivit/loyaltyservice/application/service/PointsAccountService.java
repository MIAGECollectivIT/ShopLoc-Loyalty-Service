package fr.miage.collectivit.loyaltyservice.application.service;

import fr.miage.collectivit.loyaltyservice.domain.exception.MerchantLoyaltyRateNotFoundException;
import fr.miage.collectivit.loyaltyservice.domain.model.MerchantLoyaltyRate;
import fr.miage.collectivit.loyaltyservice.domain.port.in.ConfigureMerchantLoyaltyRateUseCase;
import fr.miage.collectivit.loyaltyservice.domain.port.in.GetMerchantLoyaltyRateQuery;
import fr.miage.collectivit.loyaltyservice.domain.port.out.MerchantLoyaltyRatePersistencePort;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service applicatif orchestrant les cas d'utilisation liés aux comptes de fidélité et aux barèmes
 * marchands (Composant PointsAccountService prescrit par le schéma d'architecture).
 */
@Service
@RequiredArgsConstructor
public class PointsAccountService
    implements ConfigureMerchantLoyaltyRateUseCase, GetMerchantLoyaltyRateQuery {

  private final MerchantLoyaltyRatePersistencePort persistencePort;

  @Override
  @Transactional
  public MerchantLoyaltyRate configureRate(
      Long storeId, BigDecimal euroAmount, Integer pointsReward) {
    return persistencePort
        .findByStoreId(storeId)
        .map(
            existing -> {
              existing.updateRates(euroAmount, pointsReward);
              return persistencePort.save(existing);
            })
        .orElseGet(
            () -> {
              MerchantLoyaltyRate newRate =
                  new MerchantLoyaltyRate(storeId, euroAmount, pointsReward);
              return persistencePort.save(newRate);
            });
  }

  @Override
  @Transactional(readOnly = true)
  public MerchantLoyaltyRate getRateByStoreId(Long storeId) {
    return persistencePort
        .findByStoreId(storeId)
        .orElseThrow(() -> new MerchantLoyaltyRateNotFoundException(storeId));
  }
}
