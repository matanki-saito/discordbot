package com.popush.henrietta.discord.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@AllArgsConstructor
@Builder
public class BotCallCommand {
    private List<String> searchWords;

    private String index;

    @Deprecated
    private String command;

    private List<String> commands;
}
