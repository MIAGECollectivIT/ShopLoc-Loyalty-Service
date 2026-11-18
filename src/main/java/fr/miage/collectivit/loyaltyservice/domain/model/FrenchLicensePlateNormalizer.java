package fr.miage.collectivit.loyaltyservice.domain.model;

import fr.miage.collectivit.loyaltyservice.domain.exception.InvalidLicensePlateException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Validates and normalizes French vehicle license plates (SIV and FNI formats). */
public final class FrenchLicensePlateNormalizer {

  private static final Pattern SIV_PATTERN =
      Pattern.compile(
          "^[A-HJ-NP-TV-Z]{2}[-\\s]?[0-9]{3}[-\\s]?[A-HJ-NP-TV-Z]{2}$", Pattern.CASE_INSENSITIVE);
  private static final Pattern FNI_PATTERN =
      Pattern.compile(
          "^[0-9]{1,4}[-\\s]?[A-Z]{1,3}[-\\s]?(?:[0-9]{2,3}|2[AB])$", Pattern.CASE_INSENSITIVE);
  private static final Pattern SIV_CLEAN_PATTERN =
      Pattern.compile("^[A-HJ-NP-TV-Z]{2}[0-9]{3}[A-HJ-NP-TV-Z]{2}$");
  private static final Pattern FNI_CLEAN_PATTERN =
      Pattern.compile("^([0-9]{1,4})([A-Z]{1,3})([0-9]{2,3}|2[AB])$");

  private FrenchLicensePlateNormalizer() {}

  /**
   * Validates and returns the normalized license plate (e.g. AA-123-BB or 1234 AB 75).
   *
   * @throws InvalidLicensePlateException if the plate is blank or does not match SIV/FNI formats
   */
  public static String validateAndNormalize(String value) {
    if (value == null || value.isBlank()) {
      throw new InvalidLicensePlateException("License plate must not be blank");
    }
    String trimmed = value.trim();
    if (!SIV_PATTERN.matcher(trimmed).matches() && !FNI_PATTERN.matcher(trimmed).matches()) {
      throw new InvalidLicensePlateException("Invalid French license plate format: " + trimmed);
    }
    return normalize(trimmed);
  }

  /** Normalizes a raw plate string without throwing if non-matching. */
  public static String normalize(String value) {
    if (value == null) {
      return null;
    }
    String raw = value.trim().toUpperCase(Locale.ROOT);
    String clean = raw.replaceAll("[-\\s]", "");

    if (SIV_CLEAN_PATTERN.matcher(clean).matches()) {
      return clean.substring(0, 2) + "-" + clean.substring(2, 5) + "-" + clean.substring(5, 7);
    }

    Matcher fniMatcher = FNI_CLEAN_PATTERN.matcher(clean);
    if (fniMatcher.matches()) {
      return fniMatcher.group(1) + " " + fniMatcher.group(2) + " " + fniMatcher.group(3);
    }

    return raw;
  }
}
