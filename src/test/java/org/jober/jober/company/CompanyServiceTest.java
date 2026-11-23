
package org.jober.jober.company;

import org.jober.jober.company.dto.CompanyCreateRequest;
import org.jober.jober.company.dto.CompanyUpdateRequest;
import org.jober.jober.common.enums.UserRole;
import org.jober.jober.common.exception.ConflictException;
import org.jober.jober.common.exception.ForbiddenException;
import org.jober.jober.common.web.CurrentUser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private CompanyService companyService;

    private CurrentUser admin;
    private CurrentUser companyUser;

    @BeforeEach
    void setUp() {
        admin = new CurrentUser(1L, UserRole.ADMIN);
        companyUser = new CurrentUser(2L, UserRole.COMPANY);
    }

    @Test
    void companyCannotCreateAnotherCompany() {
        CompanyCreateRequest request = createRequest();

        assertThrows(ForbiddenException.class,
                () -> companyService.create(request, companyUser));

        verifyNoInteractions(companyRepository);
    }

    @Test
    void companyCannotDeleteAnotherCompany() {
        assertThrows(ForbiddenException.class,
                () -> companyService.delete(1L, companyUser));

        verifyNoInteractions(companyRepository);
    }

    @Test
    void companyCanUpdateOwnProfile() {
        Company company = new Company();
        company.setId(2L);
        company.setName("Old Name");
        company.setEmail("company@example.com");

        when(companyRepository.findById(2L))
                .thenReturn(Optional.of(company));

        when(companyRepository.saveAndFlush(any(Company.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CompanyUpdateRequest request = new CompanyUpdateRequest(
                "Updated Company",
                "https://example.com",
                "Software",
                "Sofia",
                "company@example.com"
        );

        var response = companyService.updateOwn(request, companyUser);

        assertEquals("Updated Company", response.name());
        assertEquals(2L, response.id());

        verify(companyRepository).findById(2L);
        verify(companyRepository).saveAndFlush(company);
    }

    @Test
    void duplicateEmailReturnsConflict() {
        CompanyCreateRequest request = createRequest();

        when(companyRepository.existsByEmailIgnoreCase(
                "new@example.com"))
                .thenReturn(true);

        assertThrows(ConflictException.class,
                () -> companyService.create(request, admin));

        verify(companyRepository, never())
                .saveAndFlush(any(Company.class));
    }

    @Test
    void adminCreatesCompanyWithHashedPassword() {
        CompanyCreateRequest request = createRequest();

        when(passwordEncoder.encode("password123"))
                .thenReturn("hashed-password");

        when(companyRepository.saveAndFlush(any(Company.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = companyService.create(request, admin);

        assertEquals("Test Company", response.name());

        verify(passwordEncoder).encode("password123");

        verify(companyRepository).saveAndFlush(argThat(company ->
                company.getPasswordHash().equals("hashed-password")
        ));
    }



@Test
void duplicateCompanyNameReturnsConflict() {
    CompanyCreateRequest request = createRequest();

    when(companyRepository.existsByNameIgnoreCase("Test Company"))
            .thenReturn(true);

    assertThrows(ConflictException.class,
            () -> companyService.create(request, admin));

    verify(companyRepository, never())
            .saveAndFlush(any(Company.class));
}

@Test
void duplicateCompanyNameWithDifferentCaseReturnsConflict() {
    CompanyCreateRequest request = new CompanyCreateRequest(
            "TEST COMPANY",
            "https://example.com",
            "Software",
            "Sofia",
            "new@example.com",
            "password123"
    );

    when(companyRepository.existsByNameIgnoreCase("TEST COMPANY"))
            .thenReturn(true);

    assertThrows(ConflictException.class,
            () -> companyService.create(request, admin));

    verify(companyRepository)
            .existsByNameIgnoreCase("TEST COMPANY");

    verify(companyRepository, never())
            .saveAndFlush(any(Company.class));
}

@Test
void studentEmailCannotBeUsedForCompany() {
    CompanyCreateRequest request = createRequest();

    when(jdbcTemplate.queryForObject(
            anyString(),
            eq(Integer.class),
            eq("new@example.com")
    )).thenReturn(1);

    assertThrows(ConflictException.class,
            () -> companyService.create(request, admin));

    verify(companyRepository, never())
            .saveAndFlush(any(Company.class));
}

@Test
void companyCannotUpdateAnotherCompanyById() {
    CompanyUpdateRequest request = new CompanyUpdateRequest(
            "Updated Company",
            "https://example.com",
            "Software",
            "Sofia",
            "company@example.com"
    );

    assertThrows(ForbiddenException.class,
            () -> companyService.updateById(
                    1L, request, companyUser));

    verifyNoInteractions(companyRepository);
}


    private CompanyCreateRequest createRequest() {
        return new CompanyCreateRequest(
                "Test Company",
                "https://example.com",
                "Software",
                "Sofia",
                "new@example.com",
                "password123"
        );
    }
}
