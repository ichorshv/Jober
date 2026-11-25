package org.jober.jober.stats;

import org.jober.jober.company.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StatsRepository extends JpaRepository<Company, Long> {

    @Query(value = """
            WITH first_interview AS (
                SELECT application_id, MIN(scheduled_at) AS first_at
                FROM interviews
                GROUP BY application_id
            ),
            company_stats AS (
                SELECT c.id,
                       c.name AS company,
                       COUNT(a.id) FILTER (WHERE a.status <> 'SAVED') AS applied,
                       COUNT(a.id) FILTER (WHERE a.status IN ('INTERVIEWING', 'OFFER', 'REJECTED')) AS responded,
                       AVG(CAST(fi.first_at AS date) - a.applied_date) AS avg_days
                FROM companies c
                JOIN job_offers j ON j.company_id = c.id
                JOIN applications a ON a.job_offer_id = j.id
                LEFT JOIN first_interview fi ON fi.application_id = a.id
                GROUP BY c.id, c.name
            )
            SELECT company AS "company",
                   applied AS "applied",
                   responded AS "responded",
                   ROUND(100.0 * responded / applied, 1) AS "responseRate",
                   ROUND(CAST(avg_days AS numeric), 1) AS "avgDaysToInterview",
                   RANK() OVER (ORDER BY CAST(responded AS numeric) / applied DESC, avg_days ASC NULLS LAST) AS "companyRank"
            FROM company_stats
            WHERE applied > 0
            ORDER BY "companyRank"
            """, nativeQuery = true)
    List<CompanyRanking> rankCompaniesByResponsiveness();
}