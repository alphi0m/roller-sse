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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.roller.weblogger.pojos.WeblogEntryComment;
import org.apache.roller.weblogger.pojos.WeblogEntryComment.ApprovalStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CommentModerationService}.
 *
 * <p>All policies used here are lightweight inline lambdas or instances of the
 * real production policies – no mocking framework is needed.</p>
 */
class CommentModerationServiceTest {

    // ---------------------------------------------------------------- helpers

    /** Policy that always approves. */
    private static final ModerationPolicy ALWAYS_APPROVE = c -> ModerationDecision.approve();

    /** Policy that always marks as pending. */
    private static final ModerationPolicy ALWAYS_PENDING = c -> ModerationDecision.pending("always pending");

    /** Policy that always marks as spam. */
    private static final ModerationPolicy ALWAYS_SPAM    = c -> ModerationDecision.spam("always spam");

    private static WeblogEntryComment cleanComment() {
        WeblogEntryComment c = new WeblogEntryComment();
        c.setContent("This is a normal comment.");
        c.setRemoteHost("1.2.3.4");
        return c;
    }

    // ---------------------------------------------------------------- evaluate()

    @Test
    void singleApprovePolicy_returnsApprove() {
        CommentModerationService svc = new CommentModerationService(ALWAYS_APPROVE);
        assertEquals(ModerationDecision.Verdict.APPROVE,
                svc.evaluate(cleanComment()).getVerdict());
    }

    @Test
    void singlePendingPolicy_returnsPending() {
        CommentModerationService svc = new CommentModerationService(ALWAYS_PENDING);
        assertEquals(ModerationDecision.Verdict.PENDING,
                svc.evaluate(cleanComment()).getVerdict());
    }

    @Test
    void singleSpamPolicy_returnsSpam() {
        CommentModerationService svc = new CommentModerationService(ALWAYS_SPAM);
        assertEquals(ModerationDecision.Verdict.SPAM,
                svc.evaluate(cleanComment()).getVerdict());
    }

    @Test
    void spamTakesPrecedenceOverPending_inPipeline() {
        // PENDING first, then SPAM – SPAM must win
        CommentModerationService svc =
                new CommentModerationService(ALWAYS_PENDING, ALWAYS_SPAM);
        assertEquals(ModerationDecision.Verdict.SPAM,
                svc.evaluate(cleanComment()).getVerdict());
    }

    @Test
    void spamShortCircuits_remainingPoliciesNotEvaluated() {
        // Policy after SPAM should never run (verified via side-effect counter).
        int[] callCount = {0};
        ModerationPolicy countingPolicy = c -> {
            callCount[0]++;
            return ModerationDecision.approve();
        };

        CommentModerationService svc =
                new CommentModerationService(ALWAYS_SPAM, countingPolicy);
        svc.evaluate(cleanComment());

        assertEquals(0, callCount[0], "Policy after SPAM should not be called");
    }

    @Test
    void pendingTakesPrecedenceOverApprove() {
        CommentModerationService svc =
                new CommentModerationService(ALWAYS_APPROVE, ALWAYS_PENDING);
        assertEquals(ModerationDecision.Verdict.PENDING,
                svc.evaluate(cleanComment()).getVerdict());
    }

    @Test
    void allApprove_returnsApprove() {
        CommentModerationService svc =
                new CommentModerationService(ALWAYS_APPROVE, ALWAYS_APPROVE, ALWAYS_APPROVE);
        assertEquals(ModerationDecision.Verdict.APPROVE,
                svc.evaluate(cleanComment()).getVerdict());
    }

    @Test
    void evaluate_neverReturnsNull() {
        CommentModerationService svc = new CommentModerationService(ALWAYS_APPROVE);
        assertNotNull(svc.evaluate(cleanComment()));
    }

    // ---------------------------------------------------------------- screen() – status side-effect

    @Test
    void screen_spamVerdict_setsCommentStatusToSpam() {
        CommentModerationService svc = new CommentModerationService(ALWAYS_SPAM);
        WeblogEntryComment c = cleanComment();
        svc.screen(c);
        assertEquals(ApprovalStatus.SPAM, c.getStatus());
    }

    @Test
    void screen_pendingVerdict_setsCommentStatusToPending() {
        CommentModerationService svc = new CommentModerationService(ALWAYS_PENDING);
        WeblogEntryComment c = cleanComment();
        svc.screen(c);
        assertEquals(ApprovalStatus.PENDING, c.getStatus());
    }

    @Test
    void screen_approveVerdict_doesNotChangeStatus() {
        CommentModerationService svc = new CommentModerationService(ALWAYS_APPROVE);
        WeblogEntryComment c = cleanComment();
        // Roller defaults status to APPROVED; we explicitly check it is not overwritten
        ApprovalStatus original = c.getStatus();
        svc.screen(c);
        assertEquals(original, c.getStatus());
    }

    @Test
    void screen_returnsDecision() {
        CommentModerationService svc = new CommentModerationService(ALWAYS_SPAM);
        ModerationDecision d = svc.screen(cleanComment());
        assertNotNull(d);
        assertEquals(ModerationDecision.Verdict.SPAM, d.getVerdict());
    }

    // ---------------------------------------------------------------- constructor validation

    @Test
    void emptyPolicyList_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new CommentModerationService(Collections.emptyList()));
    }

    @Test
    void nullPolicyList_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new CommentModerationService((List<ModerationPolicy>) null));
    }

    // ---------------------------------------------------------------- getDefault()

    @Test
    void getDefault_returnsNonNull() {
        assertNotNull(CommentModerationService.getDefault());
    }

    @Test
    void getDefault_returnsSameInstance() {
        assertSame(CommentModerationService.getDefault(),
                   CommentModerationService.getDefault());
    }

    @Test
    void getDefault_spamKeyword_isDetected() {
        WeblogEntryComment c = new WeblogEntryComment();
        c.setContent("Buy Viagra now – cheap pills!");
        c.setRemoteHost("5.6.7.8");
        ModerationDecision d = CommentModerationService.getDefault().evaluate(c);
        assertEquals(ModerationDecision.Verdict.SPAM, d.getVerdict());
    }

    @Test
    void getDefault_cleanComment_isApproved() {
        WeblogEntryComment c = new WeblogEntryComment();
        c.setContent("Really enjoyed reading this article.");
        c.setRemoteHost("5.6.7.9");
        ModerationDecision d = CommentModerationService.getDefault().evaluate(c);
        assertEquals(ModerationDecision.Verdict.APPROVE, d.getVerdict());
    }

    // ---------------------------------------------------------------- Integration with real policies

    @Test
    void realPolicies_keywordSpam_shortCircuitsRateLimit() {
        // Even if rate limit would say PENDING, a spam keyword should give SPAM
        RateLimitModerationPolicy ratePolicy = new RateLimitModerationPolicy(1, 60_000L);
        KeywordModerationPolicy keywordPolicy = new KeywordModerationPolicy("badword", "");

        CommentModerationService svc =
                new CommentModerationService(keywordPolicy, ratePolicy);

        WeblogEntryComment c = new WeblogEntryComment();
        c.setContent("this comment contains badword");
        c.setRemoteHost("9.9.9.9");

        assertEquals(ModerationDecision.Verdict.SPAM, svc.evaluate(c).getVerdict());
    }

    @Test
    void realPolicies_rateExceeded_pendingForCleanComment() {
        // Soft limit = 1, so second comment should be PENDING
        RateLimitModerationPolicy ratePolicy = new RateLimitModerationPolicy(1, 60_000L);
        CommentModerationService svc = new CommentModerationService(ratePolicy);

        WeblogEntryComment c = new WeblogEntryComment();
        c.setContent("Normal clean comment");
        c.setRemoteHost("8.8.8.8");

        svc.evaluate(c);  // first  → APPROVE
        ModerationDecision second = svc.evaluate(c); // second → PENDING

        assertEquals(ModerationDecision.Verdict.PENDING, second.getVerdict());
    }
}
