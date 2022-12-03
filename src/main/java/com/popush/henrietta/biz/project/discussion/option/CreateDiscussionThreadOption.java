package com.popush.henrietta.biz.project.discussion.option;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import com.popush.discussions.DiscussionGrpc;
import com.popush.discussions.DiscussionOuterClass.CreateDiscussionRequest;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Scope("prototype")
public class CreateDiscussionThreadOption implements DiscussionOption {

    private List<String> arguments;
    private Long guildId;

    @Override
    public boolean parseRequest(String request, MessageReceivedEvent context) {
        var requests = List.of(request.split(" "));

        if (!requests.get(0).toLowerCase(Locale.ROOT).equals("d")) {
            return false;
        }

        if (requests.size() < 3) {
            return false;
        }

        context.getChannel().getJDA();
        var channelId = context.getChannel().getIdLong();
        var guildChannel = context.getJDA().getGuildChannelById(channelId);
        var guild = Optional.ofNullable(guildChannel).orElseThrow().getGuild();
        guildId = guild.getIdLong();

        return true;
    }

    @Override
    public void execute() {
        log.info("grpc test");

        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 6565)
                                                      .usePlaintext()
                                                      .build();

        var stub = DiscussionGrpc.newBlockingStub(channel);

        var request = CreateDiscussionRequest
                .newBuilder()
                .setGame(arguments.get(0))
                .setMod(arguments.get(1))
                .setCandidates(arguments.get(2))
                .setDiscordGuildId(guildId)
                .build();

        var reply = stub.createDiscussion(request);

        log.info(reply.getMessage());
    }
}
