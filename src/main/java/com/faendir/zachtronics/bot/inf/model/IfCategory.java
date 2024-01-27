/*
 * Copyright (c) 2024
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

package com.faendir.zachtronics.bot.inf.model;

import com.faendir.zachtronics.bot.model.CategoryJava;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.faendir.zachtronics.bot.inf.model.IfMetric.*;
import static com.faendir.zachtronics.bot.inf.model.IfType.STANDARD;

@Getter
public enum IfCategory implements CategoryJava<IfCategory, IfScore, IfMetric> {
    CF("CF", List.of(CYCLES, FOOTPRINT, BLOCKS), ANY_FLAG, 0b100),
    CB("CB", List.of(CYCLES, BLOCKS, FOOTPRINT), ANY_FLAG, 0b100),
    CFNG("CFNG", List.of(CYCLES, NO_GRA, FOOTPRINT, BLOCKS), NO_GRA, 0b100),
    CBNG("CBNG", List.of(CYCLES, NO_GRA, BLOCKS, FOOTPRINT), NO_GRA, 0b100),

    FC("FC", List.of(FOOTPRINT, CYCLES, BLOCKS), ANY_FLAG, 0b010),
    FB("FB", List.of(FOOTPRINT, BLOCKS, CYCLES), ANY_FLAG, 0b010),
    FIC("FIC", List.of(FOOTPRINT, INBOUNDS, CYCLES, BLOCKS), INBOUNDS, 0b010),
    FIB("FIB", List.of(FOOTPRINT, INBOUNDS, BLOCKS, CYCLES), INBOUNDS, 0b010),

    BC("BC", List.of(BLOCKS, CYCLES, FOOTPRINT), ANY_FLAG, 0b001),
    BF("BF", List.of(BLOCKS, FOOTPRINT, CYCLES), ANY_FLAG, 0b001),
    BNC("BNC", List.of(BLOCKS, NO_FLAGS, CYCLES, FOOTPRINT), NO_FLAGS, 0b001),
    BNF("BNF", List.of(BLOCKS, NO_FLAGS, FOOTPRINT, CYCLES), NO_FLAGS, 0b001);

    /** contains <tt>%d%s%d%s%d%s</tt> plus a bunch of <tt>*</tt> most likely */
    static final String[] FORMAT_STRINGS = {"%d%s%d%s%d%s", "%d%s%d%s**%d**%s", "%d%s**%d**%s%d%s", null, "**%d**%s%d%s%d%s"};

    private final String displayName;
    private final List<IfMetric> metrics;
    private final Comparator<IfScore> scoreComparator;
    private final IfMetric eligibility;
    private final Set<IfType> supportedTypes;
    private final int scoreFormatId;

    IfCategory(String displayName, @NotNull List<IfMetric> metrics, @NotNull IfMetric eligibility, int scoreFormatId) {
        this.displayName = displayName;
        this.metrics = metrics;
        this.scoreComparator = makeCategoryComparator(metrics);
        this.eligibility = eligibility;
        this.supportedTypes = (eligibility == INFINITE || eligibility == NO_FLAGS) ? Collections.singleton(STANDARD)
                                                                                   : EnumSet.allOf(IfType.class);
        this.scoreFormatId = scoreFormatId;
    }

    @Override
    public boolean supportsScore(@NotNull IfScore score) {
        return !((Boolean) eligibility.getExtract().apply(score));
    }
}
