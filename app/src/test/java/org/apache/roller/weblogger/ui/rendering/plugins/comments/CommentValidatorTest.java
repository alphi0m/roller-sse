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

package org.apache.roller.weblogger.ui.rendering.plugins.comments;

import java.lang.reflect.Field;
import java.util.Properties;

import org.apache.roller.util.RollerConstants;
import org.apache.roller.weblogger.config.WebloggerConfig;
import org.apache.roller.weblogger.pojos.WeblogEntryComment;
import org.apache.roller.weblogger.util.RollerMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests per {@link CommentValidator} (contratto) e
 * {@link CommentValidationManager} (logica di orchestrazione/media).
 *
 * <h3>Nota di progettazione — isolamento da DB</h3>
 * <p>Il costruttore di default di {@link CommentValidationManager} carica i
 * validator configurati in {@code roller.properties}
 * ({@code BannedwordslistCommentValidator}, ecc.) tramite reflection. Questi
 * validator richiedono una connessione Derby via {@code WebloggerRuntimeConfig}
 * che non è disponibile nei test unitari. Per isolare il test, {@code @BeforeEach}
 * svuota la lista interna {@code validators} via reflection — operazione limitata
 * alla sola classe di test, senza modificare il codice di produzione.</p>
 *
 * <h3>Contratto CommentValidator</h3>
 * <ul>
 *   <li>100 ({@link RollerConstants#PERCENT_100}) = pienamente valido.</li>
 *   <li>0 = spam / non valido.</li>
 *   <li>Gli errori sono comunicati tramite il parametro {@link RollerMessages}.</li>
 *   <li>{@link CommentValidator#getName()} deve restituire un nome non vuoto.</li>
 * </ul>
 *
 * <h3>Regola di media di CommentValidationManager</h3>
 * <p>Con n validator che restituiscono s₁…sₙ: risultato = (s₁+…+sₙ)/n.
 * Lista vuota → 100 (nessuna restrizione).</p>
 */
class CommentValidatorTest {

    // ---------------------------------------------------------------- validator inline di test

    /** Sempre valido: ritorna 100, nessun messaggio. */
    private static final CommentValidator ALWAYS_VALID = new CommentValidator() {
        @Override public String getName() { return "AlwaysValid"; }
        @Override public int validate(WeblogEntryComment c, RollerMessages m) {
            return RollerConstants.PERCENT_100;
        }
    };

    /** Sempre non valido: ritorna 0, aggiunge un errore. */
    private static final CommentValidator ALWAYS_INVALID = new CommentValidator() {
        @Override public String getName() { return "AlwaysInvalid"; }
        @Override public int validate(WeblogEntryComment c, RollerMessages m) {
            m.addError("comment.validator.test.invalid");
            return 0;
        }
    };

    /** Mezza confidenza: ritorna 50, nessun messaggio. */
    private static final CommentValidator HALF_CONFIDENCE = new CommentValidator() {
        @Override public String getName() { return "HalfConfidence"; }
        @Override public int validate(WeblogEntryComment c, RollerMessages m) {
            return 50;
        }
    };

    // ---------------------------------------------------------------- manager isolato

    private CommentValidationManager mgr;

    @BeforeEach
    void setUp() throws Exception {
        // Il costruttore di CommentValidationManager carica i validator configurati
        // in roller.properties (BannedwordslistCommentValidator ecc.) che richiedono
        // Derby tramite WebloggerRuntimeConfig. Per evitarlo, azzeriamo temporaneamente
        // la property in WebloggerConfig prima di chiamare il costruttore.
        Field configField = WebloggerConfig.class.getDeclaredField("config");
        configField.setAccessible(true);
        Properties props = (Properties) configField.get(null);

        String originalClassnames = props.getProperty("comment.validator.classnames");
        props.setProperty("comment.validator.classnames", "");

        try {
            mgr = new CommentValidationManager();
        } finally {
            // Ripristina il valore originale per non interferire con altri test
            if (originalClassnames != null) {
                props.setProperty("comment.validator.classnames", originalClassnames);
            } else {
                props.remove("comment.validator.classnames");
            }
        }
    }

    // ---------------------------------------------------------------- helper

    private static WeblogEntryComment comment(String content, String ip) {
        WeblogEntryComment c = new WeblogEntryComment();
        c.setContent(content);
        c.setRemoteHost(ip);
        return c;
    }

    // =====================================================================
    // CommentValidator — contratto
    // =====================================================================

    @Test
    void validValidator_returnsHundred_addsNoErrors() {
        RollerMessages msgs = new RollerMessages();
        int score = ALWAYS_VALID.validate(comment("Hello!", "1.2.3.4"), msgs);
        assertEquals(RollerConstants.PERCENT_100, score);
        assertEquals(0, msgs.getErrorCount());
    }

    @Test
    void invalidValidator_returnsZero_addsOneError() {
        RollerMessages msgs = new RollerMessages();
        int score = ALWAYS_INVALID.validate(comment("spam content", "5.6.7.8"), msgs);
        assertEquals(0, score);
        assertEquals(1, msgs.getErrorCount());
    }

    @Test
    void getName_isNotBlank_forAllTestValidators() {
        assertFalse(ALWAYS_VALID.getName().isBlank());
        assertFalse(ALWAYS_INVALID.getName().isBlank());
        assertFalse(HALF_CONFIDENCE.getName().isBlank());
    }

    @Test
    void validator_doesNotModifyCommentContent() {
        WeblogEntryComment c = comment("original content", "1.1.1.1");
        ALWAYS_VALID.validate(c, new RollerMessages());
        assertEquals("original content", c.getContent());
    }

    @Test
    void halfConfidenceScore_isInValidRange() {
        int score = HALF_CONFIDENCE.validate(comment("text", "1.1.1.1"), new RollerMessages());
        assertTrue(score >= 0 && score <= 100,
                "score dovrebbe essere in [0, 100], era: " + score);
    }

    @Test
    void invalidValidator_errorKeyIsPreserved() {
        RollerMessages msgs = new RollerMessages();
        ALWAYS_INVALID.validate(comment("spam", "1.1.1.1"), msgs);
        assertEquals("comment.validator.test.invalid", msgs.getErrors().next().getKey());
    }

    @Test
    void sameComment_validatedTwice_noSideEffects() {
        WeblogEntryComment c = comment("neutral text", "9.9.9.9");
        RollerMessages m1 = new RollerMessages();
        RollerMessages m2 = new RollerMessages();
        ALWAYS_VALID.validate(c, m1);
        ALWAYS_VALID.validate(c, m2);
        assertEquals("neutral text", c.getContent());
        assertEquals(0, m1.getErrorCount());
        assertEquals(0, m2.getErrorCount());
    }

    @Test
    void customInlineValidator_behavesCorrectly() {
        CommentValidator lv = new CommentValidator() {
            @Override public String getName() { return "CustomInline"; }
            @Override public int validate(WeblogEntryComment c, RollerMessages m) {
                if (c.getContent() != null && c.getContent().contains("badword")) {
                    m.addError("custom.error");
                    return 0;
                }
                return RollerConstants.PERCENT_100;
            }
        };

        RollerMessages clean = new RollerMessages();
        assertEquals(100, lv.validate(comment("clean comment", "1.1.1.1"), clean));
        assertEquals(0,   clean.getErrorCount());

        RollerMessages spam = new RollerMessages();
        assertEquals(0,   lv.validate(comment("click badword here", "1.1.1.1"), spam));
        assertEquals(1,   spam.getErrorCount());
    }

    // =====================================================================
    // RollerMessages — accumulo
    // =====================================================================

    @Test
    void rollerMessages_accumulatesErrorsAcrossMultipleCalls() {
        RollerMessages msgs = new RollerMessages();
        ALWAYS_INVALID.validate(comment("a", "1.1.1.1"), msgs);
        ALWAYS_INVALID.validate(comment("b", "1.1.1.1"), msgs);
        assertEquals(2, msgs.getErrorCount());
    }

    @Test
    void rollerMessages_validValidatorNeverAddsErrors() {
        RollerMessages msgs = new RollerMessages();
        msgs.addError("pre-existing-error");
        ALWAYS_VALID.validate(comment("clean", "1.1.1.1"), msgs);
        assertEquals(1, msgs.getErrorCount()); // solo il pre-esistente
    }

    // =====================================================================
    // CommentValidationManager — lista vuota (reset via @BeforeEach)
    // =====================================================================

    @Test
    void noValidators_validateComment_returnsHundred() {
        // mgr svuotato in setUp(), nessun validator attivo
        int score = mgr.validateComment(comment("hi", "1.2.3.4"), new RollerMessages());
        assertEquals(100, score);
    }

    @Test
    void noValidators_noErrorsReported() {
        RollerMessages msgs = new RollerMessages();
        mgr.validateComment(comment("hi", "1.2.3.4"), msgs);
        assertEquals(0, msgs.getErrorCount());
    }

    // =====================================================================
    // CommentValidationManager — singolo validator
    // =====================================================================

    @Test
    void singleValidValidator_scoresHundred() {
        mgr.addCommentValidator(ALWAYS_VALID);
        RollerMessages msgs = new RollerMessages();
        assertEquals(100, mgr.validateComment(comment("hi", "1.2.3.4"), msgs));
        assertEquals(0, msgs.getErrorCount());
    }

    @Test
    void singleInvalidValidator_scoresZero_andReportsError() {
        mgr.addCommentValidator(ALWAYS_INVALID);
        RollerMessages msgs = new RollerMessages();
        assertEquals(0, mgr.validateComment(comment("spam", "1.2.3.4"), msgs));
        assertEquals(1, msgs.getErrorCount());
    }

    @Test
    void singleHalfConfidenceValidator_scoresFifty() {
        mgr.addCommentValidator(HALF_CONFIDENCE);
        assertEquals(50, mgr.validateComment(comment("text", "1.1.1.1"), new RollerMessages()));
    }

    // =====================================================================
    // CommentValidationManager — più validator (media)
    // =====================================================================

    @Test
    void twoValidValidators_averageIsHundred() {
        mgr.addCommentValidator(ALWAYS_VALID);
        mgr.addCommentValidator(ALWAYS_VALID);
        assertEquals(100, mgr.validateComment(comment("hi", "1.1.1.1"), new RollerMessages()));
    }

    @Test
    void validAndInvalidValidator_averageIsFifty() {
        mgr.addCommentValidator(ALWAYS_VALID);   // 100
        mgr.addCommentValidator(ALWAYS_INVALID); //   0  → media = 50
        assertEquals(50, mgr.validateComment(comment("mixed", "1.1.1.1"), new RollerMessages()));
    }

    @Test
    void threeValidators_averageIsCorrect() {
        mgr.addCommentValidator(ALWAYS_VALID);     // 100
        mgr.addCommentValidator(HALF_CONFIDENCE);  //  50
        mgr.addCommentValidator(ALWAYS_INVALID);   //   0
        // (100 + 50 + 0) / 3 = 50
        assertEquals(50, mgr.validateComment(comment("text", "1.1.1.1"), new RollerMessages()));
    }

    @Test
    void multipleInvalidValidators_errorsAccumulate() {
        CommentValidator err1 = new CommentValidator() {
            @Override public String getName() { return "Err1"; }
            @Override public int validate(WeblogEntryComment c, RollerMessages m) {
                m.addError("err.one"); return 0;
            }
        };
        CommentValidator err2 = new CommentValidator() {
            @Override public String getName() { return "Err2"; }
            @Override public int validate(WeblogEntryComment c, RollerMessages m) {
                m.addError("err.two"); return 0;
            }
        };
        mgr.addCommentValidator(err1);
        mgr.addCommentValidator(err2);
        RollerMessages msgs = new RollerMessages();
        mgr.validateComment(comment("bad", "1.1.1.1"), msgs);
        assertEquals(2, msgs.getErrorCount());
    }

    @Test
    void validateComment_neverReturnsNegative() {
        mgr.addCommentValidator(ALWAYS_INVALID);
        int score = mgr.validateComment(comment("x", "1.1.1.1"), new RollerMessages());
        assertTrue(score >= 0);
    }

    @Test
    void validateComment_neverExceedsHundred() {
        mgr.addCommentValidator(ALWAYS_VALID);
        int score = mgr.validateComment(comment("x", "1.1.1.1"), new RollerMessages());
        assertTrue(score <= 100);
    }
}
