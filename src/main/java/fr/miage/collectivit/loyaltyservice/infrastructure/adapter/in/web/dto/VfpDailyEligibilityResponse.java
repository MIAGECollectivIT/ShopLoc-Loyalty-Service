package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto;

import fr.miage.collectivit.loyaltyservice.domain.model.VfpDailyEligibility;
import io.swagger.v3.oas.annotations.media.Schema;

/** REST response DTO representing a user's daily VFP benefit eligibility (SLE-48 / SLE-233). */
@Schema(description = "Éligibilité journalière aux avantages institutionnels VFP")
public record VfpDailyEligibilityResponse(
    @Schema(description = "Statut VFP actif de l'usager", example = "true") boolean isVfp,
    @Schema(
            description = "Indique si un avantage peut encore être activé aujourd'hui",
            example = "true")
        boolean eligible,
    @Schema(description = "Stationnement gratuit déjà utilisé aujourd'hui", example = "false")
        boolean parkingUsedToday,
    @Schema(description = "Titre de transport gratuit déjà utilisé aujourd'hui", example = "false")
        boolean transitUsedToday,
    @Schema(description = "Type d'avantage déjà consommé aujourd'hui", example = "PARKING")
        String consumedBenefitType,
    @Schema(description = "Session de stationnement encore active s'il y en a une")
        ParkingSessionResponse activeParkingSession) {

  public static VfpDailyEligibilityResponse fromDomain(VfpDailyEligibility eligibility) {
    ParkingSessionResponse activeSession =
        eligibility.activeParkingSession() != null
            ? ParkingSessionResponse.fromDomain(eligibility.activeParkingSession())
            : null;
    return new VfpDailyEligibilityResponse(
        eligibility.isVfp(),
        eligibility.eligible(),
        eligibility.parkingUsedToday(),
        eligibility.transitUsedToday(),
        eligibility.consumedBenefitType(),
        activeSession);
  }
}
