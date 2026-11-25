package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto;

import fr.miage.collectivit.loyaltyservice.domain.port.in.GiftCreateCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Request DTO for creating a new loyalty gift item (SLE-60 / SLE-49). */
public record GiftCreateRequest(
        @NotBlank(message = "Gift name is mandatory") @Size(max = 120, message = "Gift name must not exceed 120 characters") String name,
        @Size(max = 1000, message = "Gift description must not exceed 1000 characters") String description,
        @NotNull(message = "Points cost is mandatory") @Positive(message = "Points cost must be strictly positive") Integer pointsCost,
        @PositiveOrZero(message = "Stock cannot be negative") Integer stock,
        @Size(max = 500, message = "Image URL must not exceed 500 characters")
        @Pattern(
            regexp = "^(https?://.*|/.*)?$",
            message = "Image URL must use http(s) or a relative path")
        String imageUrl,
        Boolean vfpExclusive) {

    public GiftCreateRequest(
            String name, String description, Integer pointsCost, Integer stock, String imageUrl) {
        this(name, description, pointsCost, stock, imageUrl, false);
    }

    public GiftCreateCommand toCommand() {
        return new GiftCreateCommand(
                name,
                description,
                pointsCost,
                stock != null ? stock : 0,
                imageUrl,
                Boolean.TRUE.equals(vfpExclusive));
    }
}
