package com.donkan.sisvetclinic.modules.clinical.infrastructure.input.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.donkan.sisvetclinic.modules.clinical.service.port.in.FollowUpUseCase;

@Component
public class FollowUpScheduler {

    private final FollowUpUseCase followUps;

    public FollowUpScheduler(FollowUpUseCase followUps) {
        this.followUps = followUps;
    }

    @Scheduled(
        cron = "${sisvet.clinical.followup-cron:0 0 * * * *}"
    )
    public void reviewPendingFollowUps() {
        followUps.reviewPendingFollowUps();
    }
}
