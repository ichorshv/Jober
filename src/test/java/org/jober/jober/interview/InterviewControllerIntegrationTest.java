package org.jober.jober.interview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jober.jober.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class InterviewControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void companyCanCreateUpdateOutcomeAndOwnerStudentCanReadInterviews() throws Exception {
        String createBody = """
                {
                  "applicationId": 1,
                  "type": "TECHNICAL",
                  "scheduledAt": "2026-12-10T15:00:00",
                  "locationOrLink": "https://meet.google.com/test-link",
                  "notes": "Integration interview"
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/interviews")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "COMPANY")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outcome").value("PENDING"))
                .andExpect(jsonPath("$.applicationId").value(1))
                .andReturn();

        JsonNode createdJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long interviewId = createdJson.get("id").asLong();

        // Чужда компания не може да променя outcome -> 403
        mockMvc.perform(patch("/api/interviews/{id}/outcome", interviewId)
                        .header("X-User-Id", "2")
                        .header("X-User-Role", "COMPANY")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "outcome": "FAILED" }
                                """))
                .andExpect(status().isForbidden());

        // Собственикът компания обновява outcome на PASSED -> 200
        mockMvc.perform(patch("/api/interviews/{id}/outcome", interviewId)
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "COMPANY")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "outcome": "PASSED" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("PASSED"));

        // Собственикът студент вижда интервютата по applicationId -> 200
        mockMvc.perform(get("/api/interviews/application/{applicationId}", 1)
                        .header("X-User-Id", "2")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].outcome").value("PASSED"));

        // Друг студент няма достъп -> 403
        mockMvc.perform(get("/api/interviews/application/{applicationId}", 1)
                        .header("X-User-Id", "3")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isForbidden());
    }
}