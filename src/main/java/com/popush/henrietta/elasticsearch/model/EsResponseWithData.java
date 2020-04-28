package com.popush.henrietta.elasticsearch.model;

import com.popush.henrietta.discord.model.BotCallCommand;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EsResponseWithData<T> {
    private BotCallCommand callCommand;
    private String id;
    private T data;
}
