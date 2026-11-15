package fr.miage.collectivit.loyaltyservice.domain.port.in;

import fr.miage.collectivit.loyaltyservice.domain.model.UserVfpStatus;
import java.util.Optional;

/** Inbound query port to retrieve the current persisted VFP status of a user (SLE-45 / SLE-203). */
public interface GetUserVfpStatusQuery {

  Optional<UserVfpStatus> getStatus(String userId);
}
