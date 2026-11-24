package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** JPA entity representing the vfp_transit_tickets table in PostgreSQL (SLE-47). */
@Entity
@Table(name = "vfp_transit_tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VfpTransitTicketJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "uid", nullable = false, unique = true)
  private UUID uid;

  @Column(name = "user_id", nullable = false, length = 64)
  private String userId;

  @Column(name = "ticket_reference", nullable = false, unique = true, length = 64)
  private String ticketReference;

  @Column(name = "ticket_code", nullable = false, length = 32)
  private String ticketCode;

  @Column(name = "network_name", nullable = false, length = 64)
  private String networkName;

  @Column(name = "status", nullable = false, length = 32)
  private String status;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "ticket_date", nullable = false)
  private LocalDate ticketDate;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
