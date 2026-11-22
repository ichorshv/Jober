
package org.jober.jober.company;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository
        extends JpaRepository<Company, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(
            String email, Long id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
