package org.jober.jober.stats;

import org.jober.jober.common.web.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/company-ranking")
    public List<CompanyRanking> getCompanyRanking(CurrentUser currentUser) {
        return statsService.getCompanyRanking(currentUser);
    }
}