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
import fr.miage.collectivit.loyaltyservice.domain.exception.TransitGatewayUnavailableException;
import fr.miage.collectivit.loyaltyservice.domain.exception.TransitTicketAlreadyUsedException;
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
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Global exception handler for Loyalty Service web adapters conforming to RFC 7807 ProblemDetail.
 */
@RestControllerAdvice
@SuppressWarnings({"PMD.TooManyMethods", "PMD.ExcessiveImports", "PMD.AvoidDuplicateLiterals"})
public class GlobalExceptionHandler {

  private static final String VALIDATION_FAILED = "Validation Failed";

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
  public ResponseEntity<ProblemDetail> handleBadRequestExceptions(Exception ex) {
    return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleValidationExceptions(
      MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      errors.put(error.getField(), error.getDefaultMessage());
    }
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, VALIDATION_FAILED);
    problem.setTitle(VALIDATION_FAILED);
    problem.setProperty("timestamp", Instant.now().toString());
    problem.setProperty("status", HttpStatus.BAD_REQUEST.value());
    problem.setProperty("error", VALIDATION_FAILED);
    problem.setProperty("message", VALIDATION_FAILED);
    problem.setProperty("details", errors);
    return ResponseEntity.badRequest().body(problem);
  }

  @ExceptionHandler({
    StoreAccessDeniedException.class,
    UserActivityAccessDeniedException.class,
    UserPointsAccessDeniedException.class,
    UserNotVfpException.class
  })
  public ResponseEntity<ProblemDetail> handleAccessDeniedExceptions(RuntimeException ex) {
    return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
  }

  @ExceptionHandler({
    SalesServiceUnavailableException.class,
    ParkingGatewayUnavailableException.class,
    TransitGatewayUnavailableException.class,
    UsersServiceUnavailableException.class
  })
  public ResponseEntity<ProblemDetail> handleExternalServiceUnavailable(RuntimeException ex) {
    return buildResponse(HttpStatus.BAD_GATEWAY, ex.getMessage());
  }

  @ExceptionHandler({
    ParkingSessionAlreadyUsedException.class,
    TransitTicketAlreadyUsedException.class
  })
  public ResponseEntity<ProblemDetail> handleBenefitConflict(RuntimeException ex) {
    return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler({
    StoreNotFoundException.class,
    MerchantLoyaltyRateNotFoundException.class,
    GiftNotFoundException.class
  })
  public ResponseEntity<ProblemDetail> handleNotFoundExceptions(RuntimeException ex) {
    return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(
      DataIntegrityViolationException ex) {
    return buildResponse(
        HttpStatus.CONFLICT, "A database conflict occurred while updating loyalty rate for store.");
  }

  private ResponseEntity<ProblemDetail> buildResponse(HttpStatus status, String message) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message);
    problem.setTitle(status.getReasonPhrase());
    problem.setProperty("timestamp", Instant.now().toString());
    problem.setProperty("status", status.value());
    problem.setProperty("error", status.getReasonPhrase());
    problem.setProperty("message", message);
    return ResponseEntity.status(status).body(problem);
  }
}
