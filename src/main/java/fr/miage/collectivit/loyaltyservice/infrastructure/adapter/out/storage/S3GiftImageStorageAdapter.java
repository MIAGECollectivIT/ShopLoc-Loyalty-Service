package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.storage;

import fr.miage.collectivit.loyaltyservice.domain.model.GiftImageResource;
import fr.miage.collectivit.loyaltyservice.domain.port.out.GiftImageStoragePort;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@SuppressWarnings({
  "PMD.GodClass",
  "PMD.AvoidCatchingGenericException",
  "PMD.GuardLogStatement",
  "PMD.CyclomaticComplexity"
})
public class S3GiftImageStorageAdapter implements GiftImageStoragePort {

  private static final Logger log = LoggerFactory.getLogger(S3GiftImageStorageAdapter.class);

  private static final List<String> ALLOWED_CONTENT_TYPES =
      Arrays.asList("image/jpeg", "image/png", "image/webp", "image/gif", "image/svg+xml");

  private final S3Client s3Client;
  private final String bucketName;
  private final String endpoint;
  private final String publicBaseUrl;

  @Autowired
  public S3GiftImageStorageAdapter(
      S3Client s3Client,
      @Value("${s3.bucket-name:product-images}") String bucketName,
      @Value("${s3.endpoint:http://minio:9000}") String endpoint,
      @Value("${s3.public-base-url:}") String publicBaseUrl) {
    this.s3Client = s3Client;
    this.bucketName = (bucketName != null && !bucketName.isBlank()) ? bucketName : "product-images";
    this.endpoint = endpoint != null ? endpoint.replaceAll("/+$", "") : "http://minio:9000";
    this.publicBaseUrl = publicBaseUrl != null ? publicBaseUrl.replaceAll("/+$", "") : "";
  }

  @Override
  public String storeImage(
      InputStream inputStream, long size, String originalFilename, String contentType) {
    if (inputStream == null || size <= 0) {
      throw new IllegalArgumentException("Le flux de l'image ne peut pas être vide");
    }

    if (contentType == null
        || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
      throw new IllegalArgumentException(
          "Format de fichier non supporté ("
              + contentType
              + "). Formats acceptés : JPEG, PNG, WEBP, GIF, SVG");
    }

    String extension = extractExtension(originalFilename, contentType);
    String s3Key = UUID.randomUUID() + extension;

    try {
      PutObjectRequest putObjectRequest =
          PutObjectRequest.builder().bucket(bucketName).key(s3Key).contentType(contentType).build();

      s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, size));
      log.info("Image cadeau uploadée avec succès sur S3 [bucket={}, key={}]", bucketName, s3Key);
    } catch (Exception e) {
      throw new IllegalStateException("Échec de l'upload de l'image cadeau sur S3 MinIO", e);
    }

    return buildPublicUrl(bucketName, s3Key);
  }

  @Override
  public Optional<GiftImageResource> downloadImage(String imageUrlOrKey) {
    if (imageUrlOrKey == null || imageUrlOrKey.isBlank()) {
      return Optional.empty();
    }

    ParsedS3Ref ref = parseS3Ref(imageUrlOrKey);
    if (ref.key().isBlank()) {
      return Optional.empty();
    }

    try {
      GetObjectRequest getObjectRequest =
          GetObjectRequest.builder().bucket(ref.bucket()).key(ref.key()).build();
      ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
      String contentType = objectBytes.response().contentType();
      if (contentType == null || contentType.isBlank()) {
        contentType = guessContentType(ref.key());
      }
      return Optional.of(new GiftImageResource(objectBytes.asByteArray(), contentType));
    } catch (NoSuchKeyException e) {
      log.warn("Image cadeau introuvable sur S3 : bucket={}, key={}", ref.bucket(), ref.key());
      return Optional.empty();
    } catch (Exception e) {
      log.error(
          "Erreur lors de la récupération de l'image cadeau S3 [bucket={}, key={}] : {}",
          ref.bucket(),
          ref.key(),
          e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public void deleteImage(String imageUrlOrKey) {
    if (imageUrlOrKey == null || imageUrlOrKey.isBlank()) {
      return;
    }
    try {
      ParsedS3Ref ref = parseS3Ref(imageUrlOrKey);
      if (ref.key().isBlank()) {
        return;
      }
      DeleteObjectRequest deleteRequest =
          DeleteObjectRequest.builder().bucket(ref.bucket()).key(ref.key()).build();
      s3Client.deleteObject(deleteRequest);
      log.info("Image cadeau S3 supprimée : bucket={}, key={}", ref.bucket(), ref.key());
    } catch (Exception e) {
      log.warn(
          "Impossible de supprimer l'image cadeau S3 '{}' : {}", imageUrlOrKey, e.getMessage());
    }
  }

  private String extractExtension(String filename, String contentType) {
    if (filename != null && filename.contains(".")) {
      String ext = filename.substring(filename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
      if (Arrays.asList(".jpg", ".jpeg", ".png", ".webp", ".gif", ".svg").contains(ext)) {
        return ext;
      }
    }
    return switch (contentType.toLowerCase(Locale.ROOT)) {
      case "image/png" -> ".png";
      case "image/webp" -> ".webp";
      case "image/gif" -> ".gif";
      case "image/svg+xml" -> ".svg";
      default -> ".jpg";
    };
  }

  private String buildPublicUrl(String targetBucket, String s3Key) {
    if (!publicBaseUrl.isBlank()) {
      return publicBaseUrl + "/" + targetBucket + "/" + s3Key;
    }
    return endpoint + "/" + targetBucket + "/" + s3Key;
  }

  private ParsedS3Ref parseS3Ref(String imageUrlOrKey) {
    String trimmed = imageUrlOrKey.trim();

    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
      try {
        URI uri = URI.create(trimmed);
        String path = uri.getPath();
        if (path != null && path.startsWith("/")) {
          path = path.substring(1);
        }
        if (path != null && path.contains("/")) {
          int firstSlash = path.indexOf('/');
          String extractedBucket = path.substring(0, firstSlash);
          String extractedKey = path.substring(firstSlash + 1);
          return new ParsedS3Ref(extractedBucket, extractedKey);
        }
        return new ParsedS3Ref(bucketName, path != null ? path : trimmed);
      } catch (Exception ignored) {
        return new ParsedS3Ref(bucketName, trimmed);
      }
    }

    if (trimmed.contains("/")) {
      int firstSlash = trimmed.indexOf('/');
      return new ParsedS3Ref(trimmed.substring(0, firstSlash), trimmed.substring(firstSlash + 1));
    }

    return new ParsedS3Ref(bucketName, trimmed);
  }

  private String guessContentType(String key) {
    String lower = key.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".png")) return "image/png";
    if (lower.endsWith(".webp")) return "image/webp";
    if (lower.endsWith(".gif")) return "image/gif";
    if (lower.endsWith(".svg")) return "image/svg+xml";
    return "image/jpeg";
  }

  private record ParsedS3Ref(String bucket, String key) {}
}
