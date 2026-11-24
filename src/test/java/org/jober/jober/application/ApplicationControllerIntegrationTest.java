package org.jober.jober.application;

import org.jober.jober.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ApplicationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void studentCanListOwnApplicationsWithPagination() throws Exception {
        mockMvc.perform(get("/api/applications/me")
                        .header("X-User-Id", "2")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void studentCanCreateSavedApplicationAndDuplicateReturnsConflict() throws Exception {
        String body = """
                {
                  "jobOfferId": 3,
                  "notes": "Integration test note"
                }
                """;

        mockMvc.perform(post("/api/applications")
                        .header("X-User-Id", "2")
                        .header("X-User-Role", "STUDENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SAVED"));

        mockMvc.perform(post("/api/applications")
                        .header("X-User-Id", "2")
                        .header("X-User-Role", "STUDENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }
}