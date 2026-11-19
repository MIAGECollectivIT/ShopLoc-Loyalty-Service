package fr.miage.collectivit.loyaltyservice.domain.model;

/**
 * Domain projection describing a user's daily VFP institutional benefit eligibility under the
 * Europe/Paris calendar-day non-cumulation rule (SLE-48 / SLE-233).
 */
public record VfpDailyEligibility(
    boolean isVfp,
    boolean eligible,
    boolean parkingUsedToday,
    boolean transitUsedToday,
    String consumedBenefitType,
    VfpParkingSession activeParkingSession) {}
