
package org.jober.jober.joboffer;

import org.jober.jober.company.Company;
import org.jober.jober.company.CompanyRepository;
import org.jober.jober.common.enums.JobType;
import org.jober.jober.common.enums.UserRole;
import org.jober.jober.common.exception.BadRequestException;
import org.jober.jober.common.exception.ForbiddenException;
import org.jober.jober.common.web.CurrentUser;
import org.jober.jober.joboffer.dto.JobOfferCreateRequest;
import org.jober.jober.joboffer.dto.JobOfferUpdateRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobOfferServiceTest {

    @Mock
    private JobOfferRepository jobOfferRepository;

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private JobOfferService jobOfferService;

    private CurrentUser student;
    private CurrentUser companyUser;
    private CurrentUser admin;

    private Company company;
    private JobOffer jobOffer;

    @BeforeEach
    void setUp() {
        student = new CurrentUser(1L, UserRole.STUDENT);
        companyUser = new CurrentUser(2L, UserRole.COMPANY);
        admin = new CurrentUser(1L, UserRole.ADMIN);

        company = new Company();
        company.setId(2L);
        company.setName("Test Company");

        jobOffer = new JobOffer();
        jobOffer.setId(10L);
        jobOffer.setCompany(company);
        jobOffer.setTitle("Java Intern");
        jobOffer.setJobType(JobType.INTERNSHIP);
        jobOffer.setActive(true);
    }

    @Test
    void studentCannotCreateJobOffer() {
        assertThrows(ForbiddenException.class,
                () -> jobOfferService.create(
                        createRequest(null), student));

        verifyNoInteractions(
                jobOfferRepository, companyRepository);
    }

    @Test
    void companyCanCreateOwnJobOffer() {
        when(companyRepository.findById(2L))
                .thenReturn(Optional.of(company));

        when(jobOfferRepository.saveAndFlush(any(JobOffer.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        var response = jobOfferService.create(
                createRequest(null), companyUser);

        assertEquals("Java Intern", response.title());
        assertEquals(2L, response.companyId());

        verify(jobOfferRepository).saveAndFlush(
                argThat(offer ->
                        offer.getCompany().getId().equals(2L)
                                && offer.isActive()));
    }

    @Test
    void companyCannotCreateForAnotherCompany() {
        assertThrows(ForbiddenException.class,
                () -> jobOfferService.create(
                        createRequest(3L), companyUser));

        verifyNoInteractions(
                jobOfferRepository, companyRepository);
    }

    @Test
    void companyCannotUpdateAnotherCompanyOffer() {
        Company otherCompany = new Company();
        otherCompany.setId(3L);

        jobOffer.setCompany(otherCompany);

        when(jobOfferRepository.findById(10L))
                .thenReturn(Optional.of(jobOffer));

        assertThrows(ForbiddenException.class,
                () -> jobOfferService.update(
                        10L, updateRequest(), companyUser));

        verify(jobOfferRepository, never())
                .saveAndFlush(any(JobOffer.class));
    }

    @Test
    void companyCanUpdateOwnJobOffer() {
        when(jobOfferRepository.findById(10L))
                .thenReturn(Optional.of(jobOffer));

        when(jobOfferRepository.saveAndFlush(any(JobOffer.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        var response = jobOfferService.update(
                10L, updateRequest(), companyUser);

        assertEquals("Updated Java Intern", response.title());
        assertEquals(2L, response.companyId());

        verify(jobOfferRepository).saveAndFlush(jobOffer);
    }

    @Test
    void adminCanUpdateAnyJobOffer() {
        when(jobOfferRepository.findById(10L))
                .thenReturn(Optional.of(jobOffer));

        when(jobOfferRepository.saveAndFlush(any(JobOffer.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        var response = jobOfferService.update(
                10L, updateRequest(), admin);

        assertEquals("Updated Java Intern", response.title());

        verify(jobOfferRepository).saveAndFlush(jobOffer);
    }

    @Test
    void maximumSalaryCannotBeLowerThanMinimum() {
        JobOfferCreateRequest request =
                new JobOfferCreateRequest(
                        null,
                        "Java Intern",
                        "Internship position",
                        JobType.INTERNSHIP,
                        "Sofia",
                        new BigDecimal("2000"),
                        new BigDecimal("1000"),
                        LocalDate.now().plusDays(30)
                );

        when(companyRepository.findById(2L))
                .thenReturn(Optional.of(company));

        assertThrows(BadRequestException.class,
                () -> jobOfferService.create(
                        request, companyUser));

        verify(jobOfferRepository, never())
                .saveAndFlush(any(JobOffer.class));
    }

    @Test
    void deadlineCannotBeInThePast() {
        JobOfferCreateRequest request =
                new JobOfferCreateRequest(
                        null,
                        "Java Intern",
                        "Internship position",
                        JobType.INTERNSHIP,
                        "Sofia",
                        new BigDecimal("800"),
                        new BigDecimal("1200"),
                        LocalDate.now().minusDays(1)
                );

        when(companyRepository.findById(2L))
                .thenReturn(Optional.of(company));

        assertThrows(BadRequestException.class,
                () -> jobOfferService.create(
                        request, companyUser));

        verify(jobOfferRepository, never())
                .saveAndFlush(any(JobOffer.class));
    }

    private JobOfferCreateRequest createRequest(Long companyId) {
        return new JobOfferCreateRequest(
                companyId,
                "Java Intern",
                "Internship position",
                JobType.INTERNSHIP,
                "Sofia",
                new BigDecimal("800"),
                new BigDecimal("1200"),
                LocalDate.now().plusDays(30)
        );
    }

    private JobOfferUpdateRequest updateRequest() {
        return new JobOfferUpdateRequest(
                "Updated Java Intern",
                "Updated description",
                JobType.INTERNSHIP,
                "Sofia",
                new BigDecimal("1000"),
                new BigDecimal("1500"),
                LocalDate.now().plusDays(60)
        );
    }
}
