package com.popush.henrietta.discord.states;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ParatranzAggregationReport {
    @Singular
    private List<PercentItem> percentItems;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PercentItem {
        private Long lengthBegin;
        private Long lengthEnd;
        private Long allCount;
        private Long translatedItemCount;
        private Double translatedItemPercent;
    }
}
