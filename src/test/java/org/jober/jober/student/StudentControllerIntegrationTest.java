package org.jober.jober.student;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

@SpringBootTest(classes = JoberApplication.class)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class StudentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void studentGetsOwnProfileWithoutPasswordHash() throws Exception {
        mockMvc.perform(get("/api/students/me").header("X-User-Id", "2").header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ivan@student.example"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void missingHeadersReturn401() throws Exception {
        mockMvc.perform(get("/api/students/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void studentUpdatesOwnProfile() throws Exception {
        mockMvc.perform(put("/api/students/me").header("X-User-Id", "2").header("X-User-Role", "STUDENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "ivan@student.example", "fullName": "Ivan P. Petrov",
                                 "university": "Sofia University", "major": "AI", "graduationYear": 2028}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Ivan P. Petrov"))
                .andExpect(jsonPath("$.major").value("AI"));
    }

    @Test
    void updateWithInvalidBodyReturns400() throws Exception {
        mockMvc.perform(put("/api/students/me").header("X-User-Id", "2").header("X-User-Role", "STUDENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "bad", "fullName": "", "graduationYear": 1990}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.graduationYear").exists());
    }

    @Test
    void adminSearchesStudents() throws Exception {
        mockMvc.perform(get("/api/students").param("q", "sofia university")
                        .header("X-User-Id", "1").header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void studentCannotListStudents() throws Exception {
        mockMvc.perform(get("/api/students").header("X-User-Id", "2").header("X-User-Role", "STUDENT"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminGetsAndDeletesStudent() throws Exception {
        mockMvc.perform(get("/api/students/3").header("X-User-Id", "1").header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Maria Georgieva"));

        mockMvc.perform(delete("/api/students/3").header("X-User-Id", "1").header("X-User-Role", "ADMIN"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/students/3").header("X-User-Id", "1").header("X-User-Role", "ADMIN"))
                .andExpect(status().isNotFound());
    }
}
