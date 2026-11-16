package fr.miage.collectivit.loyaltyservice.domain.port.out;

import fr.miage.collectivit.loyaltyservice.domain.model.GiftImageResource;
import java.io.InputStream;
import java.util.Optional;

/** Outbound port for persisting and streaming gift images using S3-compatible object storage. */
public interface GiftImageStoragePort {

  /**
   * Uploads a gift image to the S3 bucket.
   *
   * @param inputStream data stream of the image
   * @param size size in bytes
   * @param originalFilename original name of the uploaded file
   * @param contentType MIME type of the image
   * @return public or relative storage URL/key of the saved image
   */
  String storeImage(
      InputStream inputStream, long size, String originalFilename, String contentType);

  /**
   * Retrieves the binary content and content-type of an image from S3.
   *
   * @param imageUrlOrKey S3 URL or object key
   * @return Optional containing the resource if present
   */
  Optional<GiftImageResource> downloadImage(String imageUrlOrKey);

  /**
   * Deletes an image from the S3 bucket.
   *
   * @param imageUrlOrKey S3 URL or object key
   */
  void deleteImage(String imageUrlOrKey);
}
