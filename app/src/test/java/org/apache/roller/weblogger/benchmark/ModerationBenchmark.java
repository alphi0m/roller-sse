/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */

package org.apache.roller.weblogger.benchmark;

import java.sql.Timestamp;
import java.time.Instant;

import org.apache.roller.weblogger.moderation.CommentModerationService;
import org.apache.roller.weblogger.moderation.KeywordModerationPolicy;
import org.apache.roller.weblogger.moderation.ModerationDecision;
import org.apache.roller.weblogger.moderation.RateLimitModerationPolicy;
import org.apache.roller.weblogger.pojos.WeblogEntryComment;
import org.apache.roller.weblogger.pojos.WeblogEntryComment.ApprovalStatus;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class ModerationBenchmark {

    private WeblogEntryComment keywordComment;
    private WeblogEntryComment rateLimitComment;
    private WeblogEntryComment serviceComment;

    private KeywordModerationPolicy keywordPolicy;
    private RateLimitModerationPolicy rateLimitPolicy;
    private CommentModerationService moderationService;

    @Setup(Level.Trial)
    public void setup() {
        keywordPolicy = new KeywordModerationPolicy(
                "casino,poker,lottery,free money,buy now",
                "review,manual check,suspicious"
        );
        rateLimitPolicy = new RateLimitModerationPolicy(5, 10 * 60 * 1000L);
        moderationService = new CommentModerationService(keywordPolicy, rateLimitPolicy);

        keywordComment = createComment(
                "Marco Rossi",
                "Questo articolo è utile, ma visita il nostro casino bonus per offerte speciali.",
                "https://example.com/promozione",
                "203.0.113.10"
        );

        rateLimitComment = createComment(
                "Giulia Bianchi",
                "Grazie per il post, seguo spesso questo blog.",
                "https://example.org/profile",
                "198.51.100.7"
        );

        serviceComment = createComment(
                "Luca Verdi",
                "Contenuto apparentemente normale ma con frase buy now incorporata.",
                "https://example.net/info",
                "192.0.2.22"
        );
    }

    @Setup(Level.Iteration)
    public void resetIterationState() {
        rateLimitPolicy.resetCounters();
        serviceComment.setStatus(ApprovalStatus.APPROVED);
    }

    @Benchmark
    public ModerationDecision benchmarkKeywordModerationEvaluate() {
        return keywordPolicy.evaluate(keywordComment);
    }

    @Benchmark
    public ModerationDecision benchmarkRateLimitModerationEvaluate() {
        return rateLimitPolicy.evaluate(rateLimitComment);
    }

    @Benchmark
    public ModerationDecision benchmarkCommentModerationServiceScreen() {
        serviceComment.setStatus(ApprovalStatus.APPROVED);
        return moderationService.screen(serviceComment);
    }

    private static WeblogEntryComment createComment(String name, String content, String url, String remoteHost) {
        WeblogEntryComment comment = new WeblogEntryComment();
        comment.setName(name);
        comment.setContent(content);
        comment.setUrl(url);
        comment.setRemoteHost(remoteHost);
        comment.setEmail("user@example.com");
        comment.setPostTime(Timestamp.from(Instant.now()));
        comment.setContentType("text/plain");
        comment.setNotify(Boolean.FALSE);
        comment.setStatus(ApprovalStatus.APPROVED);
        return comment;
    }
}
