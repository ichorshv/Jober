
package org.jober.jober.company;

import org.jober.jober.company.dto.CompanyCreateRequest;
import org.jober.jober.company.dto.CompanyResponse;
import org.jober.jober.company.dto.CompanyUpdateRequest;
import org.jober.jober.common.enums.UserRole;
import org.jober.jober.common.exception.ConflictException;
import org.jober.jober.common.exception.ForbiddenException;
import org.jober.jober.common.exception.NotFoundException;
import org.jober.jober.common.web.CurrentUser;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public CompanyService(
            CompanyRepository companyRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate) {

        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Page<CompanyResponse> getAll(Pageable pageable) {
        return companyRepository.findAll(pageable)
                .map(this::toResponse);
    }

    public CompanyResponse getById(Long id) {
        return toResponse(findCompany(id));
    }

    @Transactional
    public CompanyResponse create(
            CompanyCreateRequest request,
            CurrentUser user) {

        user.requireAdmin();

        validateEmail(request.email(), null);
        validateName(request.name(), null);

        Company company = new Company();
        company.setName(request.name());
        company.setWebsite(request.website());
        company.setIndustry(request.industry());
        company.setLocation(request.location());
        company.setEmail(request.email());

        company.setPasswordHash(
                passwordEncoder.encode(request.password()));

        Company saved = companyRepository.saveAndFlush(company);
        return toResponse(saved);
    }

    @Transactional
    public CompanyResponse updateOwn(
            CompanyUpdateRequest request,
            CurrentUser user) {

        user.requireRole(UserRole.COMPANY);

        Company company = findCompany(user.id());
        return updateCompany(company, request);
    }

    @Transactional
    public CompanyResponse updateById(
            Long id,
            CompanyUpdateRequest request,
            CurrentUser user) {

        user.requireAdmin();

        Company company = findCompany(id);
        return updateCompany(company, request);
    }

    @Transactional
    public void delete(Long id, CurrentUser user) {
        user.requireAdmin();

        Company company = findCompany(id);
        companyRepository.delete(company);
    }

    private CompanyResponse updateCompany(
            Company company,
            CompanyUpdateRequest request) {

        validateEmail(request.email(), company.getId());
        validateName(request.name(), company.getId());

        company.setName(request.name());
        company.setWebsite(request.website());
        company.setIndustry(request.industry());
        company.setLocation(request.location());
        company.setEmail(request.email());

        Company saved = companyRepository.saveAndFlush(company);
        return toResponse(saved);
    }

    private Company findCompany(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Company not found: " + id));
    }

    private void validateEmail(String email, Long excludeId) {

        boolean companyExists = excludeId == null
                ? companyRepository.existsByEmailIgnoreCase(email)
                : companyRepository
                    .existsByEmailIgnoreCaseAndIdNot(
                            email, excludeId);

        if (companyExists) {
            throw new ConflictException(
                    "Email is already registered");
        }

        Integer studentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM students " +
                "WHERE LOWER(email) = LOWER(?)",
                Integer.class,
                email
        );

        if (studentCount != null && studentCount > 0) {
            throw new ConflictException(
                    "Email is already registered");
        }
    }

    private void validateName(String name, Long excludeId) {

    boolean exists = excludeId == null
            ? companyRepository.existsByNameIgnoreCase(name)
            : companyRepository.existsByNameIgnoreCaseAndIdNot(
                    name, excludeId);

    if (exists) {
        throw new ConflictException(
                "Company name already exists");
    }
}

    private CompanyResponse toResponse(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getWebsite(),
                company.getIndustry(),
                company.getLocation(),
                company.getEmail(),
                company.getCreatedAt()
        );
    }
}
