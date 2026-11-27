package org.jober.jober.joboffer.dto;

public record JobOfferStats(
        Long offerId,
        String title,
        String company,
        Long applicants
) {
}