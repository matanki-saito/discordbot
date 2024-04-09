package com.popush.henrietta.elasticsearch;

import java.util.List;

import com.popush.henrietta.biz.project.BotCallCommand;

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
