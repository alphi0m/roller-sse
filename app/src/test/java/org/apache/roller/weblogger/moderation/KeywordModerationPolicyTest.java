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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link KeywordModerationPolicy}.
 */
class KeywordModerationPolicyTest {

    // ---------------------------------------------------------------- helpers

    /** Returns a plain comment with the given content, no name or URL. */
    private static WeblogEntryComment commentWithContent(String content) {
        WeblogEntryComment c = new WeblogEntryComment();
        c.setContent(content);
        return c;
    }

    private static WeblogEntryComment commentWithName(String name) {
        WeblogEntryComment c = new WeblogEntryComment();
        c.setName(name);
        return c;
    }

    private static WeblogEntryComment commentWithUrl(String url) {
        WeblogEntryComment c = new WeblogEntryComment();
        c.setUrl(url);
        return c;
    }

    private static WeblogEntryComment emptyComment() {
        return new WeblogEntryComment();
    }

    // ---------------------------------------------------------------- APPROVE – normal comments

    @Test
    void normalComment_noKeywords_isApproved() {
        KeywordModerationPolicy policy = new KeywordModerationPolicy("", "");
        ModerationDecision d = policy.evaluate(commentWithContent("Great post!"));
        assertEquals(ModerationDecision.Verdict.APPROVE, d.getVerdict());
    }

    @Test
    void noKeywordsConfigured_alwaysApprove() {
        KeywordModerationPolicy policy = new KeywordModerationPolicy(
                Collections.emptyList(), Collections.emptyList());
        assertEquals(ModerationDecision.Verdict.APPROVE,
                policy.evaluate(commentWithContent("buy now casino viagra")).getVerdict());
    }

    @Test
    void emptyComment_allFieldsNull_isApproved() {
        KeywordModerationPolicy policy = new KeywordModerationPolicy("spam,viagra", "review");
        assertEquals(ModerationDecision.Verdict.APPROVE,
                policy.evaluate(emptyComment()).getVerdict());
    }

    // ---------------------------------------------------------------- SPAM

    @Test
    void spamKeywordInContent_isSpam() {
        KeywordModerationPolicy policy = new KeywordModerationPolicy("viagra", "");
        assertEquals(ModerationDecision.Verdict.SPAM,
                policy.evaluate(commentWithContent("Buy Viagra now!")).getVerdict());
    }

    @Test
    void spamKeyword_caseInsensitive() {
        KeywordModerationPolicy policy = new KeywordModerationPolicy("CASINO", "");
        assertEquals(ModerationDecision.Verdict.SPAM,
                policy.evaluate(commentWithContent("visit casino.com")).getVerdict());
    }

    @Test
    void spamKeywordInName_isSpam() {
        KeywordModerationPolicy policy = new KeywordModerationPolicy("spammer", "");
        assertEquals(ModerationDecision.Verdict.SPAM,
                policy.evaluate(commentWithName("spammer123")).getVerdict());
    }

    @Test
    void spamKeywordInUrl_isSpam() {
        KeywordModerationPolicy policy = new KeywordModerationPolicy("poker", "");
        assertEquals(ModerationDecision.Verdict.SPAM,
                policy.evaluate(commentWithUrl("http://best-poker-site.com")).getVerdict());
    }

    @Test
    void spamDecision_reasonMentionsKeyword() {
        KeywordModerationPolicy policy = new KeywordModerationPolicy("lottery", "");
        ModerationDecision d = policy.evaluate(commentWithContent("win the lottery"));
        assertNotNull(d.getReason());
        assertTrue(d.getReason().contains("lottery"));
    }

    @Test
    void multipleSpamKeywords_firstMatchReturnsSpam() {
        KeywordModerationPolicy policy = new KeywordModerationPolicy("bad,ugly", "");
        assertEquals(ModerationDecision.Verdict.SPAM,
                policy.evaluate(commentWithContent("this is bad")).getVerdict());
    }

    // ---------------------------------------------------------------- PENDING

    @Test
    void pendingKeywordInContent_isPending() {
        KeywordModerationPolicy policy = new KeywordModerationPolicy("", "review,check");
        assertEquals(ModerationDecision.Verdict.PENDING,
                policy.evaluate(commentWithContent("please review this")).getVerdict());
    }

    @Test
    void pendingKeyword_caseInsensitive() {
        KeywordModerationPolicy policy = new KeywordModerationPolicy("", "LINK");
        assertEquals(ModerationDecision.Verdict.PENDING,
                policy.evaluate(commentWithContent("click the link")).getVerdict());
    }

    @Test
    void pendingDecision_reasonMentionsKeyword() {
        KeywordModerationPolicy policy = new KeywordModerationPolicy("", "flagged");
        ModerationDecision d = policy.evaluate(commentWithContent("this is flagged"));
        assertNotNull(d.getReason());
        assertTrue(d.getReason().contains("flagged"));
    }

    // ---------------------------------------------------------------- Precedence: SPAM wins over PENDING

    @Test
    void spamTakesPrecedenceOverPending() {
        KeywordModerationPolicy policy = new KeywordModerationPolicy("virus", "link");
        WeblogEntryComment c = new WeblogEntryComment();
        c.setContent("click the link, also download this virus");
        assertEquals(ModerationDecision.Verdict.SPAM, policy.evaluate(c).getVerdict());
    }

    // ---------------------------------------------------------------- CSV constructor

    @Test
    void csvConstructor_parsesMultipleSpamKeywords() {
        KeywordModerationPolicy policy = new KeywordModerationPolicy("spam,junk,malware", "");
        assertEquals(ModerationDecision.Verdict.SPAM,
                policy.evaluate(commentWithContent("full of junk here")).getVerdict());
    }

    @Test
    void csvConstructor_blankCsv_treatedAsEmpty() {
        KeywordModerationPolicy policy = new KeywordModerationPolicy("   ", "   ");
        assertEquals(ModerationDecision.Verdict.APPROVE,
                policy.evaluate(commentWithContent("random text")).getVerdict());
    }

    @Test
    void nullLists_treatedAsEmpty_noNpe() {
        assertDoesNotThrow(() -> {
            KeywordModerationPolicy policy = new KeywordModerationPolicy(
                    (List<String>) null, (List<String>) null);
            policy.evaluate(commentWithContent("hello world"));
        });
    }
}
