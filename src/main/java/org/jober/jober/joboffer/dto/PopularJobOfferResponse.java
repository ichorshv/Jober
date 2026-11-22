package org.jober.jober.joboffer.dto;

public record PopularJobOfferResponse(
        Long jobOfferId,
        String title,
        Long applicationCount
) {
}