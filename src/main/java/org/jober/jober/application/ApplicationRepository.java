package org.jober.jober.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    boolean existsByJobOfferId(Long jobOfferId);

    Page<Application> findByStudentId(Long studentId, Pageable pageable);

    @Query("""
            SELECT a FROM Application a
            JOIN a.jobOffer j
            JOIN j.company c
            WHERE c.id = :companyId
            """)
    Page<Application> findAllByCompanyId(@Param("companyId") Long companyId, Pageable pageable);
}