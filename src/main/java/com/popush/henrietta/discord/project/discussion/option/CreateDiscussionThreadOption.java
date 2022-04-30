package com.popush.henrietta.discord.project.discussion.option;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import com.popush.discussions.DiscussionGrpc;
import com.popush.discussions.DiscussionOuterClass.CreateDiscussionRequest;
import com.popush.henrietta.discord.exception.CommandErrorException;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@AllArgsConstructor
@Builder(toBuilder = true)
public class CreateDiscussionThreadOption implements DiscussionOption {

    private List<String> arguments;
    private MessageReceivedEvent context;

    @Override
    public boolean matchOption(String optionText) {
        return optionText.contains("d");
    }

    @Override
    public DiscussionOption makeClone(List<String> arguments, MessageReceivedEvent context) {
        return this.toBuilder()
                   .arguments(arguments)
                   .context(context)
                   .build();
    }

    @Override
    public void parseArguments() throws CommandErrorException {
        //
    }

    @Override
    public void execute() {
        log.info("grpc test");

        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 6565)
                                                      .usePlaintext()
                                                      .build();

        var stub = DiscussionGrpc.newBlockingStub(channel);

        var jda = context.getChannel().getJDA();
        var channelId = context.getChannel().getIdLong();
        var guildChannel = jda.getGuildChannelById(channelId);
        var guild = Optional.ofNullable(guildChannel).orElseThrow().getGuild();

        var request = CreateDiscussionRequest
                .newBuilder()
                .setGame("eu4")
                .setMod("jpmod")
                .setCandidates("test1,test2,test3")
                .setDiscordGuildId(guild.getIdLong())
                .build();

        var reply = stub.createDiscussion(request);

        log.info(reply.getMessage());
    }
}
