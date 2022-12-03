package com.popush.henrietta.biz.project;

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

    private List<String> commands;
}
