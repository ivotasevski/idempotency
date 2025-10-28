package com.ivotasevski.idempotency.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CompensationJob {

    @Scheduled(cron = "*/10 * * * * *")
    public void compensateStuckInProgress() {
        // TODO: Implement this
    }

    @Scheduled(cron = "*/10 * * * * *")
    public void compensateUndefined() {
        // TODO: Implement this
    }

    @Scheduled(cron = "*/10 * * * * *")
    public void resetStuckInCompensation() {
        // TODO: Implement this
    }

    @Scheduled(cron = "*/10 * * * * *")
    public void compensatePendingCompensation() {
        // TODO: Implement this
    }

}
