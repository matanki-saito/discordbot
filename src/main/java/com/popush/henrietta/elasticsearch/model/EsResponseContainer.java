package com.popush.henrietta.elasticsearch.model;

import java.util.List;

import com.popush.henrietta.discord.model.BotCallCommand;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EsResponseContainer<T> {
    private BotCallCommand callCommand;
    private Long findCount;
    List<EsResponseWithData<T>> data;
}
