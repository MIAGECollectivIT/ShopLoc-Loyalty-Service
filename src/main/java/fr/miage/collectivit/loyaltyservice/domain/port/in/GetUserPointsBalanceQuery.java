package fr.miage.collectivit.loyaltyservice.domain.port.in;

import fr.miage.collectivit.loyaltyservice.domain.model.UserPointsBalance;

public interface GetUserPointsBalanceQuery {

  UserPointsBalance getUserPointsBalance(String userId);
}
