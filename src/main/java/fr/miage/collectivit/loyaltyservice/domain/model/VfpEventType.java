package fr.miage.collectivit.loyaltyservice.domain.model;

/** Audit event type capturing transitions in user VFP qualification status (SLE-45 / SLE-199). */
public enum VfpEventType {
  VFP_GRANTED,
  VFP_REVOKED
}
