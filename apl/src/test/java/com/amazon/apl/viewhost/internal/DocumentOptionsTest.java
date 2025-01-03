package com.amazon.apl.viewhost.internal;

import com.amazon.apl.android.metrics.MetricsOptions;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.viewhost.config.DocumentOptions;
import com.amazon.alexaext.ExtensionRegistrar;
import com.amazon.apl.android.dependencies.IUserPerceivedFatalCallback;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class DocumentOptionsTest {

    @Test
    public void testMergeDocumentOptions_providedDocumentOptionsNull() {
        DocumentOptions defaultDocumentOptions = DocumentOptions.builder()
                .extensionRegistrar(mock(ExtensionRegistrar.class))
                .extensionFlags(new HashMap<>())
                .telemetryProvider(mock(ITelemetryProvider.class))
                .metricsOptions(mock(MetricsOptions.class))
                .userPerceivedFatalCallback(mock(IUserPerceivedFatalCallback.class))
                .properties(new HashMap<>())
                .build();

        DocumentOptions mergedOptions = defaultDocumentOptions.merge(null);

        assertEquals(defaultDocumentOptions, mergedOptions);
    }

    @Test
    public void testMergeDocumentOptions_defaultDocumentOptionsNull() {
        DocumentOptions providedDocumentOptions = DocumentOptions.builder()
                .extensionRegistrar(mock(ExtensionRegistrar.class))
                .extensionFlags(new HashMap<>())
                .telemetryProvider(mock(ITelemetryProvider.class))
                .metricsOptions(mock(MetricsOptions.class))
                .userPerceivedFatalCallback(mock(IUserPerceivedFatalCallback.class))
                .properties(new HashMap<>())
                .build();

        DocumentOptions mergedOptions = providedDocumentOptions.merge(null);

        assertEquals(providedDocumentOptions, mergedOptions);
    }

    @Test
    public void testMergeDocumentOptions_bothOptionsProvided() {
        ExtensionRegistrar providedExtensionRegistrar = mock(ExtensionRegistrar.class);
        Map<String, Object> providedExtensionFlags = new HashMap<>();
        providedExtensionFlags.put("key1", "value1");
        ITelemetryProvider providedTelemetryProvider = mock(ITelemetryProvider.class);
        MetricsOptions providedMetricsOptions = mock(MetricsOptions.class);
        IUserPerceivedFatalCallback providedUserPerceivedFatalCallback = mock(IUserPerceivedFatalCallback.class);
        Map<String, Object> providedProperties = new HashMap<>();
        providedProperties.put("prop1", "value1");

        DocumentOptions providedDocumentOptions = DocumentOptions.builder()
                .extensionRegistrar(providedExtensionRegistrar)
                .extensionFlags(providedExtensionFlags)
                .telemetryProvider(providedTelemetryProvider)
                .metricsOptions(providedMetricsOptions)
                .userPerceivedFatalCallback(providedUserPerceivedFatalCallback)
                .properties(providedProperties)
                .build();

        ExtensionRegistrar defaultExtensionRegistrar = mock(ExtensionRegistrar.class);
        Map<String, Object> defaultExtensionFlags = new HashMap<>();
        defaultExtensionFlags.put("key2", "value2");
        ITelemetryProvider defaultTelemetryProvider = mock(ITelemetryProvider.class);
        MetricsOptions defaultMetricsOptions = mock(MetricsOptions.class);
        IUserPerceivedFatalCallback defaultUserPerceivedFatalCallback = mock(IUserPerceivedFatalCallback.class);
        Map<String, Object> defaultProperties = new HashMap<>();
        defaultProperties.put("prop2", "value2");

        DocumentOptions defaultDocumentOptions = DocumentOptions.builder()
                .extensionRegistrar(defaultExtensionRegistrar)
                .extensionFlags(defaultExtensionFlags)
                .telemetryProvider(defaultTelemetryProvider)
                .metricsOptions(defaultMetricsOptions)
                .userPerceivedFatalCallback(defaultUserPerceivedFatalCallback)
                .properties(defaultProperties)
                .build();

        DocumentOptions mergedOptions = defaultDocumentOptions.merge(providedDocumentOptions);

        assertEquals(providedExtensionRegistrar, mergedOptions.getExtensionRegistrar());
        assertEquals(providedTelemetryProvider, mergedOptions.getTelemetryProvider());
        assertEquals(providedMetricsOptions, mergedOptions.getMetricsOptions());
        assertEquals(providedUserPerceivedFatalCallback, mergedOptions.getUserPerceivedFatalCallback());

        Map<String, Object> mergedExtensionFlags = mergedOptions.getExtensionFlags();
        assertEquals(2, mergedExtensionFlags.size());
        assertTrue(mergedExtensionFlags.containsKey("key1"));
        assertTrue(mergedExtensionFlags.containsValue("value1"));
        assertTrue(mergedExtensionFlags.containsKey("key2"));
        assertTrue(mergedExtensionFlags.containsValue("value2"));

        Map<String, Object> mergedProperties = mergedOptions.getProperties();
        assertEquals(2, mergedProperties.size());
        assertTrue(mergedProperties.containsKey("prop1"));
        assertTrue(mergedProperties.containsValue("value1"));
        assertTrue(mergedProperties.containsKey("prop2"));
        assertTrue(mergedProperties.containsValue("value2"));
    }
}
