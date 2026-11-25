package fr.miage.collectivit.loyaltyservice.domain.port.in;

import fr.miage.collectivit.loyaltyservice.domain.model.PhysicalGiftRedemptionResult;
import java.util.UUID;

/** Inbound port for atomic in-store physical gift redemption against loyalty points (SLE-43). */
public interface RedeemPhysicalGiftsUseCase {

  PhysicalGiftRedemptionResult redeemPhysicalGifts(
      UUID storeUid, RedeemPhysicalGiftsCommand command, UUID callerUserId, String callerRole);
}
