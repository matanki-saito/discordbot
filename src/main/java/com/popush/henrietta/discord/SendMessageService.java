package com.popush.henrietta.discord;

import org.springframework.stereotype.Service;

import net.dv8tion.jda.api.entities.MessageChannel;

@Service
public class SendMessageService {

    public void sendPlaneMessage(MessageChannel channel, String message) {
        channel.sendMessage(message)
               .queue(response -> {

               });
    }
}
