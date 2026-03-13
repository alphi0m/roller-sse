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

import org.apache.commons.lang3.StringUtils;
import org.apache.roller.weblogger.pojos.WeblogEntryComment;

/**
 * A {@link ModerationPolicy} that flags or rejects comments containing
 * forbidden keywords.
 *
 * <p>The policy works in two tiers:</p>
 * <ol>
 *   <li><strong>Spam keywords</strong> – if any is found in the content, name,
 *       or URL of the comment, the decision is immediately {@link ModerationDecision#spam spam}.</li>
 *   <li><strong>Pending keywords</strong> – if any is found, the decision is
 *       {@link ModerationDecision#pending pending} (manual review required).</li>
 * </ol>
 *
 * <p>Comparisons are case-insensitive. Both keyword lists default to empty,
 * so the policy is a no-op until configured.</p>
 */
public class KeywordModerationPolicy implements ModerationPolicy {

        /*@ public invariant spamKeywords != null;
            @ public invariant pendingKeywords != null;
            @*/

    //@ spec_public
    private final List<String> spamKeywords;
    //@ spec_public
    private final List<String> pendingKeywords;

    // ---------------------------------------------------------------- constructors

    /**
     * Creates a policy with explicit spam and pending keyword lists.
     *
     * @param spamKeywords    words that trigger a SPAM verdict (case-insensitive)
     * @param pendingKeywords words that trigger a PENDING verdict (case-insensitive)
     */
    /*@ public normal_behavior
      @   ensures this.spamKeywords != null;
      @   ensures this.pendingKeywords != null;
      @*/
    public KeywordModerationPolicy(List<String> spamKeywords, List<String> pendingKeywords) {
        this.spamKeywords    = spamKeywords    != null ? spamKeywords    : Collections.emptyList();
        this.pendingKeywords = pendingKeywords != null ? pendingKeywords : Collections.emptyList();
    }

    /**
     * Convenience constructor that accepts comma-separated keyword strings.
     *
     * @param spamKeywordsCsv    comma-separated spam keywords, may be blank
     * @param pendingKeywordsCsv comma-separated pending keywords, may be blank
     */
    /*@ public normal_behavior
      @   ensures this.spamKeywords != null;
      @   ensures this.pendingKeywords != null;
      @*/
    public KeywordModerationPolicy(String spamKeywordsCsv, String pendingKeywordsCsv) {
        this(parseKeywords(spamKeywordsCsv), parseKeywords(pendingKeywordsCsv));
    }

    // ---------------------------------------------------------------- ModerationPolicy

        /*@ also
            @ public normal_behavior
            @   requires comment != null;
            @   ensures \result != null;
            @*/
    @Override
    public ModerationDecision evaluate(WeblogEntryComment comment) {
        String searchText = buildSearchText(comment);

        for (String keyword : spamKeywords) {
            if (keyword != null && !keyword.isEmpty() && StringUtils.containsIgnoreCase(searchText, keyword)) {
                return ModerationDecision.spam("contains spam keyword: " + keyword);
            }
        }

        for (String keyword : pendingKeywords) {
            if (keyword != null && !keyword.isEmpty() && StringUtils.containsIgnoreCase(searchText, keyword)) {
                return ModerationDecision.pending("contains flagged keyword: " + keyword);
            }
        }

        return ModerationDecision.approve();
    }

    // ---------------------------------------------------------------- helpers

    /**
     * Builds a single string from all textual fields that should be inspected.
     */
    private static String buildSearchText(WeblogEntryComment comment) {
        String content = comment.getContent();
        String name = comment.getName();
        String url = comment.getUrl();

        StringBuilder sb = new StringBuilder();
        if (content != null) {
            sb.append(content).append(' ');
        }
        if (name != null) {
            sb.append(name).append(' ');
        }
        if (url != null) {
            sb.append(url).append(' ');
        }
        return sb.toString();
    }

    private static List<String> parseKeywords(String csv) {
        if (csv == null || csv.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.asList(csv.split(","));
    }
}
