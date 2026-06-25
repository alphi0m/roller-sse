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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.roller.weblogger.pojos.WeblogEntryComment;
import org.apache.roller.weblogger.pojos.WeblogEntryComment.ApprovalStatus;

/**
 * Orchestrates a chain of {@link ModerationPolicy} instances to produce a
 * single moderation decision for an incoming {@link WeblogEntryComment}.
 *
 * <h3>Verdict precedence</h3>
 * <p>Policies are evaluated in order, highest-severity wins:</p>
 * <pre>
 *   SPAM &gt; PENDING &gt; APPROVE
 * </pre>
 * <p>Evaluation stops as soon as a SPAM verdict is returned by any policy.</p>
 *
 * <h3>Mapping to {@link ApprovalStatus}</h3>
 * <table border="1">
 *   <tr><th>Verdict</th><th>ApprovalStatus</th></tr>
 *   <tr><td>APPROVE</td><td>APPROVED</td></tr>
 *   <tr><td>PENDING</td><td>PENDING</td></tr>
 *   <tr><td>SPAM   </td><td>SPAM</td></tr>
 * </table>
 *
 * <h3>Default instance</h3>
 * <p>A ready-to-use singleton can be obtained via {@link #getDefault()}.
 * It bundles a {@link KeywordModerationPolicy} with common spam keywords and
 * a {@link RateLimitModerationPolicy} with conservative defaults.</p>
 */
public class CommentModerationService {

        /*@ public invariant policies != null;
            @ public invariant !policies.isEmpty();
            @*/

    private static final Logger log = LoggerFactory.getLogger(CommentModerationService.class);

    // Common spam keywords bundled with the default instance.
    private static final String DEFAULT_SPAM_KEYWORDS =
            "viagra,cialis,casino,poker,lottery,free money,click here,buy now";

    private static final CommentModerationService DEFAULT_INSTANCE =
            new CommentModerationService(
                    new KeywordModerationPolicy(DEFAULT_SPAM_KEYWORDS, ""),
                    new RateLimitModerationPolicy()
            );

    // ---------------------------------------------------------------- state

    //@ spec_public
    private final List<ModerationPolicy> policies;

    // ---------------------------------------------------------------- constructors

    /**
     * Creates a service with the given ordered list of policies.
     *
     * @param policies one or more policies; must not be {@code null} or empty
     */
    /*@ public normal_behavior
      @   requires policies != null && !policies.isEmpty();
      @   ensures this.policies != null;
      @   ensures !this.policies.isEmpty();
      @ also
      @ public exceptional_behavior
      @   requires policies == null || policies.isEmpty();
      @   signals (IllegalArgumentException e) true;
      @*/
    public CommentModerationService(List<ModerationPolicy> policies) {
        if (policies == null || policies.isEmpty()) {
            throw new IllegalArgumentException("At least one ModerationPolicy is required");
        }
        this.policies = new ArrayList<>(policies);
    }

    /**
     * Varargs convenience constructor.
     *
     * @param policies one or more policies; must not be {@code null} or empty
     */
    /*@ public normal_behavior
      @   requires policies != null && policies.length > 0;
      @   ensures this.policies != null;
      @   ensures !this.policies.isEmpty();
      @*/
    public CommentModerationService(ModerationPolicy... policies) {
        this(Arrays.asList(policies));
    }

    // ---------------------------------------------------------------- public API

    /**
     * Returns the shared default instance, which includes keyword detection
     * and IP rate-limiting out of the box.
     *
     * @return the default {@link CommentModerationService}; never {@code null}
     */
    /*@ public normal_behavior
      @   ensures \result != null;
      @ pure
      @*/
    public static CommentModerationService getDefault() {
        return DEFAULT_INSTANCE;
    }

    /**
     * Evaluates the comment against all registered policies and returns the
     * most restrictive {@link ModerationDecision}.
     *
     * @param comment the comment to evaluate; must not be {@code null}
     * @return the combined moderation decision; never {@code null}
     */
    /*@ public normal_behavior
      @   requires comment != null;
      @   ensures \result != null;
      @*/
    public ModerationDecision evaluate(WeblogEntryComment comment) {
        ModerationDecision worst = ModerationDecision.approve();

        for (ModerationPolicy policy : policies) {
            ModerationDecision decision = policy.evaluate(comment);

            if (decision.getVerdict() == ModerationDecision.Verdict.SPAM) {
                log.debug("Comment rejected as SPAM by {} : {}",
                        policy.getClass().getSimpleName(), decision.getReason());
                return decision;  // short-circuit – can't get worse than SPAM
            }

            if (decision.getVerdict() == ModerationDecision.Verdict.PENDING
                    && worst.getVerdict() == ModerationDecision.Verdict.APPROVE) {
                worst = decision;
            }
        }

        return worst;
    }

    /**
     * Evaluates the comment and applies the resulting verdict to
     * {@link WeblogEntryComment#setStatus(ApprovalStatus)}, overriding only
     * when a policy flags the comment (i.e. the caller's default is preserved
     * when the decision is {@link ModerationDecision.Verdict#APPROVE}).
     *
     * @param comment the comment to screen; must not be {@code null}
     * @return the {@link ModerationDecision} that was applied
     */
    /*@ public normal_behavior
      @   requires comment != null;
      @   ensures \result != null;
      @*/
    public ModerationDecision screen(WeblogEntryComment comment) {
        ModerationDecision decision = evaluate(comment);
        String commentId = comment.getId() != null ? comment.getId() : "unknown";

        switch (decision.getVerdict()) {
            case SPAM:
                comment.setStatus(ApprovalStatus.SPAM);
                log.warn("Moderation decision for comment [{}]: SPAM \u2014 reason: {}",
                        commentId, decision.getReason());
                break;
            case PENDING:
                comment.setStatus(ApprovalStatus.PENDING);
                log.info("Moderation decision for comment [{}]: PENDING \u2014 reason: {}",
                        commentId, decision.getReason());
                break;
            case APPROVE:
            default:
                log.info("Moderation decision for comment [{}]: APPROVE \u2014 reason: {}",
                        commentId, "no policy triggered");
                // Leave the status as set by the caller.
                break;
        }

        return decision;
    }
}
