
package org.jober.jober.company;

import org.jober.jober.JoberApplication;
import org.jober.jober.TestcontainersConfiguration;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = JoberApplication.class)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class CompanyControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getAllCompaniesReturns200() throws Exception {
        mockMvc.perform(get("/api/companies")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getCompanyByIdReturns200() throws Exception {
        mockMvc.perform(get("/api/companies/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name")
                        .value("Acme Software"));
    }

    @Test
    void adminCanCreateCompany() throws Exception {
        String body = """
                {
                  "name": "Integration Test Company",
                  "website": "https://integration.example",
                  "industry": "Technology",
                  "location": "Sofia",
                  "email": "integration-company@example.com",
                  "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/companies")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name")
                        .value("Integration Test Company"))
                .andExpect(jsonPath("$.email")
                        .value("integration-company@example.com"))
                .andExpect(jsonPath("$.passwordHash")
                        .doesNotExist());
    }

    @Test
    void studentCannotCreateCompany() throws Exception {
        String body = """
                {
                  "name": "Unauthorized Company",
                  "email": "unauthorized@example.com",
                  "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/companies")
                        .header("X-User-Id", "2")
                        .header("X-User-Role", "STUDENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void companyCanUpdateOwnProfile() throws Exception {
        String body = """
                {
                  "name": "Acme Software Updated",
                  "website": "https://acme.example",
                  "industry": "Software",
                  "location": "Sofia",
                  "email": "hr@acme.example"
                }
                """;

        mockMvc.perform(put("/api/companies/me")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "COMPANY")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name")
                        .value("Acme Software Updated"));
    }
}
