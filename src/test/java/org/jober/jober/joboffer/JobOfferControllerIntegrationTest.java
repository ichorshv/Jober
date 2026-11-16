
package org.jober.jober.joboffer;

import org.jober.jober.JoberApplication;
import org.jober.jober.TestcontainersConfiguration;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = JoberApplication.class)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class JobOfferControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getJobOfferByIdReturns200() throws Exception {
        mockMvc.perform(get("/api/job-offers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Java Intern"))
                .andExpect(jsonPath("$.companyName")
                        .value("Acme Software"));
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
                .andExpect(jsonPath("$.title")
                        .value("Spring Boot Intern"))
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
                .andExpect(jsonPath("$.title")
                        .value("Updated Java Intern"));
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
}
