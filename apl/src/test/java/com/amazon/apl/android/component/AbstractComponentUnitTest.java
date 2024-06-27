/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import static com.amazon.apl.enums.Display.kDisplayInvisible;
import static com.amazon.apl.enums.Display.kDisplayNormal;
import static com.amazon.apl.enums.Role.kRoleNone;
import static com.amazon.apl.enums.Role.kRoleSearch;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.Color;
import android.graphics.RectF;
import android.text.TextUtils;
import android.view.View;

import androidx.test.platform.app.InstrumentationRegistry;

import com.amazon.apl.android.APLController;
import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.Content;
import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.RuntimeConfig;
import com.amazon.apl.android.font.CompatFontResolver;
import com.amazon.apl.android.metrics.ICounter;
import com.amazon.apl.android.metrics.ITimer;
import com.amazon.apl.android.metrics.impl.MetricsRecorder;
import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.android.scaling.ViewportMetrics;
import com.amazon.apl.android.utils.APLTrace;
import com.amazon.apl.enums.LayoutDirection;
import com.amazon.apl.enums.ScreenShape;
import com.amazon.apl.enums.ViewportMode;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * This abstract test class provides a basic framework for testing an APL
 * Component.  It supports validation of properties, jni integration, and
 * view creation.  This base class will test  features common to all components and
 * call into base classes for Component specific features.
 */
public abstract class AbstractComponentUnitTest<V extends View, C extends Component> extends ViewhostRobolectricTest {

    static {
        RuntimeConfig runtimeConfig = RuntimeConfig.builder().fontResolver(new CompatFontResolver()).build();
        APLController.initializeAPL(InstrumentationRegistry.getInstrumentation().getContext(), runtimeConfig);
    }

    // Base document has a instance of the test Component as the top Component. The component
    // type is inserted as the "type": property. Required and optional properties can also be
    // inserted into the component properties. Additionally components like {@link VectorGraphic}
    // can have optional template properties.
    final static String BASE_DOC = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.0\"," +
            "  \"mainTemplate\": {" +
            "    \"item\":" +
            "    {" +
            "      \"id\": \"testcomp\", " +
            "      \"type\": \"%s\" %s" +
            "    }" +
            "  }" +
            "%s" +
            "}";

    // Document has a instance of the test Component parented by a Frame. The component
    // type is inserted as the "type": property. Required and optional properties can also be
    // inserted into the component properties.
    @SuppressWarnings("SpellCheckingInspection")
    final static String PARENT_DOC = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.0\"," +
            "  \"mainTemplate\": {" +
            "    \"item\": {" +
            "      \"type\": \"Frame\"," +
            "     \"name\": \"parentcomp\"," +
            "      \"item\": { " +
            "        \"id\": \"testcomp\", " +
            "        \"type\": \"%s\" %s" +
            "      }" +
            "    }" +
            "  }" +
            "}";

    final static String PARENT_DOC_WITH_LANG_AND_LAYOUT_DIRECTION = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.0\"," +
            "  \"lang\": \"en-US\"," +
            "  \"layoutDirection\": \"RTL\"," +
            "  \"mainTemplate\": {" +
            "    \"item\": {" +
            "      \"type\": \"Frame\"," +
            "     \"name\": \"parentcomp\"," +
            "      \"item\": { " +
            "        \"id\": \"testcomp\", " +
            "        \"type\": \"%s\" %s" +
            "      }" +
            "    }" +
            "  }" +
            "}";

    // Properties that are common to all components, but optional.
    final static String OPTIONAL_COMMON_PROPERTIES = "" +
            "\"accessibilityLabel\": \"Go APL\",\n" +
            "      \"description\": \"APL Description\",\n" +
            "      \"checked\": true,\n" +
            "      \"display\": \"invisible\",\n" +
            "      \"height\": 111,\n" +
            "      \"width\": 642,\n" +
            "      \"maxHeight\": 123,\n" +
            "      \"minHeight\": 13,\n" +
            "      \"maxWidth\": 669,\n" +
            "      \"minWidth\": 64,\n" +
            "      \"role\": \"search\",\n" +
            "      \"opacity\": 0.5,\n" +
            "      \"actions\": {\n" +
            "          \"name\": \"activate\",\n" +
            "          \"label\": \"Reply to user\",\n" +
            "          \"command\": {\n" +
            "              \"type\": \"SendEvent\",\n" +
            "              \"arguments\": \"Activated by action invocation\"\n" +
            "          }\n" +
            "      }";

    // Display metrics.
    ViewportMetrics mMetrics;

    /**
     * @return The string representation used in the APL document for this component.  For example
     * when testing the Text Component this method should return "Text".
     */
    abstract String getComponentType();

    /**
     * Test the required properties of the Component.
     *
     * @param component The Component for testing.
     */
    abstract void testProperties_required(C component);

    // RootContext - detached from APLLayout
    RootContext mRootContext;
    RenderingContext mRenderingContext;

    // RootConfig
    RootConfig mRootConfig;

    // Configurable options
    @SuppressWarnings("WeakerAccess")
    APLOptions mAplOptions;

    // Android Context
    Context mContext = InstrumentationRegistry.getInstrumentation().getContext();


    // Default required properties, default to empty.
    String REQUIRED_PROPERTIES = "";
    // Default optional properties, default to empty.
    String OPTIONAL_PROPERTIES = "";


    @Mock
    IAPLViewPresenter mAPLPresenter;
    @Mock
    MetricsRecorder mMetricsRecorder;
    @Mock
    protected ICounter mCounter;
    @Mock
    protected ITimer mTimer;

    @Before
    public void setup() {
        // create a RootContext
        mMetrics = ViewportMetrics.builder()
                .width(2048)
                .height(1024)
                .dpi(160)
                .shape(ScreenShape.RECTANGLE)
                .theme("dark")
                .mode(ViewportMode.kViewportModeHub)
                .build();


        MockitoAnnotations.initMocks(this);
        mRootConfig = RootConfig.create("Unit Test", "1.0");
        when(mAPLPresenter.getAPLTrace()).thenReturn(mock(APLTrace.class));
    }

    /**
     * Inflates an APL Document.  The document will have the Component type {@link #getComponentType()}
     * inserted into it at the "type" property.
     *
     * @param document The resulting document.
     */
    protected void inflateDocument(String document) {
        inflateDocument(document, null);
    }

    /**
     * Inflates an APL Document.  The document will have the Component type {@link #getComponentType()}
     * inserted into it at the "type" property.
     *
     * @param document The resulting document.
     * @param payload  The data payload.
     */
    @SuppressWarnings("WeakerAccess")
    void inflateDocument(String document,
                         @SuppressWarnings("SameParameterValue") String payload) {
        inflateDocument(document, payload, APLOptions.builder().build());
    }


    /**
     * Inflates an APL Document.  The document will have the Component type {@link #getComponentType()}
     * inserted into it at the "type" property.
     *
     * @param document   The resulting document.
     * @param payload    The data payload.
     * @param aplOptions The configurable options.
     */
    void inflateDocument(String document,
                         @SuppressWarnings("SameParameterValue") String payload,
                         APLOptions aplOptions) {

        Content content = null;
        try {
            content = Content.create(String.format(document, getComponentType()));
        } catch (Content.ContentException e) {
            Assert.fail(e.getMessage());
        }

        if (payload != null) {
            content.addData("payload", payload);
        }
        if (content.isError() || content.isWaiting()) {
            Assert.fail("The document content is incorrect.");
        }
        mAplOptions = aplOptions;

        // create a RootContext
        mMetrics = ViewportMetrics.builder()
                .width(2048)
                .height(1024)
                .dpi(160)
                .shape(ScreenShape.RECTANGLE)
                .theme("dark")
                .mode(ViewportMode.kViewportModeHub)
                .build();

        when(mMetricsRecorder.createCounter(anyString())).thenReturn(mCounter);
        when(mMetricsRecorder.startTimer(anyString(), any())).thenReturn(mTimer);

        mRootContext = RootContext.create(mMetrics, content, mRootConfig, mAplOptions, mAPLPresenter, mMetricsRecorder);
        mRenderingContext = mRootContext.getRenderingContext();

        if (mRootContext.getNativeHandle() == 0) {
            Assert.fail("The document failed to load.");
        }
    }


    /**
     * Builds a document from the {@link #BASE_DOC}. The document will have the Component type
     * {@link #getComponentType()} inserted into it at the "type" property, and REQUIRED_PROPERTIES.
     *
     * @return APL document.
     */
    @SuppressWarnings("WeakerAccess")
    final String buildDocument() {
        return buildDocument(BASE_DOC, REQUIRED_PROPERTIES, "", "");
    }


    /**
     * Builds a document from the {@link #BASE_DOC}. The document will have the Component type
     * {@link #getComponentType()} inserted into it at the "type" property, and additional properties
     * inserted into the Component properties portion of document. An empty or null string is allowed
     * for properties.
     *
     * @param properties Additional properties to insert into the document.
     * @return APL document.
     */
    final String buildDocument(String properties) {
        return buildDocument(BASE_DOC, properties, "", "");
    }

    /**
     * Builds a document from the {@link #BASE_DOC}. The document will have the Component type
     * {@link #getComponentType()} inserted into it at the "type" property, and additional properties
     * inserted into the Component properties portion of document. An empty or null string is allowed
     * for properties.
     *
     * @param requiredProperties Required properties to insert into the document.
     * @param optionalProperties Optional properties to insert into the document.
     * @return APL document.
     */
    final String buildDocument(String requiredProperties, String optionalProperties) {
        return buildDocument(BASE_DOC, requiredProperties, optionalProperties, "");
    }

    //Default optional template properties, default to empty.
    static String OPTIONAL_TEMPLATE_PROPERTIES = "";

    /**
     * Test the optional properties of the Component.  This test should check for default value
     * and values. {@link #OPTIONAL_PROPERTIES} should be set prior to this test to ensure a valid
     * Component is created.
     *
     * @param component The Component for testing.
     */
    abstract void testProperties_optionalDefaultValues(C component);

    /**
     * Test the optional properties of the Component.  This test should check values when the property
     * is set explicitly, and should use values other than the default.  Set the {@link #OPTIONAL_PROPERTIES}
     * value before this test.
     *
     * @param component The Component for testing.
     */
    abstract void testProperties_optionalExplicitValues(C component);

    /**
     * Gets a component from the RootContext created by the test doc.  This method
     * first looks for a component named 'testcomp', if not found it returns the top component.
     *
     * @return the component named 'testcomp', otherwise the "Top" component in the document.
     */
    @SuppressWarnings("unchecked")
    final C getTestComponent() {
        Component component = mRootContext.findComponentById("testcomp");
        if (component != null) {
            return (C) component;
        }
        return (C) mRootContext.getTopComponent();
    }


    /**
     * Builds a document. The document will have the Component type
     * {@link #getComponentType()} inserted into it at the "type" property, additional properties
     * inserted into the Component properties portion of document, and additional template properties .
     * An empty or null string is allowed for properties.
     *
     * @param requiredProperties         Required properties to insert into the document.
     * @param optionalProperties         Optional properties to insert into the document.
     * @param optionalTemplateProperties Optional template properties to insert into the document.
     * @return APL document.
     */
    final String buildDocument(String requiredProperties, String optionalProperties,
                               String optionalTemplateProperties) {
        return buildDocument(BASE_DOC, requiredProperties, optionalProperties,
                optionalTemplateProperties);
    }

    /**
     * Builds a document. The document will have the Component type
     * {@link #getComponentType()} inserted into it at the "type" property, additional properties
     * inserted into the Component properties portion of document, and additional template
     * properties. An empty or null string is allowed.
     * for properties.
     *
     * @param baseDocument               Base Document to modify.
     * @param requiredProperties         Required properties to insert into the document.
     * @param optionalProperties         Optional properties to insert into the document.
     * @param optionalTemplateProperties Optional template properties to insert into the document.
     * @return APL document.
     */
    final String buildDocument(String baseDocument,
                               String requiredProperties,
                               String optionalProperties,
                               String optionalTemplateProperties) {
        StringBuilder fullProps = new StringBuilder();
        if (requiredProperties != null && requiredProperties.length() > 0) {
            fullProps.append(",");
            fullProps.append(requiredProperties);
        }
        if (optionalProperties != null && optionalProperties.length() > 0) {
            fullProps.append(",");
            fullProps.append(optionalProperties);
        }
        if (!TextUtils.isEmpty(optionalTemplateProperties)) {
            optionalTemplateProperties = "," + optionalTemplateProperties;
        }
        return String.format(baseDocument, getComponentType(),
                fullProps.toString(), optionalTemplateProperties);
    }


    /**
     * Test the default width and height dimensions of the Component. Also, this test should check
     * the origin of the screen coordinates.
     */
    @Test
    public void testProperties_bounds() {
        String size = "\"width\": \"100vw\", \"height\": \"100vh\"";
        inflateDocument(buildDocument(REQUIRED_PROPERTIES, size));
        Component component = getTestComponent();
        Rect componentBounds = component.getBounds();
        assertEquals(2048, componentBounds.intWidth());
        assertEquals(1024, componentBounds.intHeight());
        assertEquals(0, componentBounds.intLeft());
        assertEquals(0, componentBounds.intTop());
    }

    /**
     * Test the width and height dimensions of the Component after applying certain explicit
     * padding. Also, this test should check the origin of the screen coordinates.
     */
    @Test
    public void testProperties_innerBounds() {
        String padding = "\"width\": \"100vw\", \"height\": \"100vh\", \"paddingLeft\": 10, \"paddingTop\": 20, \"paddingRight\": 30, \"paddingBottom\": 40";
        inflateDocument(buildDocument(REQUIRED_PROPERTIES, padding));
        Component component = getTestComponent();
        Rect innerBounds = component.getInnerBounds();
        assertEquals(2008, innerBounds.intWidth());
        assertEquals(964, innerBounds.intHeight());
        assertEquals(10, innerBounds.intLeft());
        assertEquals(20, innerBounds.intTop());
        assertEquals(10, component.getInnerBounds().intLeft() - component.getBounds().intLeft());
        assertEquals(20, component.getInnerBounds().intTop() - component.getBounds().intTop());
        assertEquals(30, component.getBounds().intRight() - component.getInnerBounds().intRight());
        assertEquals(40, component.getBounds().intBottom() - component.getInnerBounds().intBottom());
    }

    @Test
    public void testProperties_requiredCommonExplicit() {
        inflateDocument(buildDocument());
        Component component = getTestComponent();

        //No properties are common and required
        assertEquals("kComponentType" + getComponentType(), component.getComponentType().toString());
    }

    @Test
    public void testProperties_optionalCommonExplicitValues() {
        // these are optional props common to all components
        inflateDocument(buildDocument(REQUIRED_PROPERTIES, OPTIONAL_COMMON_PROPERTIES));
        Component component = getTestComponent();

        assertEquals("Go APL", component.getAccessibilityLabel());
        assertTrue(component.isChecked());
        assertEquals(kDisplayInvisible, component.getDisplay());
        assertEquals("testcomp", component.getId());
        assertEquals(.5f, component.getOpacity(), 0.0f);
        assertEquals(kRoleSearch, component.getRole());
        assertEquals(1, component.getAccessibilityActions().size());
        assertEquals("activate", component.getAccessibilityActions().at(0).name());
        assertEquals("Reply to user", component.getAccessibilityActions().at(0).label());
    }

    @Test
    public void testProperties_optionalCommonDefaultValues() {
        // these are optional props common to all components
        inflateDocument(buildDocument(REQUIRED_PROPERTIES));
        Component component = getTestComponent();

        assertEquals("", component.getAccessibilityLabel());
        assertFalse(component.isChecked());
        assertFalse(component.isDisabled());
        assertEquals(kDisplayNormal, component.getDisplay());
        assertEquals("testcomp", component.getId());
        assertEquals(1.0, component.getOpacity(), 0.0);
        assertEquals(kRoleNone, component.getRole());
        assertEquals(0, component.getAccessibilityActions().size());
        assertEquals(LayoutDirection.kLayoutDirectionLTR, component.getLayoutDirection());
    }

    @Test
    public void testProperties_requiredProperties() {
        inflateDocument(buildDocument());
        testProperties_required(getTestComponent());
    }

    @Test
    public void testProperties_optionalDefaultProperties() {
        inflateDocument(buildDocument());
        testProperties_optionalDefaultValues(getTestComponent());
    }


    @Test
    public void testProperties_optionalExplicitProperties() {
        String doc = buildDocument(REQUIRED_PROPERTIES, OPTIONAL_PROPERTIES, OPTIONAL_TEMPLATE_PROPERTIES);
        inflateDocument(doc);
        testProperties_optionalExplicitValues(getTestComponent());
    }


    @Test
    public void testComponent_asChild() {
        inflateDocument(buildDocument(PARENT_DOC, REQUIRED_PROPERTIES, "", ""));
        Component top = mRootContext.getTopComponent();
        Component component = getTestComponent();

        assertEquals(1, top.getChildCount());
        assertEquals(component.getUniqueId(), top.getChildId(0));
        assertEquals(component, top.getChildAt(0));
        assertEquals(component.getParentId(), top.getUniqueId());
    }


    @Test
    public void testComponent_id() {
        inflateDocument(buildDocument(PARENT_DOC, REQUIRED_PROPERTIES, "", ""));
        Component component = getTestComponent();

        assertEquals(component.getComponentId(), component.getUniqueId());
        assertEquals("testcomp", component.getId());
    }


    @Test
    public void testComponent_shadowDefaultProperties() {
        String size = "\"width\": \"30dp\", \"height\": \"50dp\"";
        inflateDocument(buildDocument(REQUIRED_PROPERTIES), size);
        Component component = getTestComponent();

        assertEquals(0, component.getShadowOffsetHorizontal());
        assertEquals(0, component.getShadowOffsetVertical());
        assertEquals(0, component.getShadowRadius());
        assertEquals(Color.TRANSPARENT, component.getShadowColor());
        assertArrayEquals(new float[]{0f, 0f, 0f, 0f}, component.getShadowCornerRadius(), 0.01f);
        Rect componentBounds = component.getBounds();
        RectF shadowBounds = component.getShadowRect();
        assertEquals(componentBounds.getLeft(), shadowBounds.left, 0.01f);
        assertEquals(componentBounds.getTop(), shadowBounds.top, 0.01f);
        assertEquals(componentBounds.getRight(), shadowBounds.right, 0.01f);
        assertEquals(componentBounds.getBottom(), shadowBounds.bottom, 0.01f);
    }

    @Test
    public void testProperties_shadow() {
        String shadowProps = "\"shadowHorizontalOffset\": \"16dp\", \"shadowVerticalOffset\": \"10dp\", \"shadowRadius\": \"32dp\", \"shadowColor\": \"red\", \"transform\": [{ \"rotate\": \"90deg\" }]";
        String doc = buildDocument(REQUIRED_PROPERTIES, shadowProps);
        inflateDocument(doc);
        Component comp = getTestComponent();

        assertEquals(16, comp.getShadowOffsetHorizontal());
        assertEquals(10, comp.getShadowOffsetVertical());
        assertEquals(32, comp.getShadowRadius());
        assertEquals(Color.RED, comp.getShadowColor());
    }

    @Test
    public void testComponent_receives_document_level_layoutDirection() {
        inflateDocument(buildDocument(PARENT_DOC_WITH_LANG_AND_LAYOUT_DIRECTION, REQUIRED_PROPERTIES, "", ""));
        Component component = getTestComponent();

        assertEquals(LayoutDirection.kLayoutDirectionRTL, component.getLayoutDirection());
    }
}
