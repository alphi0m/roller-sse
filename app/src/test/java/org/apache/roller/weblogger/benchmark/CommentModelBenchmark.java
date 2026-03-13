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
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.roller.weblogger.pojos.CommentSearchCriteria;
import org.apache.roller.weblogger.pojos.WeblogEntryComment;
import org.apache.roller.weblogger.pojos.WeblogEntryComment.ApprovalStatus;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class CommentModelBenchmark {

    private WeblogEntryComment comment;
    private CommentSearchCriteria criteria;

    @Setup
    public void setup() {
        comment = new WeblogEntryComment();
        comment.setName("Mario Rossi");
        comment.setEmail("mario.rossi@example.com");
        comment.setUrl("https://example.com/user/mario");
        comment.setContent("Commento di esempio con testo realistico per benchmark.");
        comment.setRemoteHost("203.0.113.55");
        comment.setPostTime(Timestamp.from(Instant.now()));
        comment.setStatus(ApprovalStatus.APPROVED);
        comment.setNotify(Boolean.TRUE);
        comment.setContentType("text/plain");

        criteria = new CommentSearchCriteria();
    }

    @Benchmark
    public int benchmarkWeblogEntryCommentGetters() {
        int total = 0;
        total += safeLength(comment.getName());
        total += safeLength(comment.getContent());
        total += safeLength(comment.getRemoteHost());
        total += comment.getStatus().ordinal();
        total += comment.getNotify() ? 1 : 0;
        return total;
    }

    @Benchmark
    public int benchmarkCommentSearchCriteriaSettersGetters() {
        Date now = new Date();
        criteria.setSearchText("moderation");
        criteria.setStartDate(now);
        criteria.setEndDate(new Date(now.getTime() + 60_000));
        criteria.setStatus(ApprovalStatus.PENDING);
        criteria.setOffset(10);
        criteria.setMaxResults(50);
        criteria.setReverseChrono(true);

        int total = 0;
        total += safeLength(criteria.getSearchText());
        total += criteria.getStartDate() != null ? 1 : 0;
        total += criteria.getEndDate() != null ? 1 : 0;
        total += criteria.getStatus() != null ? criteria.getStatus().ordinal() : 0;
        total += criteria.getOffset();
        total += criteria.getMaxResults();
        total += criteria.isReverseChrono() ? 1 : 0;
        return total;
    }

    private static int safeLength(String value) {
        return value == null ? 0 : value.length();
    }
}
