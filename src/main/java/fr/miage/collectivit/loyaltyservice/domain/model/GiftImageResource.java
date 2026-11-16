package fr.miage.collectivit.loyaltyservice.domain.model;

/** Value object representing binary gift image data and its media type retrieved from S3. */
public record GiftImageResource(byte[] bytes, String contentType) {

  public GiftImageResource {
    if (bytes == null || bytes.length == 0) {
      throw new IllegalArgumentException("Image content must not be null or empty");
    }
    if (contentType == null || contentType.isBlank()) {
      contentType = "image/jpeg";
    }
  }
}
