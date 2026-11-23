package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web;

import fr.miage.collectivit.loyaltyservice.domain.exception.GiftNotFoundException;
import fr.miage.collectivit.loyaltyservice.domain.exception.InsufficientGiftStockException;
import fr.miage.collectivit.loyaltyservice.domain.exception.InvalidGiftException;
import fr.miage.collectivit.loyaltyservice.domain.exception.InvalidLicensePlateException;
import fr.miage.collectivit.loyaltyservice.domain.exception.InvalidLoyaltyRateException;
import fr.miage.collectivit.loyaltyservice.domain.exception.MerchantLoyaltyRateNotFoundException;
import fr.miage.collectivit.loyaltyservice.domain.exception.ParkingGatewayUnavailableException;
import fr.miage.collectivit.loyaltyservice.domain.exception.ParkingSessionAlreadyUsedException;
import fr.miage.collectivit.loyaltyservice.domain.exception.SalesServiceUnavailableException;
import fr.miage.collectivit.loyaltyservice.domain.exception.StoreAccessDeniedException;
import fr.miage.collectivit.loyaltyservice.domain.exception.StoreNotFoundException;
import fr.miage.collectivit.loyaltyservice.domain.exception.UserActivityAccessDeniedException;
import fr.miage.collectivit.loyaltyservice.domain.exception.UserNotVfpException;
import fr.miage.collectivit.loyaltyservice.domain.exception.UserPointsAccessDeniedException;
import fr.miage.collectivit.loyaltyservice.domain.exception.UsersServiceUnavailableException;
import fr.miage.collectivit.loyaltyservice.domain.exception.VehicleNotRegisteredException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Global exception handler for Loyalty Service web adapters. */
@RestControllerAdvice
@SuppressWarnings("PMD.TooManyMethods")
public class GlobalExceptionHandler {

  @ExceptionHandler({
    InvalidLoyaltyRateException.class,
    InvalidGiftException.class,
    InsufficientGiftStockException.class,
    InvalidLicensePlateException.class,
    VehicleNotRegisteredException.class,
    IllegalArgumentException.class,
    HandlerMethodValidationException.class,
    MethodArgumentTypeMismatchException.class,
    jakarta.validation.ConstraintViolationException.class
  })
  public ResponseEntity<Map<String, Object>> handleBadRequestExceptions(Exception ex) {
    return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidationExceptions(
      MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      errors.put(error.getField(), error.getDefaultMessage());
    }
    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", Instant.now().toString());
    body.put("status", HttpStatus.BAD_REQUEST.value());
    body.put("error", "Validation Failed");
    body.put("details", errors);
    return ResponseEntity.badRequest().body(body);
  }

  @ExceptionHandler({
    StoreAccessDeniedException.class,
    UserActivityAccessDeniedException.class,
    UserPointsAccessDeniedException.class,
    UserNotVfpException.class
  })
  public ResponseEntity<Map<String, Object>> handleAccessDeniedExceptions(RuntimeException ex) {
    return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
  }

  @ExceptionHandler({
    SalesServiceUnavailableException.class,
    ParkingGatewayUnavailableException.class,
    UsersServiceUnavailableException.class
  })
  public ResponseEntity<Map<String, Object>> handleExternalServiceUnavailable(RuntimeException ex) {
    return buildResponse(HttpStatus.BAD_GATEWAY, ex.getMessage());
  }

  @ExceptionHandler(ParkingSessionAlreadyUsedException.class)
  public ResponseEntity<Map<String, Object>> handleParkingConflict(
      ParkingSessionAlreadyUsedException ex) {
    return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler({
    StoreNotFoundException.class,
    MerchantLoyaltyRateNotFoundException.class,
    GiftNotFoundException.class
  })
  public ResponseEntity<Map<String, Object>> handleNotFoundExceptions(RuntimeException ex) {
    return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(
      DataIntegrityViolationException ex) {
    return buildResponse(
        HttpStatus.CONFLICT, "A database conflict occurred while updating loyalty rate for store.");
  }

  private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", Instant.now().toString());
    body.put("status", status.value());
    body.put("message", message);
    return ResponseEntity.status(status).body(body);
  }
}
