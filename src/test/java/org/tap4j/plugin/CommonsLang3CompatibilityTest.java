/*
 * The MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.tap4j.plugin;

import java.util.Collections;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.tap4j.plugin.model.TapTestResultResult;
import org.tap4j.plugin.util.DiagnosticUtil;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CommonsLang3CompatibilityTest {
    @Test
    public void migratedClassesDoNotLinkToCommonsLang2() throws Exception {
        for (Class<?> type : new Class<?>[] {TapPublisher.class, TapResult.class,
                TapTestResultResult.class, DiagnosticUtil.class}) {
            try (InputStream bytes = type.getResourceAsStream(type.getSimpleName() + ".class")) {
                String constants = new String(bytes.readAllBytes(), StandardCharsets.ISO_8859_1);
                assertFalse(type.getName(), constants.contains("org/apache/commons/lang/"));
            }
        }
    }

    @Test
    public void publisherPreservesDefaultsForMissingOptions() {
        TapPublisher publisher = new TapPublisher("*.tap", null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null);
        assertFalse(publisher.getFailIfNoResults());
        assertTrue(publisher.getOutputTapToConsole());
        assertTrue(publisher.getEnableSubtests());
        assertFalse(publisher.getShowOnlyFailures());
    }

    @Test
    public void resultHandlesMissingAndExplicitFailureFilter() {
        TapResult result = new TapResult("sample", null, Collections.emptyList(), false, false, false);
        result.setShowOnlyFailures(null);
        assertFalse(result.getShowOnlyFailures());
        result.setShowOnlyFailures(true);
        assertTrue(result.getShowOnlyFailures());
        result.setShowOnlyFailures(false);
        assertFalse(result.getShowOnlyFailures());
    }
}
