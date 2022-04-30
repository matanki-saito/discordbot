package com.popush.henrietta.discord.service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import com.popush.henrietta.discord.project.Project;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class BotCommandService {

    private final List<Project> projects;

    private final Pattern pattern = Pattern.compile("^([a-zA-Z0-9\\-_]{1,4})::(.*)");

    public Optional<Project> prepareProject(MessageReceivedEvent event) {
        // botの投稿は無視する
        if (event.getAuthor().isBot()) {
            return Optional.empty();
        }

        var message = event.getMessage().getContentRaw();
        var m = pattern.matcher(message);
        if (!m.find()) {
            return Optional.empty();
        }

        var projectName = m.group(1).toLowerCase();
        var request = m.group(2).toLowerCase();

        var project = projects.stream().filter(x -> x.getProjectName().equals(projectName)).findAny();
        if (project.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(project.get().makeClone(request, event));
    }
}
