/*
 * Copyright (c) 2023
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

package com.faendir.zachtronics.bot.sz.repository;

import com.faendir.zachtronics.bot.BotTest;
import com.faendir.zachtronics.bot.reddit.RedditService;
import com.faendir.zachtronics.bot.reddit.Subreddit;
import com.faendir.zachtronics.bot.repository.CategoryRecord;
import com.faendir.zachtronics.bot.sz.model.*;
import com.faendir.zachtronics.bot.validation.ValidationException;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@BotTest
@Disabled("Massive tests only for manual testing or migrations")
public class SzManualTest {

    @Autowired
    private SzSolutionRepository repository;
    @Autowired
    private RedditService redditService;

    @Test
    public void testFullIO() throws IOException {
        for (SzPuzzle p : repository.getTrackedPuzzles()) {

            Iterable<SzRecord> records = repository.findCategoryHolders(p, true).stream()
                                                   .map(CategoryRecord::getRecord)
                                                   ::iterator;
            for (SzRecord r : records) {
                SzSubmission submission = new SzSubmission(p, r.getScore(), r.getAuthor(), r.getDisplayLink(),
                                                           Files.readString(r.getDataPath()));
                repository.submit(submission);
            }

            System.out.println("Done " + p.getDisplayName());
        }
        System.out.println("Done");
    }

    @Test
    public void rebuildRedditWiki() {
        repository.rebuildRedditLeaderboard(null);
        String page = redditService.getWikiPage(Subreddit.SHENZHEN_IO, "index")
                                   .replaceAll("file:/tmp/sz-leaderboard[0-9]+/",
                                               "https://raw.githubusercontent.com/12345ieee/shenzhenIO-leaderboard/master");
        System.out.println(page);
    }

    @Test
    public void submitSaveFolder() throws IOException {
        String author = "someGuy";

        Path savesPath = Paths.get("../shenzhenIO/other_saves/" + author);
        /*
        cp -a ../shenzhenIO/leaderboard/* src/test/resources/repositories/sz-leaderboard/
         */

        for (SzPuzzle puzzle : repository.getTrackedPuzzles()) {
            try (DirectoryStream<Path> paths = Files.newDirectoryStream(savesPath, puzzle.getId() + "*")) {
                for (Path path : paths) {
                    String data = Files.readString(path).replace("\r\n", "\n");
                    SzSubmission submission;
                    try {
                        submission = SzSubmission.fromData(data, author, null);
                    }
                    catch (Exception e) {
                        if (e instanceof ValidationException) {
                            String message = e.getMessage();
                            if (message.equals("Solution must be solved") || message.matches("Declared (?:cost|lines): .+") ||
                                message.startsWith("Unknown puzzle: "))
                                continue;
                        }
                        System.err.println(path);
                        throw e;
                    }

                    System.out.println(repository.submit(submission));
                }
            }
        }

        /*
        rm -r src/test/resources/repositories/sz-leaderboard/
        git restore --source=HEAD --staged --worktree -- src/test/resources/repositories/sz-leaderboard/
        rsync -a --delete --exclude=README.txt $(ls -1dt /tmp/sz-leaderboard* | head -n1)/* ../shenzhenIO/leaderboard/
         */

        System.out.println("Done");
    }

    @Test
    public void bootstrapPsv() throws IOException {
        Path repoPath = Paths.get("../shenzhenIO/leaderboard");

        for (SzPuzzle puzzle : repository.getTrackedPuzzles()) {
            Path indexPath = repoPath.resolve(puzzle.getGroup().getRepoFolder()).resolve(puzzle.getId()).resolve("solutions.psv");
            try (ICSVWriter writer = new CSVWriterBuilder(Files.newBufferedWriter(indexPath)).withSeparator('|').build()) {

                Map<SzScore, CategoryRecord<SzRecord, SzCategory>> scoreMap =
                        repository.findCategoryHolders(puzzle, true).stream()
                                  .collect(Collectors.toMap(cr -> cr.getRecord().getScore(),
                                                            Function.identity(),
                                                            (cr1, cr2) -> {
                                                                cr1.getCategories().addAll(cr2.getCategories());
                                                                return cr1;
                                                            },
                                                            () -> new TreeMap<>(SzCategory.CP.getScoreComparator())));

                for (CategoryRecord<SzRecord, SzCategory> cr : scoreMap.values()) {
                    SzRecord record = cr.getRecord();
                    String author = record.getAuthor();
                    String categories = cr.getCategories().stream()
                                          .map(SzCategory::name)
                                          .sorted()
                                          .collect(Collectors.joining(","));

                    String[] csvRecord = {record.getScore().toDisplayString(),
                                          author,
                                          categories};
                    writer.writeNext(csvRecord, false);
                }
            }
        }
    }
}
