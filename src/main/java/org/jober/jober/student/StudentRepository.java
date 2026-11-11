package org.jober.jober.student;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /** Admin search by name, email or university. The query must not be null. */
    @Query("""
            SELECT s FROM Student s
            WHERE LOWER(s.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(s.email) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(s.university) LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    Page<Student> search(@Param("query") String query, Pageable pageable);
}
