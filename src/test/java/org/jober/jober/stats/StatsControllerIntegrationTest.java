package org.jober.jober.stats;

import org.jober.jober.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class StatsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminCanFetchCompanyRankingFromSeedData() throws Exception {
        mockMvc.perform(get("/api/stats/company-ranking")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].company").exists())
                .andExpect(jsonPath("$[0].applied").exists())
                .andExpect(jsonPath("$[0].responded").exists())
                .andExpect(jsonPath("$[0].responseRate").exists())
                .andExpect(jsonPath("$[0].companyRank").exists());
    }

    @Test
    void nonAdminCannotFetchCompanyRanking() throws Exception {
        mockMvc.perform(get("/api/stats/company-ranking")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "COMPANY"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/stats/company-ranking")
                        .header("X-User-Id", "2")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isForbidden());
    }
}