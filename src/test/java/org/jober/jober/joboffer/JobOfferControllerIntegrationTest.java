
package org.jober.jober.joboffer;

import jakarta.persistence.EntityManager;

import org.jober.jober.JoberApplication;
import org.jober.jober.TestcontainersConfiguration;
import org.jober.jober.application.Application;
import org.jober.jober.application.ApplicationRepository;
import org.jober.jober.company.CompanyRepository;
import org.jober.jober.common.enums.ApplicationStatus;
import org.jober.jober.common.enums.JobType;
import org.jober.jober.student.Student;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = JoberApplication.class)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class JobOfferControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobOfferRepository jobOfferRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void getJobOfferByIdReturns200() throws Exception {
        mockMvc.perform(get("/api/job-offers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Java Intern"))
                .andExpect(jsonPath("$.companyName").value("Acme Software"));
    }

    @Test
    void companyCanCreateJobOffer() throws Exception {
        String body = """
                {
                  "title": "Spring Boot Intern",
                  "description": "Java backend internship",
                  "jobType": "INTERNSHIP",
                  "location": "Sofia",
                  "salaryMin": 1000,
                  "salaryMax": 1500,
                  "deadline": "2099-12-31"
                }
                """;

        mockMvc.perform(post("/api/job-offers")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "COMPANY")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Spring Boot Intern"))
                .andExpect(jsonPath("$.companyId").value(1))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void studentCannotCreateJobOffer() throws Exception {
        String body = """
                {
                  "title": "Unauthorized Offer",
                  "jobType": "INTERNSHIP",
                  "location": "Sofia",
                  "deadline": "2099-12-31"
                }
                """;

        mockMvc.perform(post("/api/job-offers")
                        .header("X-User-Id", "2")
                        .header("X-User-Role", "STUDENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void companyCanUpdateOwnJobOffer() throws Exception {
        String body = """
                {
                  "title": "Updated Java Intern",
                  "description": "Updated internship",
                  "jobType": "INTERNSHIP",
                  "location": "Sofia",
                  "salaryMin": 1200,
                  "salaryMax": 1800,
                  "deadline": "2099-12-31"
                }
                """;

        mockMvc.perform(put("/api/job-offers/1")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "COMPANY")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Java Intern"));
    }

    @Test
    void companyCannotUpdateAnotherCompanyOffer() throws Exception {
        String body = """
                {
                  "title": "Unauthorized Update",
                  "jobType": "FULL_TIME",
                  "location": "Sofia",
                  "deadline": "2099-12-31"
                }
                """;

        mockMvc.perform(put("/api/job-offers/3")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "COMPANY")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void companyCanCloseOwnJobOffer() throws Exception {
        mockMvc.perform(patch("/api/job-offers/1/close")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "COMPANY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void popularOffersCountRealApplicationsOnly() throws Exception {

        // Create a separate job offer for the test
        JobOffer offer = new JobOffer();
        offer.setCompany(companyRepository.findById(1L).orElseThrow());
        offer.setTitle("Popular Test Internship");
        offer.setDescription("Integration test position");
        offer.setJobType(JobType.INTERNSHIP);
        offer.setLocation("Sofia");
        offer.setDeadline(LocalDate.now().plusDays(30));
        offer.setActive(true);

        offer = jobOfferRepository.saveAndFlush(offer);

        // Two students actually apply
        createTestApplication(1L, offer, ApplicationStatus.APPLIED);
        createTestApplication(2L, offer, ApplicationStatus.APPLIED);

        // Third student only saves the offer
        createTestApplication(3L, offer, ApplicationStatus.SAVED);

        applicationRepository.flush();

        // Request offers with at least 2 applications
        mockMvc.perform(get("/api/job-offers/popular")
                        .param("minApplications", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$[?(@.title == 'Popular Test Internship')].applicationCount")
                        .value(contains(2)));
    }

    @Test
    void popularOffersRejectInvalidMinimum() throws Exception {
        mockMvc.perform(get("/api/job-offers/popular")
                        .param("minApplications", "0"))
                .andExpect(status().isBadRequest());
    }

    // Helper method used by the integration test
    private void createTestApplication(
            Long studentId,
            JobOffer offer,
            ApplicationStatus status) {

        Application application = new Application();

        application.setStudent(
                entityManager.getReference(Student.class, studentId));

        application.setJobOffer(offer);
        application.setStatus(status);

        if (status != ApplicationStatus.SAVED) {
            application.setAppliedDate(LocalDate.now());
        }

        applicationRepository.save(application);
    }



@Test
void studentSeesOnlyNotAppliedActiveOffers() throws Exception {

    // Student 1 saves this offer but does not apply
    JobOffer savedOffer = createTestOffer(
            "Saved Test Internship", true);

    createTestApplication(
            1L, savedOffer, ApplicationStatus.SAVED);

    // Student 1 applies to this offer
    JobOffer appliedOffer = createTestOffer(
            "Applied Test Internship", true);

    createTestApplication(
            1L, appliedOffer, ApplicationStatus.APPLIED);

    // This offer is inactive
    JobOffer inactiveOffer = createTestOffer(
            "Inactive Test Internship", false);

    applicationRepository.flush();

    mockMvc.perform(get("/api/job-offers/not-applied")
                    .header("X-User-Id", "1")
                    .header("X-User-Role", "STUDENT")
                    .param("page", "0")
                    .param("size", "100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath(
                    "$.content[?(@.title == 'Saved Test Internship')]")
                    .exists())
            .andExpect(jsonPath(
                    "$.content[?(@.title == 'Applied Test Internship')]")
                    .doesNotExist())
            .andExpect(jsonPath(
                    "$.content[?(@.title == 'Inactive Test Internship')]")
                    .doesNotExist());
}

@Test
void companyCannotAccessNotAppliedOffers() throws Exception {

    mockMvc.perform(get("/api/job-offers/not-applied")
                    .header("X-User-Id", "1")
                    .header("X-User-Role", "COMPANY"))
            .andExpect(status().isForbidden());
}

// Helper method for creating test job offers
private JobOffer createTestOffer(
        String title,
        boolean active) {

    JobOffer offer = new JobOffer();

    offer.setCompany(
            companyRepository.findById(1L).orElseThrow());

    offer.setTitle(title);
    offer.setDescription("Integration test position");
    offer.setJobType(JobType.INTERNSHIP);
    offer.setLocation("Sofia");
    offer.setDeadline(LocalDate.now().plusDays(30));
    offer.setActive(active);

    return jobOfferRepository.saveAndFlush(offer);
}

}
