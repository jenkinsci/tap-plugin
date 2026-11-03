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
