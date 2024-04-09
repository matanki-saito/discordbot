package com.popush.henrietta.scheduler;

import com.popush.henrietta.biz.discussion.DiscussionFlow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class Scheduler {
    private final DiscussionFlow discussionFlow;

    private final JDA jda;

    @Scheduled(cron = "0 0 */6 * * *")
    public void reportCurrentTime() {
        try {
            discussionFlow.checkPost(jda);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
