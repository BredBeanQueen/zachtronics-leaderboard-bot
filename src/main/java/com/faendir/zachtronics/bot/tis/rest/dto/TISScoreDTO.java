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

package com.faendir.zachtronics.bot.tis.rest.dto;

import com.faendir.zachtronics.bot.tis.model.TISScore;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
public class TISScoreDTO {
    int cycles;
    int nodes;
    int instructions;

    boolean achievement;
    boolean cheating;

    @NotNull
    public static TISScoreDTO fromScore(@NotNull TISScore score) {
        return new TISScoreDTO(score.getCycles(), score.getNodes(), score.getInstructions(), score.isAchievement(), score.isCheating());
    }
}

