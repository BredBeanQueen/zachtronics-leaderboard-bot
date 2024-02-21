/*
 * Copyright (c) 2022
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.faendir.zachtronics.bot.sz.validation;

import com.faendir.zachtronics.bot.sz.validation.chips.SzChip;
import com.faendir.zachtronics.bot.sz.validation.chips.SzChipUC;
import com.faendir.zachtronics.bot.validation.ValidationException;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Value
public class SzSave {
    private static final Pattern TRACES_REGEX = Pattern.compile("[\\.0-9A-Z]{22}\\n".repeat(14));

    @NotNull String name;
    @NotNull String puzzle;
    /** in Yen cents */
    @Nullable Integer productionCost;
    @Nullable Integer powerUsage;
    @Nullable Integer linesOfCode;

    @NotNull String traces;

    @NotNull List<SzChip> chips;

    /**
     * @param solution UNIX newlines only
     */
    @NotNull
    public static SzSave unmarshal(String solution) {
        List<String> blocks = Arrays.asList(Pattern.compile("(?<=\\n)\\n+(?=\\[|$)").split(solution));
        String metadataBlock = blocks.get(0);
        if (!metadataBlock.startsWith("[name]"))
            throw new ValidationException("Invalid solution file, first block: \"" + metadataBlock + "\"");
        Map<String, String> metadataMap = readAllTags(metadataBlock);

        String tracesBlock = blocks.get(1);
        String traces = readTag(tracesBlock, "traces");
        if (!TRACES_REGEX.matcher(traces).matches())
            throw new ValidationException("traces");

        List<String> chipBlocks = blocks.subList(2, blocks.size());
        List<SzChip> chips = chipBlocks.stream()
                                       .map(SzSave::readAllTags)
                                       .map(SzChip::unmarshal)
                                       .toList();

        return new SzSave(metadataMap.get("name"),
                          metadataMap.get("puzzle"),
                          getIntOrNull(metadataMap, "production-cost"),
                          getIntOrNull(metadataMap, "power-usage"),
                          getIntOrNull(metadataMap, "lines-of-code"),
                          traces,
                          chips);
    }


    @NotNull
    private static String readTag(@NotNull String block, @NotNull String tag) {
        if (block.startsWith("[" + tag + "]")) {
            int headerLength = tag.length() + 2;
            return block.substring(headerLength).stripLeading();
        }
        else
            throw new ValidationException("No [" + tag + "] in \"" + block + "\"");
    }

    @NotNull
    private static Map<String, String> readAllTags(@NotNull String lines) {
        String[] blocks = Pattern.compile("\\n(?:(?=\\[)|$)").split(lines);
        Map<String, String> result = new HashMap<>();
        for (String block : blocks) {
            String tag = block.substring(1, block.indexOf(']'));
            String content = block.substring(block.indexOf(']') + 1).stripLeading();
            result.put(tag, content);
        }
        return result;
    }

    /** absent = exception */
    public static int getInt(@NotNull Map<String, String> map, @NotNull String tag) {
        String candidate = map.get(tag);
        if (candidate != null)
            return Integer.parseInt(candidate);
        else
            throw new ValidationException("No [" + tag + "] in map");
    }

    /** absent = null */
    @Nullable
    public static Integer getIntOrNull(@NotNull Map<String, String> map, @NotNull String tag) {
        String candidate = map.get(tag);
        return candidate == null ? null : Integer.valueOf(candidate);
    }

    /** absent = false */
    public static boolean getBoolean(@NotNull Map<String, String> map, @NotNull String tag) {
        return Boolean.parseBoolean(map.get(tag));
    }

    public int cost() {
        if (productionCost == null)
            throw new ValidationException("Solution must be solved");

        int cost = chips.stream()
                        .mapToInt(SzChip::getCost)
                        .sum();
        if (cost * 100 != productionCost)
            throw new ValidationException("Declared cost: " + productionCost / 100 + ", effective cost: " + cost);
        return cost;
    }

    public int lines() {
        if (linesOfCode == null)
            throw new ValidationException("Solution must be solved");

        int lines = chips.stream()
                         .filter(c -> c instanceof SzChipUC)
                         .map(c -> (SzChipUC) c)
                         .mapToInt(SzChipUC::linesOfCode)
                         .sum();
        if (lines != linesOfCode)
            throw new ValidationException("Declared lines: " + linesOfCode + ", effective lines: " + lines);
        return lines;
    }
}
