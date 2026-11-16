package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.miage.collectivit.loyaltyservice.domain.model.GiftImageResource;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@DisplayName("Secondary Adapter - S3GiftImageStorageAdapter Unit Tests")
class S3GiftImageStorageAdapterTest {

  private S3Client s3Client;
  private S3GiftImageStorageAdapter adapter;

  @BeforeEach
  void setUp() {
    s3Client = mock(S3Client.class);
    adapter = new S3GiftImageStorageAdapter(s3Client, "product-images", "http://minio:9000", "");
  }

  @Test
  @DisplayName("Should successfully upload image and return S3 URL")
  void shouldUploadGiftImageSuccessfully() {
    byte[] content = "fake-image-bytes".getBytes(StandardCharsets.UTF_8);
    ByteArrayInputStream stream = new ByteArrayInputStream(content);

    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(PutObjectResponse.builder().build());

    String resultUrl = adapter.storeImage(stream, content.length, "mug.png", "image/png");

    assertThat(resultUrl).isNotNull();
    assertThat(resultUrl).contains("http://minio:9000/product-images/");
    assertThat(resultUrl).endsWith(".png");
    verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  @Test
  @DisplayName("Should reject unsupported image content-type")
  void shouldRejectUnsupportedContentType() {
    byte[] content = "not-an-image".getBytes(StandardCharsets.UTF_8);
    ByteArrayInputStream stream = new ByteArrayInputStream(content);

    assertThatThrownBy(
            () -> adapter.storeImage(stream, content.length, "document.pdf", "application/pdf"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Format de fichier non supporté");
  }

  @Test
  @DisplayName("Should successfully download image bytes and content-type")
  void shouldDownloadGiftImageSuccessfully() {
    byte[] content = "image-data".getBytes(StandardCharsets.UTF_8);
    GetObjectResponse getResponse = GetObjectResponse.builder().contentType("image/png").build();
    ResponseBytes<GetObjectResponse> responseBytes =
        ResponseBytes.fromByteArray(getResponse, content);

    when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

    Optional<GiftImageResource> resource =
        adapter.downloadImage("http://minio:9000/product-images/test-mug.png");

    assertThat(resource).isPresent();
    assertThat(resource.get().bytes()).isEqualTo(content);
    assertThat(resource.get().contentType()).isEqualTo("image/png");
  }

  @Test
  @DisplayName("Should return empty when image is not found in S3 (NoSuchKeyException)")
  void shouldReturnEmptyWhenImageNotFound() {
    when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
        .thenThrow(NoSuchKeyException.builder().message("Key not found").build());

    Optional<GiftImageResource> resource =
        adapter.downloadImage("http://minio:9000/product-images/missing.png");

    assertThat(resource).isEmpty();
  }

  @Test
  @DisplayName("Should delete object from S3 successfully")
  void shouldDeleteImageSuccessfully() {
    adapter.deleteImage("http://minio:9000/product-images/test-mug.png");
    verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
  }
}
