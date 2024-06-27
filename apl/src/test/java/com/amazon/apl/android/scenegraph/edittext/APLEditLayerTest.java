/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.scenegraph.edittext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.text.InputFilter;
import android.widget.EditText;

import androidx.test.core.app.ApplicationProvider;

import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.android.RuntimeConfig;
import com.amazon.apl.android.font.IFontResolver;
import com.amazon.apl.android.font.TypefaceResolver;
import com.amazon.apl.android.primitive.SGRect;
import com.amazon.apl.android.scaling.IMetricsTransform;
import com.amazon.apl.android.scenegraph.text.APLTextProperties;
import com.amazon.apl.android.sgcontent.EditTextNode;
import com.amazon.apl.android.sgcontent.Node;
import com.amazon.apl.android.views.APLView;
import com.amazon.apl.enums.KeyboardType;
import com.amazon.apl.enums.SubmitKeyType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 22, manifest = Config.NONE)
public class APLEditLayerTest {
    private APLEditLayer mAPLEditLayer;
    @Mock
    private RenderingContext mockRenderingContext;
    @Mock
    private com.amazon.apl.android.scenegraph.edittext.EditText mockNativeEditText;
    @Mock
    private EditTextNode mockEditTextNode;
    @Mock
    private EditTextConfig mockEditTextConfig;
    @Mock
    private APLTextProperties mockAPLTextProperties;
    @Mock
    private IMetricsTransform mockMetricsTransform;
    @Mock
    private RuntimeConfig mockRuntimeConfig;
    @Mock
    private IFontResolver mockFontResolver;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mAPLEditLayer = spy(new APLEditLayer(mockRenderingContext));
        when(mockRenderingContext.getMetricsTransform()).thenReturn(mockMetricsTransform);
        doReturn(new Node[]{mockEditTextNode}).when(mAPLEditLayer).getContent();
        doReturn(SGRect.create(new float[]{0, 0, 100, 40})).when(mAPLEditLayer).getBounds();
        doReturn(1.0f).when(mAPLEditLayer).getOpacity();
        when(mockEditTextNode.getEditText()).thenReturn(mockNativeEditText);
        when(mockEditTextNode.getEditTextConfig()).thenReturn(mockEditTextConfig);
        when(mockEditTextConfig.getTextProperties(any())).thenReturn(mockAPLTextProperties);
        when(mockEditTextConfig.getKeyboardType()).thenReturn(KeyboardType.kKeyboardTypeNormal);
        when(mockEditTextConfig.getSubmitKeyType()).thenReturn(SubmitKeyType.kSubmitKeyTypeDone);
        when(mockRuntimeConfig.getFontResolver()).thenReturn(mockFontResolver);
        TypefaceResolver.getInstance().initialize(ApplicationProvider.getApplicationContext(), mockRuntimeConfig);
    }

    @Test
    public void testAttachView_attaches_EditText_instance() {
        APLView aplView = new APLView(ApplicationProvider.getApplicationContext(), mAPLEditLayer);
        mAPLEditLayer.attachView(aplView);
        assertEquals(1, aplView.getChildCount());
        assertTrue(aplView.getChildAt(0) instanceof EditText);
    }

    @Test
    public void testAttachView_validCharactersFilter() {
        APLView aplView = new APLView(ApplicationProvider.getApplicationContext(), mAPLEditLayer);
        mAPLEditLayer.attachView(aplView);
        assertEquals(1, aplView.getChildCount());
        assertTrue(aplView.getChildAt(0) instanceof EditText);
        EditText editText = (EditText) aplView.getChildAt(0);
        when(mockEditTextConfig.strip("abcdefghi")).thenReturn("abcdefg");
        editText.setText("abcdefghi");
        // This verifies that the edittext does not accept invalid characters.
        assertEquals("abcdefg", editText.getText().toString());
    }
}
