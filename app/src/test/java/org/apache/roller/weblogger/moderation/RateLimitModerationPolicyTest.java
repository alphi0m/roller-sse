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

import org.apache.roller.weblogger.pojos.WeblogEntryComment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RateLimitModerationPolicy}.
 */
class RateLimitModerationPolicyTest {

    private static final String IP_A = "192.168.1.10";
    private static final String IP_B = "10.0.0.55";

    /** Policy with a soft limit of 3, very long window (tests won't expire it). */
    private RateLimitModerationPolicy policy;

    @BeforeEach
    void setUp() {
        // soft=3, hard=6; window = 1 hour – long enough to never expire during tests
        policy = new RateLimitModerationPolicy(3, 60 * 60 * 1000L);
    }

    // ---------------------------------------------------------------- helpers

    private static WeblogEntryComment commentFrom(String ip) {
        WeblogEntryComment c = new WeblogEntryComment();
        c.setRemoteHost(ip);
        return c;
    }

    private ModerationDecision.Verdict postN(String ip, int n) {
        ModerationDecision last = null;
        for (int i = 0; i < n; i++) {
            last = policy.evaluate(commentFrom(ip));
        }
        return last.getVerdict();
    }

    // ---------------------------------------------------------------- APPROVE – within limit

    @Test
    void firstComment_isApproved() {
        assertEquals(ModerationDecision.Verdict.APPROVE,
                policy.evaluate(commentFrom(IP_A)).getVerdict());
    }

    @Test
    void commentsUpToSoftLimit_areApproved() {
        // Posts 1, 2, 3 should all be APPROVE
        for (int i = 0; i < 3; i++) {
            assertEquals(ModerationDecision.Verdict.APPROVE,
                    policy.evaluate(commentFrom(IP_A)).getVerdict(),
                    "Expected APPROVE on comment #" + (i + 1));
        }
    }

    @Test
    void differentIps_doNotShareCounters() {
        // Exhaust IP_A
        postN(IP_A, 7);
        // IP_B should still be APPROVE on first post
        assertEquals(ModerationDecision.Verdict.APPROVE,
                policy.evaluate(commentFrom(IP_B)).getVerdict());
    }

    // ---------------------------------------------------------------- PENDING – soft threshold exceeded

    @Test
    void fourthComment_exceedsSoftLimit_isPending() {
        postN(IP_A, 3); // fill up to limit
        assertEquals(ModerationDecision.Verdict.PENDING,
                policy.evaluate(commentFrom(IP_A)).getVerdict());
    }

    @Test
    void pendingDecision_reasonMentionsIp() {
        postN(IP_A, 3);
        ModerationDecision d = policy.evaluate(commentFrom(IP_A));
        assertNotNull(d.getReason());
        assertTrue(d.getReason().contains(IP_A));
    }

    @Test
    void commentsJustAboveSoftLimit_arePending() {
        // comments 4, 5, 6 are between soft (3) and hard (6) → PENDING
        postN(IP_A, 3);
        for (int i = 4; i <= 6; i++) {
            assertEquals(ModerationDecision.Verdict.PENDING,
                    policy.evaluate(commentFrom(IP_A)).getVerdict(),
                    "Expected PENDING on comment #" + i);
        }
    }

    // ---------------------------------------------------------------- SPAM – hard threshold exceeded

    @Test
    void seventhComment_exceedsHardLimit_isSpam() {
        // hard limit = soft * 2 = 6; comment #7 → SPAM
        postN(IP_A, 6);
        assertEquals(ModerationDecision.Verdict.SPAM,
                policy.evaluate(commentFrom(IP_A)).getVerdict());
    }

    @Test
    void spamDecision_reasonMentionsIp() {
        postN(IP_A, 6);
        ModerationDecision d = policy.evaluate(commentFrom(IP_A));
        assertNotNull(d.getReason());
        assertTrue(d.getReason().contains(IP_A));
    }

    // ---------------------------------------------------------------- No IP

    @Test
    void nullIp_isApproved() {
        assertEquals(ModerationDecision.Verdict.APPROVE,
                policy.evaluate(commentFrom(null)).getVerdict());
    }

    @Test
    void emptyIp_isApproved() {
        assertEquals(ModerationDecision.Verdict.APPROVE,
                policy.evaluate(commentFrom("")).getVerdict());
    }

    // ---------------------------------------------------------------- resetCounters()

    @Test
    void resetCounters_clearsState_allowsFreshApproval() {
        postN(IP_A, 7); // IP_A is now SPAM territory
        policy.resetCounters();
        // After reset, first comment from same IP should be approved again
        assertEquals(ModerationDecision.Verdict.APPROVE,
                policy.evaluate(commentFrom(IP_A)).getVerdict());
    }

    // ---------------------------------------------------------------- Window expiry

    @Test
    void expiredWindow_resetsCounter_allowsFreshApproval() throws InterruptedException {
        // Very short window: 50 ms
        RateLimitModerationPolicy shortWindow = new RateLimitModerationPolicy(2, 50L);
        // Exhaust the limit
        shortWindow.evaluate(commentFrom(IP_A));
        shortWindow.evaluate(commentFrom(IP_A));
        assertEquals(ModerationDecision.Verdict.PENDING,
                shortWindow.evaluate(commentFrom(IP_A)).getVerdict());

        // Wait for the window to expire
        Thread.sleep(100);

        // Counter should have reset – first comment is APPROVE again
        assertEquals(ModerationDecision.Verdict.APPROVE,
                shortWindow.evaluate(commentFrom(IP_A)).getVerdict());
    }

    // ---------------------------------------------------------------- Constructor validation

    @Test
    void zeroMaxComments_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new RateLimitModerationPolicy(0, 1000L));
    }

    @Test
    void zeroWindowMillis_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new RateLimitModerationPolicy(5, 0L));
    }

    @Test
    void defaultConstructor_doesNotThrow() {
        assertDoesNotThrow((Executable) RateLimitModerationPolicy::new);
    }

    @Test
    void defaultConstructor_hasExpectedConstants() {
        assertEquals(5,          RateLimitModerationPolicy.DEFAULT_MAX_COMMENTS);
        assertEquals(600_000L,   RateLimitModerationPolicy.DEFAULT_WINDOW_MILLIS);
    }
}
