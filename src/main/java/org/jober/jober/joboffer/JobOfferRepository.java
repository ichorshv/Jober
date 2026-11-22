
package org.jober.jober.joboffer;

import org.jober.jober.common.enums.ApplicationStatus;
import org.jober.jober.joboffer.dto.PopularJobOfferResponse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JobOfferRepository
        extends JpaRepository<JobOffer, Long>,
                JpaSpecificationExecutor<JobOffer> {

    @Query("""
            SELECT new org.jober.jober.joboffer.dto.PopularJobOfferResponse(
                j.id,
                j.title,
                COUNT(a.id)
            )
            FROM JobOffer j
            JOIN Application a ON a.jobOffer = j
            WHERE a.status <> :savedStatus
            GROUP BY j.id, j.title
            HAVING COUNT(a.id) >= :minApplications
            ORDER BY COUNT(a.id) DESC, j.id ASC
            """)
    List<PopularJobOfferResponse> findPopularJobOffers(
            @Param("minApplications") long minApplications,
            @Param("savedStatus") ApplicationStatus savedStatus
    );
}
