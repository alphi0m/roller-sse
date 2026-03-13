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

package org.apache.roller.weblogger.pojos;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CommentSearchCriteria}.
 *
 * <p>Covers: default values, all setter/getter pairs, date range semantics,
 * status filter, paging fields (offset, maxResults), and flag combinations.</p>
 */
class CommentSearchCriteriaTest {

    private CommentSearchCriteria csc;

    @BeforeEach
    void setUp() {
        csc = new CommentSearchCriteria();
    }

    // ---------------------------------------------------------------- Default values

    @Test
    void defaultWeblog_isNull() {
        assertNull(csc.getWeblog());
    }

    @Test
    void defaultEntry_isNull() {
        assertNull(csc.getEntry());
    }

    @Test
    void defaultSearchText_isNull() {
        assertNull(csc.getSearchText());
    }

    @Test
    void defaultStartDate_isNull() {
        assertNull(csc.getStartDate());
    }

    @Test
    void defaultEndDate_isNull() {
        assertNull(csc.getEndDate());
    }

    @Test
    void defaultStatus_isNull() {
        assertNull(csc.getStatus());
    }

    @Test
    void defaultReverseChrono_isFalse() {
        assertFalse(csc.isReverseChrono());
    }

    @Test
    void defaultOffset_isZero() {
        assertEquals(0, csc.getOffset());
    }

    @Test
    void defaultMaxResults_isMinusOne() {
        // -1 conventionally means "no limit"
        assertEquals(-1, csc.getMaxResults());
    }

    // ---------------------------------------------------------------- Weblog field

    @Test
    void setAndGetWeblog() {
        Weblog weblog = new Weblog();
        csc.setWeblog(weblog);
        assertSame(weblog, csc.getWeblog());
    }

    @Test
    void setWeblog_nullClearsField() {
        csc.setWeblog(new Weblog());
        csc.setWeblog(null);
        assertNull(csc.getWeblog());
    }

    // ---------------------------------------------------------------- Entry field

    @Test
    void setAndGetEntry() {
        WeblogEntry entry = new WeblogEntry();
        csc.setEntry(entry);
        assertSame(entry, csc.getEntry());
    }

    @Test
    void setEntry_nullClearsField() {
        csc.setEntry(new WeblogEntry());
        csc.setEntry(null);
        assertNull(csc.getEntry());
    }

    // ---------------------------------------------------------------- Search text

    @Test
    void setAndGetSearchText() {
        csc.setSearchText("hello world");
        assertEquals("hello world", csc.getSearchText());
    }

    @Test
    void setSearchText_emptyStringIsAccepted() {
        csc.setSearchText("");
        assertEquals("", csc.getSearchText());
    }

    @Test
    void setSearchText_nullClearsField() {
        csc.setSearchText("something");
        csc.setSearchText(null);
        assertNull(csc.getSearchText());
    }

    // ---------------------------------------------------------------- Date range

    @Test
    void setAndGetStartDate() {
        Date d = new Date(1_000_000L);
        csc.setStartDate(d);
        assertEquals(d, csc.getStartDate());
    }

    @Test
    void setAndGetEndDate() {
        Date d = new Date(9_000_000L);
        csc.setEndDate(d);
        assertEquals(d, csc.getEndDate());
    }

    @Test
    void dateRange_startBeforeEnd_isAccepted() {
        Date start = new Date(1_000L);
        Date end   = new Date(5_000L);
        csc.setStartDate(start);
        csc.setEndDate(end);
        assertTrue(csc.getStartDate().before(csc.getEndDate()));
    }

    @Test
    void dateRange_equalStartAndEnd_isAccepted() {
        Date d = new Date(3_000L);
        csc.setStartDate(d);
        csc.setEndDate(d);
        assertEquals(csc.getStartDate(), csc.getEndDate());
    }

    @Test
    void setStartDate_nullClearsField() {
        csc.setStartDate(new Date());
        csc.setStartDate(null);
        assertNull(csc.getStartDate());
    }

    @Test
    void setEndDate_nullClearsField() {
        csc.setEndDate(new Date());
        csc.setEndDate(null);
        assertNull(csc.getEndDate());
    }

    // ---------------------------------------------------------------- Status filter

    @Test
    void setAndGetStatus_approved() {
        csc.setStatus(WeblogEntryComment.ApprovalStatus.APPROVED);
        assertEquals(WeblogEntryComment.ApprovalStatus.APPROVED, csc.getStatus());
    }

    @Test
    void setAndGetStatus_spam() {
        csc.setStatus(WeblogEntryComment.ApprovalStatus.SPAM);
        assertEquals(WeblogEntryComment.ApprovalStatus.SPAM, csc.getStatus());
    }

    @Test
    void setAndGetStatus_pending() {
        csc.setStatus(WeblogEntryComment.ApprovalStatus.PENDING);
        assertEquals(WeblogEntryComment.ApprovalStatus.PENDING, csc.getStatus());
    }

    @Test
    void setAndGetStatus_disapproved() {
        csc.setStatus(WeblogEntryComment.ApprovalStatus.DISAPPROVED);
        assertEquals(WeblogEntryComment.ApprovalStatus.DISAPPROVED, csc.getStatus());
    }

    @Test
    void setStatus_nullMeansAnyStatus() {
        csc.setStatus(WeblogEntryComment.ApprovalStatus.APPROVED);
        csc.setStatus(null);
        assertNull(csc.getStatus());
    }

    // ---------------------------------------------------------------- reverseChrono flag

    @Test
    void setReverseChrono_true() {
        csc.setReverseChrono(true);
        assertTrue(csc.isReverseChrono());
    }

    @Test
    void setReverseChrono_false() {
        csc.setReverseChrono(true);
        csc.setReverseChrono(false);
        assertFalse(csc.isReverseChrono());
    }

    // ---------------------------------------------------------------- offset

    @Test
    void setAndGetOffset() {
        csc.setOffset(20);
        assertEquals(20, csc.getOffset());
    }

    @Test
    void setOffset_zero() {
        csc.setOffset(0);
        assertEquals(0, csc.getOffset());
    }

    @Test
    void setOffset_largeValue() {
        csc.setOffset(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, csc.getOffset());
    }

    // ---------------------------------------------------------------- maxResults

    @Test
    void setAndGetMaxResults() {
        csc.setMaxResults(50);
        assertEquals(50, csc.getMaxResults());
    }

    @Test
    void setMaxResults_unlimitedSentinelValue() {
        // Callers conventionally use -1 for "no limit"
        csc.setMaxResults(-1);
        assertEquals(-1, csc.getMaxResults());
    }

    @Test
    void setMaxResults_one_isMinimalPage() {
        csc.setMaxResults(1);
        assertEquals(1, csc.getMaxResults());
    }

    // ---------------------------------------------------------------- Combined / integration

    @Test
    void fullyConfigured_criteriaRetainsAllFields() {
        Weblog weblog   = new Weblog();
        WeblogEntry entry = new WeblogEntry();
        Date start      = new Date(100L);
        Date end        = new Date(200L);

        csc.setWeblog(weblog);
        csc.setEntry(entry);
        csc.setSearchText("hello");
        csc.setStartDate(start);
        csc.setEndDate(end);
        csc.setStatus(WeblogEntryComment.ApprovalStatus.PENDING);
        csc.setReverseChrono(true);
        csc.setOffset(10);
        csc.setMaxResults(100);

        assertSame(weblog, csc.getWeblog());
        assertSame(entry, csc.getEntry());
        assertEquals("hello", csc.getSearchText());
        assertEquals(start, csc.getStartDate());
        assertEquals(end, csc.getEndDate());
        assertEquals(WeblogEntryComment.ApprovalStatus.PENDING, csc.getStatus());
        assertTrue(csc.isReverseChrono());
        assertEquals(10, csc.getOffset());
        assertEquals(100, csc.getMaxResults());
    }

    @Test
    void twoDifferentInstances_areIndependent() {
        CommentSearchCriteria other = new CommentSearchCriteria();
        csc.setSearchText("modified");
        assertNull(other.getSearchText());
    }
}
