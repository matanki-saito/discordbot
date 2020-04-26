package com.popush.henrietta.discord.states;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EsResponseWithData<T> {
    private String id;
    private T data;
}
