package org.jober.jober.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = JoberApplication.class)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void studentLogin() throws Exception {
        login("ivan@student.example", "password123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.fullNameOrName").value("Ivan Petrov"));
    }

    @Test
    void adminLogin() throws Exception {
        login("admin@jobtracker.example", "password123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void companyLoginIsCaseInsensitive() throws Exception {
        login("HR@Acme.Example", "password123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("COMPANY"))
                .andExpect(jsonPath("$.fullNameOrName").value("Acme Software"));
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        login("ivan@student.example", "wrong-password").andExpect(status().isUnauthorized());
    }

    @Test
    void invalidLoginBodyReturns400() throws Exception {
        login("not-an-email", "").andExpect(status().isBadRequest());
    }

    @Test
    void registerStudentThenLogin() throws Exception {
        mockMvc.perform(post("/api/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "new@student.example", "password": "secret123",
                                 "fullName": "New Student", "university": "TU Sofia", "graduationYear": 2028}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.role").value("STUDENT"));

        login("new@student.example", "secret123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void registerStudentCannotBecomeAdmin() throws Exception {
        mockMvc.perform(post("/api/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "sneaky@student.example", "password": "secret123",
                                 "fullName": "Sneaky", "role": "ADMIN"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void registerCompanyThenLogin() throws Exception {
        mockMvc.perform(post("/api/auth/register/company")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "jobs@newco.example", "password": "secret123",
                                 "name": "NewCo", "location": "Varna"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("COMPANY"))
                .andExpect(jsonPath("$.fullNameOrName").value("NewCo"));

        login("jobs@newco.example", "secret123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("COMPANY"));
    }

    @Test
    void registerStudentWithCompanyEmailReturns409() throws Exception {
        mockMvc.perform(post("/api/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "hr@acme.example", "password": "secret123", "fullName": "Someone"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void registerCompanyWithStudentEmailReturns409() throws Exception {
        mockMvc.perform(post("/api/auth/register/company")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "Ivan@Student.Example", "password": "secret123", "name": "Ivan Ltd"}
                                """))
                .andExpect(status().isConflict());
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password)));
    }
}
