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

/**
 * Represents the outcome of a moderation policy evaluation.
 *
 * <p>A decision carries a {@link Verdict} and an optional human-readable
 * reason that explains why the verdict was reached.</p>
 */
public final class ModerationDecision {

    /**
     * The possible outcomes of a moderation evaluation.
     */
    public enum Verdict {

        /** The comment is acceptable and may be published immediately. */
        APPROVE,

        /** The comment requires manual review before publication. */
        PENDING,

        /** The comment is spam and should be rejected. */
        SPAM
    }

    //@ spec_public
    private final Verdict verdict;
    //@ spec_public
    private final String  reason;

    private ModerationDecision(Verdict verdict, String reason) {
        this.verdict = verdict;
        this.reason  = reason;
    }

    // ---------------------------------------------------------------- factory

    /** Creates an APPROVE decision with no additional reason. */
    /*@ public normal_behavior
      @   ensures \result != null;
      @*/
    public static ModerationDecision approve() {
        return new ModerationDecision(Verdict.APPROVE, null);
    }

    /** Creates a PENDING decision with the supplied reason. */
    /*@ public normal_behavior
      @   requires reason != null;
      @   ensures \result != null;
      @*/
    public static ModerationDecision pending(String reason) {
        return new ModerationDecision(Verdict.PENDING, reason);
    }

    /** Creates a SPAM decision with the supplied reason. */
    /*@ public normal_behavior
      @   requires reason != null;
      @   ensures \result != null;
      @*/
    public static ModerationDecision spam(String reason) {
        return new ModerationDecision(Verdict.SPAM, reason);
    }

    // --------------------------------------------------------------- accessors

    /** Returns the verdict for this decision. */
    /*@ public normal_behavior
      @   ensures \result != null;
      @   ensures \result == verdict;
      @   pure
      @*/
    public Verdict getVerdict() {
        return verdict;
    }

    /**
     * Returns a human-readable explanation, or {@code null} when the verdict
     * is {@link Verdict#APPROVE} and no reason was recorded.
     */
    /*@ public normal_behavior
      @   ensures \result == reason;
      @   ensures (verdict == Verdict.APPROVE) ==> (\result == null);
      @   pure
      @*/
    public String getReason() {
        return reason;
    }

    /*@ also
      @ public normal_behavior
      @   ensures \result != null;
      @*/
    @Override
    public String toString() {
        return reason != null
                ? verdict + " (" + reason + ")"
                : verdict.toString();
    }
}
