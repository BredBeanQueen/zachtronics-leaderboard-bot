package com.faendir.zachtronics.bot.archive;

import com.faendir.zachtronics.bot.model.Solution;

import java.io.IOException;

/** Interface for classes that handle archival data for a level in the respective repository */
public interface SolutionsIndex<T extends Solution> {

    /**
     * @return whether the frontier changed at all
     */
    boolean add(T solution) throws IOException;
}