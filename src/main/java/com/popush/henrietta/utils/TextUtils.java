package com.popush.henrietta.utils;

import com.popush.henrietta.discord.BotListener;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@UtilityClass
public class TextUtils {

    public String last(List<String> list){
        return list.get(list.size()-1);
    }

    public Stream<String> sea(List<String> list){
        return list.subList(0,list.size()-1).stream();
    }

    /**
     * 長すぎるメッセージを分割する
     *
     * 全体を###で分割、###の位置を見てn文字間隔を超えないように分割する
     * [100,300, 200, 600, 200, 900]
     *
     * @param message 対象メッセージ
     * @param length 文字列最大長n
     * @return 分割されたメッセージ
     */
    public List<String> splitMessage(String message, int length, String splitter){
        return Arrays.stream(message.split(splitter))
                .filter(x-> !StringUtils.isEmpty(x))
                .map(x->List.of(splitter + x))
                .reduce((u, v) -> last(u).length() + last(v).length() >= length
                        ? Stream.concat(u.stream(), v.stream()).toList()
                        : Stream.concat(sea(u), Stream.of(last(u) + last(v))).toList())
                .orElseGet(List::of);
    }
}
