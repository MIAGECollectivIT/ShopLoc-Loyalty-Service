package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto;

import fr.miage.collectivit.loyaltyservice.domain.port.in.GiftUpdateCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Request DTO for updating an existing loyalty gift item (SLE-60 / SLE-49). */
public record GiftUpdateRequest(
        @NotBlank(message = "Gift name is mandatory") @Size(max = 120, message = "Gift name must not exceed 120 characters") String name,
        @Size(max = 1000, message = "Gift description must not exceed 1000 characters") String description,
        @NotNull(message = "Points cost is mandatory") @Positive(message = "Points cost must be strictly positive") Integer pointsCost,
        @Size(max = 500, message = "Image URL must not exceed 500 characters")
        @Pattern(
            regexp = "^(https?://.*|/.*)?$",
            message = "Image URL must use http(s) or a relative path")
        String imageUrl,
        Boolean vfpExclusive) {

    public GiftUpdateRequest(
            String name, String description, Integer pointsCost, String imageUrl) {
        this(name, description, pointsCost, imageUrl, false);
    }

    public GiftUpdateCommand toCommand() {
        return new GiftUpdateCommand(
                name, description, pointsCost, imageUrl, Boolean.TRUE.equals(vfpExclusive));
    }
}
