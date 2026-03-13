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

package org.apache.roller.weblogger.moderation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.roller.weblogger.pojos.WeblogEntryComment;

/**
 * A {@link ModerationPolicy} that limits the number of comments allowed from
 * the same IP address within a given time window.
 *
 * <p>When the number of attempts from a single IP exceeds
 * {@code maxCommentsPerWindow} within {@code windowMillis} milliseconds:</p>
 * <ul>
 *   <li>Verdicts alternate between {@link ModerationDecision#pending pending}
 *       (soft threshold) and {@link ModerationDecision#spam spam}
 *       (hard threshold, double the soft limit), giving administrators a
 *       chance to review potential abusers before outright blocking.</li>
 * </ul>
 *
 * <p>Counters are kept in-memory and are intentionally lightweight; they are
 * not persisted across restarts. The implementation is thread-safe.</p>
 */
public class RateLimitModerationPolicy implements ModerationPolicy {

        /*@ public invariant maxCommentsPerWindow >= 1;
            @ public invariant windowMillis >= 1;
            @*/

    /** Default soft limit: comments per window before requiring manual review. */
    public static final int DEFAULT_MAX_COMMENTS = 5;

    /** Default time window: 10 minutes in milliseconds. */
    public static final long DEFAULT_WINDOW_MILLIS = 10L * 60 * 1000;

    //@ spec_public
    private final int  maxCommentsPerWindow;
    //@ spec_public
    private final long windowMillis;

    /** Holds per-IP state: (count, windowStartTime). */
    //@ spec_public
    private final Map<String, long[]> ipCounters = new ConcurrentHashMap<>();

    // ---------------------------------------------------------------- constructors

    /**
     * Creates a policy with custom soft limit and time window.
     *
     * @param maxCommentsPerWindow max comments from the same IP within the window
     *                             before a PENDING verdict is issued
     * @param windowMillis         length of the sliding window in milliseconds
     */
    /*@ public normal_behavior
      @   requires maxCommentsPerWindow >= 1;
      @   requires windowMillis >= 1;
      @   ensures this.maxCommentsPerWindow == maxCommentsPerWindow;
      @   ensures this.windowMillis == windowMillis;
      @ also
      @ public exceptional_behavior
      @   requires maxCommentsPerWindow < 1 || windowMillis < 1;
      @   signals (IllegalArgumentException e) true;
      @*/
    public RateLimitModerationPolicy(int maxCommentsPerWindow, long windowMillis) {
        if (maxCommentsPerWindow < 1) {
            throw new IllegalArgumentException("maxCommentsPerWindow must be >= 1");
        }
        if (windowMillis < 1) {
            throw new IllegalArgumentException("windowMillis must be >= 1");
        }
        this.maxCommentsPerWindow = maxCommentsPerWindow;
        this.windowMillis         = windowMillis;
    }

    /** Creates a policy with default limits. */
    /*@ public normal_behavior
      @   ensures maxCommentsPerWindow == DEFAULT_MAX_COMMENTS;
      @   ensures windowMillis == DEFAULT_WINDOW_MILLIS;
      @*/
    public RateLimitModerationPolicy() {
        this(DEFAULT_MAX_COMMENTS, DEFAULT_WINDOW_MILLIS);
    }

    // ---------------------------------------------------------------- ModerationPolicy

        /*@ also
            @ public normal_behavior
      @   requires comment != null;
      @   ensures \result != null;
      @*/
    @Override
    public ModerationDecision evaluate(WeblogEntryComment comment) {
        String ip = comment.getRemoteHost();
        if (ip == null || ip.isEmpty()) {
            // No IP available – cannot rate-limit, let through.
            return ModerationDecision.approve();
        }

        long now   = System.currentTimeMillis();
        int  count = incrementAndGet(ip, now);

        if (count > maxCommentsPerWindow * 2) {
            return ModerationDecision.spam(
                    "rate limit exceeded (hard): " + count + " comments from " + ip);
        }
        if (count > maxCommentsPerWindow) {
            return ModerationDecision.pending(
                    "rate limit exceeded (soft): " + count + " comments from " + ip);
        }

        return ModerationDecision.approve();
    }

    // ---------------------------------------------------------------- helpers

    /**
     * Increments the counter for the given IP within the current window and
     * returns the updated count. Resets the window when it has expired.
     */
    private int incrementAndGet(String ip, long now) {
        // long[0] = count, long[1] = window start time
        long[] state = ipCounters.compute(ip, (key, existing) -> {
            if (existing == null || (now - existing[1]) > windowMillis) {
                // New window
                return new long[]{ 1L, now };
            }
            existing[0]++;
            return existing;
        });
        return (int) state[0];
    }

    /**
     * Resets all in-memory counters. Useful for testing.
     */
    /*@ public normal_behavior
      @   assignable ipCounters;
      @   ensures ipCounters.isEmpty();
      @*/
    public void resetCounters() {
        ipCounters.clear();
    }
}
