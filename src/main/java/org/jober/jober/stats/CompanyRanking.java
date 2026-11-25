package org.jober.jober.stats;

public interface CompanyRanking {
    String getCompany();
    Long getApplied();
    Long getResponded();
    Double getResponseRate();
    Double getAvgDaysToInterview();
    Long getCompanyRank();
}