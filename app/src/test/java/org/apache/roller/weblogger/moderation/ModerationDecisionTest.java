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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ModerationDecision}.
 */
class ModerationDecisionTest {

    // ---------------------------------------------------------------- approve()

    @Test
    void approve_hasCorrectVerdict() {
        ModerationDecision d = ModerationDecision.approve();
        assertEquals(ModerationDecision.Verdict.APPROVE, d.getVerdict());
    }

    @Test
    void approve_reasonIsNull() {
        assertNull(ModerationDecision.approve().getReason());
    }

    @Test
    void approve_toStringContainsVerdict() {
        assertTrue(ModerationDecision.approve().toString().contains("APPROVE"));
    }

    // ---------------------------------------------------------------- pending()

    @Test
    void pending_hasCorrectVerdict() {
        ModerationDecision d = ModerationDecision.pending("flagged word");
        assertEquals(ModerationDecision.Verdict.PENDING, d.getVerdict());
    }

    @Test
    void pending_reasonIsPreserved() {
        String reason = "contains flagged keyword: bitcoin";
        assertEquals(reason, ModerationDecision.pending(reason).getReason());
    }

    @Test
    void pending_toStringContainsVerdictAndReason() {
        String s = ModerationDecision.pending("too many links").toString();
        assertTrue(s.contains("PENDING"));
        assertTrue(s.contains("too many links"));
    }

    // ---------------------------------------------------------------- spam()

    @Test
    void spam_hasCorrectVerdict() {
        assertEquals(ModerationDecision.Verdict.SPAM,
                ModerationDecision.spam("viagra").getVerdict());
    }

    @Test
    void spam_reasonIsPreserved() {
        String reason = "contains spam keyword: casino";
        assertEquals(reason, ModerationDecision.spam(reason).getReason());
    }

    @Test
    void spam_toStringContainsVerdictAndReason() {
        String s = ModerationDecision.spam("buy now").toString();
        assertTrue(s.contains("SPAM"));
        assertTrue(s.contains("buy now"));
    }

    // ---------------------------------------------------------------- Verdict enum

    @Test
    void verdictEnum_hasThreeValues() {
        assertEquals(3, ModerationDecision.Verdict.values().length);
    }

    @Test
    void verdictEnum_valuesAreApprovesPendingSpam() {
        ModerationDecision.Verdict[] vals = ModerationDecision.Verdict.values();
        assertArrayEquals(
                new ModerationDecision.Verdict[]{
                        ModerationDecision.Verdict.APPROVE,
                        ModerationDecision.Verdict.PENDING,
                        ModerationDecision.Verdict.SPAM
                },
                vals);
    }

    // ---------------------------------------------------------------- factory isolation

    @Test
    void eachApproveCallReturnsDistinctInstance() {
        ModerationDecision d1 = ModerationDecision.approve();
        ModerationDecision d2 = ModerationDecision.approve();
        assertNotSame(d1, d2);
    }

    @Test
    void pendingWithNullReason_doesNotThrow() {
        assertDoesNotThrow(() -> ModerationDecision.pending(null));
    }

    @Test
    void spamWithNullReason_doesNotThrow() {
        assertDoesNotThrow(() -> ModerationDecision.spam(null));
    }
}
