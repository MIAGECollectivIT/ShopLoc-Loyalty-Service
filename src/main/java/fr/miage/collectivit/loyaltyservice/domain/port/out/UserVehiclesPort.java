package fr.miage.collectivit.loyaltyservice.domain.port.out;

import java.util.List;

/** Outbound port to query registered vehicle license plates of a user. */
public interface UserVehiclesPort {

  List<String> getUserLicensePlates(String userId);
}
