package org.jober.jober.stats;

import org.jober.jober.common.enums.UserRole;
import org.jober.jober.common.exception.ForbiddenException;
import org.jober.jober.common.web.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StatsService {

    private final StatsRepository statsRepository;

    public StatsService(StatsRepository statsRepository) {
        this.statsRepository = statsRepository;
    }

    @Transactional(readOnly = true)
    public List<CompanyRanking> getCompanyRanking(CurrentUser currentUser) {
        if (currentUser == null || currentUser.role() != UserRole.ADMIN) {
            throw new ForbiddenException("Only ADMIN can access company ranking statistics");
        }
        return statsRepository.rankCompaniesByResponsiveness();
    }
}