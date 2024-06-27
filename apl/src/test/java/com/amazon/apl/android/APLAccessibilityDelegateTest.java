/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.os.Bundle;
import android.util.Pair;
import android.view.View;

import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.amazon.apl.android.primitive.AccessibilityActions;
import com.amazon.apl.android.primitive.AccessibilityAdjustableRange;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.enums.PropertyKey;
import com.amazon.apl.enums.Role;
import com.amazon.apl.enums.UpdateType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class APLAccessibilityDelegateTest extends ViewhostRobolectricTest {

    private static final String COMPONENT_ID = "test-component-id";
    private static final String TEXT = "test text";
    private static final AccessibilityAdjustableRange ADJUSTABLE_RANGE = AccessibilityAdjustableRange.create(0, 10, 5);

    @Mock
    private View mView;

    @Mock
    private Component mComponent;

    @Mock
    private PropertyMap<Component, PropertyKey> mPropertyMap;

    private AccessibilityNodeInfoCompat mNode;

    private APLAccessibilityDelegate mAccessibilityDelegate;

    @Before
    public void setup() {
        mAccessibilityDelegate = new APLAccessibilityDelegate(mComponent, getApplication());
        mNode = AccessibilityNodeInfoCompat.obtain();

        when(mComponent.isDisabled()).thenReturn(false);
        when(mComponent.getAccessibilityActions()).thenReturn(new AccessibilityActions() {
            @Override
            public List<AccessibilityAction> list() {
                return Collections.emptyList();
            }
        });
        when(mComponent.getRole()).thenReturn(Role.kRoleNone);

        mAccessibilityDelegate.resetCustomActionCount();
    }

    @Test
    public void test_nodeResourceIdSetIfComponentIdPresent() {
        when(mComponent.getId()).thenReturn(COMPONENT_ID);

        mAccessibilityDelegate.onInitializeAccessibilityNodeInfo(mView, mNode);

        assertEquals(COMPONENT_ID, mNode.getViewIdResourceName());
    }

    @Test
    public void test_nodeResourceIdNotSetIfComponentIdNotPresent() {
        when(mComponent.getId()).thenReturn("");

        mAccessibilityDelegate.onInitializeAccessibilityNodeInfo(mView, mNode);

        assertNull(mNode.getViewIdResourceName());
    }

    @Test
    public void test_nodeTextSetIfComponentTextPresent() {
        when(mComponent.hasProperty(PropertyKey.kPropertyText)).thenReturn(true);
        when(mComponent.hasTextProperty()).thenReturn(true);
        when(mComponent.getProperties()).thenReturn(mPropertyMap);
        when(mPropertyMap.getString(PropertyKey.kPropertyText)).thenReturn(TEXT);
        when(mComponent.getAccessibilityNodeText()).thenReturn(TEXT);

        mAccessibilityDelegate.onInitializeAccessibilityNodeInfo(mView, mNode);

        assertEquals(TEXT, mNode.getText());
    }

    @Test
    public void test_nodeTextNotSetIfComponentTextNotPresent() {
        when(mComponent.hasProperty(PropertyKey.kPropertyText)).thenReturn(false);

        mAccessibilityDelegate.onInitializeAccessibilityNodeInfo(mView, mNode);

        assertNull(mNode.getText());
    }

    @Test
    public void test_nodeRangeInfoSetIfComponentAdjustable() {
        configureRoleInNode(Role.kRoleAdjustable);
        when(mComponent.hasProperty(PropertyKey.kPropertyAccessibilityAdjustableRange)).thenReturn(true);
        when(mComponent.getAccessibilityAdjustableRange()).thenReturn(ADJUSTABLE_RANGE);

        mAccessibilityDelegate.onInitializeAccessibilityNodeInfo(mView, mNode);

        assertEquals(ADJUSTABLE_RANGE.minValue(), mNode.getRangeInfo().getMin(), 0.001);
        assertEquals(ADJUSTABLE_RANGE.maxValue(), mNode.getRangeInfo().getMax(), 0.001);
        assertEquals(ADJUSTABLE_RANGE.currentValue(), mNode.getRangeInfo().getCurrent(), 0.001);
    }

    @Test
    public void test_nodeRangeInfoNotSetIfComponentNotAdjustable() {
        when(mComponent.hasProperty(PropertyKey.kPropertyAccessibilityAdjustableRange)).thenReturn(true);

        mAccessibilityDelegate.onInitializeAccessibilityNodeInfo(mView, mNode);

        assertNull(mNode.getRangeInfo());
    }
    
    @Test
    public void test_nodeRangeInfoNotSetIfComponentAdjustableRangeNotPresent() {
        configureRoleInNode(Role.kRoleAdjustable);
        when(mComponent.hasProperty(PropertyKey.kPropertyAccessibilityAdjustableRange)).thenReturn(false);

        mAccessibilityDelegate.onInitializeAccessibilityNodeInfo(mView, mNode);

        assertNull(mNode.getRangeInfo());
    }

    @Test
    public void test_nodeRangeInfoNotSetIfAdjustableValuePropertyPresent() {
        when(mComponent.hasProperty(PropertyKey.kPropertyAccessibilityAdjustableValue)).thenReturn(true);

        mAccessibilityDelegate.onInitializeAccessibilityNodeInfo(mView, mNode);

        assertNull(mNode.getRangeInfo());
    }

    @Test
    public void test_adjustableRole() {
        configureRoleInNode(Role.kRoleAdjustable);

        assertEquals("android.widget.SeekBar", mNode.getClassName());
        assertNull(mNode.getRoleDescription());
        assertFalse(mNode.isClickable());
        assertFalse(mNode.isCheckable());
        assertTrue(mNode.isEnabled());
    }

    @Test
    public void test_alertRole() {
        configureRoleInNode(Role.kRoleAlert);

        assertNull(mNode.getClassName());
        assertEquals("alert", mNode.getRoleDescription());
        assertFalse(mNode.isClickable());
        assertFalse(mNode.isCheckable());
        assertTrue(mNode.isEnabled());
    }

    @Test
    public void test_buttonRole() {
        configureRoleInNode(Role.kRoleButton);

        assertEquals("android.widget.Button", mNode.getClassName());
        assertEquals("button", mNode.getRoleDescription());
        assertTrue(mNode.isClickable());
        assertFalse(mNode.isCheckable());
        assertTrue(mNode.isEnabled());
    }

    @Test
    public void test_checkboxRole() {
        configureRoleInNode(Role.kRoleCheckBox);

        assertEquals("android.widget.CheckBox", mNode.getClassName());
        assertNull(mNode.getRoleDescription());
        assertTrue(mNode.isClickable());
        assertTrue(mNode.isCheckable());
        assertTrue(mNode.isEnabled());
    }

    @Test
    public void test_comboBoxRole() {
        configureRoleInNode(Role.kRoleComboBox);

        assertNull(mNode.getClassName());
        assertEquals("combo box", mNode.getRoleDescription());
        assertTrue(mNode.isClickable());
        assertFalse(mNode.isCheckable());
        assertTrue(mNode.isEnabled());
    }

    @Test
    public void test_headerRole() {
        configureRoleInNode(Role.kRoleHeader);

        assertNull(mNode.getClassName());
        assertEquals("header", mNode.getRoleDescription());
        assertFalse(mNode.isClickable());
        assertFalse(mNode.isCheckable());
        assertTrue(mNode.isEnabled());
    }

    @Test
    public void test_imageRole() {
        configureRoleInNode(Role.kRoleImage);

        assertEquals("android.widget.ImageView", mNode.getClassName());
        assertEquals("image", mNode.getRoleDescription());
        assertFalse(mNode.isClickable());
        assertFalse(mNode.isCheckable());
        assertTrue(mNode.isEnabled());
    }

    @Test
    public void test_imageButtonRole() {
        configureRoleInNode(Role.kRoleImageButton);

        assertEquals("android.widget.ImageButton", mNode.getClassName());
        assertEquals("image button", mNode.getRoleDescription());
        assertTrue(mNode.isClickable());
        assertFalse(mNode.isCheckable());
        assertTrue(mNode.isEnabled());
    }

    @Test
    public void test_keyboardKeyRole() {
        configureRoleInNode(Role.kRoleKeyboardKey);

        assertEquals("android.inputmethodservice.Keyboard$Key", mNode.getClassName());
        assertNull(mNode.getRoleDescription());
        assertTrue(mNode.isClickable());
        assertFalse(mNode.isCheckable());
        assertTrue(mNode.isEnabled());
    }

    @Test
    public void test_linkRole() {
        configureRoleInNode(Role.kRoleLink);

        assertNull(mNode.getClassName());
        assertEquals("link", mNode.getRoleDescription());
        assertTrue(mNode.isClickable());
        assertFalse(mNode.isCheckable());
        assertTrue(mNode.isEnabled());
    }

    @Test
    public void test_menuRole() {
        configureRoleInNode(Role.kRoleMenu);

        assertNull(mNode.getClassName());
        assertEquals("menu", mNode.getRoleDescription());
        assertFalse(mNode.isClickable());
        assertFalse(mNode.isCheckable());
        assertTrue(mNode.isEnabled());
    }

    @Test
    public void test_menuItemRole() {
        configureRoleInNode(Role.kRoleMenuItem);

        assertNull(mNode.getClassName());
        assertEquals("menu item", mNode.getRoleDescription());
        assertFalse(mNode.isClickable());
        assertFalse(mNode.isCheckable());
        assertTrue(mNode.isEnabled());
    }

    @Test
    public void test_progressBarRole() {
        configureRoleInNode(Role.kRoleProgressBar);

        assertNull(mNode.getClassName());
        assertEquals("progress bar", mNode.getRoleDescription());
        assertFalse(mNode.isClickable());
        assertFalse(mNode.isCheckable());
        assertTrue(mNode.isEnabled());
    }

    @Test
    public void test_radioRole() {
        configureRoleInNode(Role.kRoleRadio);

        assertEquals("android.widget.RadioButon", mNode.getClassName());
        assertNull(mNode.getRoleDescription());
        assertTrue(mNode.isClickable());
        assertTrue(mNode.isCheckable());
        assertTrue(mNode.isEnabled());
    }

    @Test
    public void test_radioGroupRole() {
        configureRoleInNode(Role.kRoleRadioGroup);

        assertNull(mNode.getClassName());
        assertEquals("radio group", mNode.getRoleDescription());
        assertFalse(mNode.isClickable());
        assertFalse(mNode.isCheckable());
        assertTrue(mNode.isEnabled());
    }

    @Test
    public void test_scrollbarRole() {
        configureRoleInNode(Role.kRoleScrollBar);

        assertNull(mNode.getClassName());
        assertEquals("scroll bar", mNode.getRoleDescription());
        assertFalse(mNode.isClickable());
        assertFalse(mNode.isCheckable());
        assertTrue(mNode.isEnabled());
    }

    @Test
    public void test_searchRole() {
        configureRoleInNode(Role.kRoleSearch);

        assertEquals("android.widget.EditText", mNode.getClassName());
        assertEquals("search", mNode.getRoleDescription());
        assertTrue(mNode.isClickable());
        assertFalse(mNode.isCheckable());
        assertTrue(mNode.isEnabled());
    }

    @Test
    public void test_spinButtonRole() {
        configureRoleInNode(Role.kRoleSpinButton);

        assertEquals("android.widget.Spinner", mNode.getClassName());
        assertNull(mNode.getRoleDescription());
        assertTrue(mNode.isClickable());
        assertFalse(mNode.isCheckable());
        assertTrue(mNode.isEnabled());
    }

    @Test
    public void test_switchRole() {
        configureRoleInNode(Role.kRoleSwitch);

        assertEquals("android.widget.Switch", mNode.getClassName());
        assertNull(mNode.getRoleDescription());
        assertTrue(mNode.isClickable());
        assertTrue(mNode.isCheckable());
        assertTrue(mNode.isEnabled());
    }

    @Test
    public void test_tabRole() {
        configureRoleInNode(Role.kRoleTab);

        assertNull(mNode.getClassName());
        assertEquals("tab", mNode.getRoleDescription());
        assertFalse(mNode.isClickable());
        assertFalse(mNode.isCheckable());
        assertTrue(mNode.isEnabled());
    }

    @Test
    public void test_tabListRole() {
        configureRoleInNode(Role.kRoleTabList);

        assertNull(mNode.getClassName());
        assertEquals("tab list", mNode.getRoleDescription());
        assertFalse(mNode.isClickable());
        assertFalse(mNode.isCheckable());
        assertTrue(mNode.isEnabled());
    }

    @Test
    public void test_textRole() {
        configureRoleInNode(Role.kRoleText);

        assertEquals("android.widget.TextView", mNode.getClassName());
        assertNull(mNode.getRoleDescription());
        assertFalse(mNode.isClickable());
        assertFalse(mNode.isCheckable());
        assertTrue(mNode.isEnabled());
    }

    @Test
    public void test_timerRole() {
        configureRoleInNode(Role.kRoleTimer);

        assertNull(mNode.getClassName());
        assertEquals("timer", mNode.getRoleDescription());
        assertFalse(mNode.isClickable());
        assertFalse(mNode.isCheckable());
        assertTrue(mNode.isEnabled());
    }

    @Test
    public void test_toolbarRole() {
        configureRoleInNode(Role.kRoleToolBar);

        assertNull(mNode.getClassName());
        assertEquals("tool bar", mNode.getRoleDescription());
        assertFalse(mNode.isClickable());
        assertFalse(mNode.isCheckable());
        assertTrue(mNode.isEnabled());
    }

    @Test
    public void test_noneRole() {
        configureRoleInNode(Role.kRoleNone);

        // should remain intact
        assertNull(mNode.getClassName());
        assertNull(mNode.getRoleDescription());
        assertFalse(mNode.isClickable());
        assertFalse(mNode.isCheckable());
        assertTrue(mNode.isEnabled());
    }

    @Test
    public void test_node_isChecked() {
        // a role as checkable
        when(mComponent.isChecked()).thenReturn(true);
        configureRoleInNode(Role.kRoleRadio);

        assertTrue(mNode.isChecked());

        // a role not as checkable
        configureRoleInNode(Role.kRoleButton);

        assertFalse(mNode.isChecked());
    }

    @Test
    public void test_node_isEnabled() {
        when(mComponent.isDisabled()).thenReturn(true);
        configureRoleInNode(Role.kRoleButton);

        assertFalse(mNode.isEnabled());
    }

    @Test
    public void test_noActions() {
        mAccessibilityDelegate.onInitializeAccessibilityNodeInfo(mView, mNode);

        assertFalse(mNode.isFocusable());
        assertEquals(0, mNode.getActionList().size());
    }

    @Test
    public void test_activateAction() {
        configureActionInNode("activate", "Reply to user");

        AccessibilityNodeInfoCompat.AccessibilityActionCompat actualActionNode = mNode.getActionList().get(0);
        assertEquals(AccessibilityNodeInfoCompat.ACTION_CLICK, actualActionNode.getId());
        assertEquals("Reply to user", actualActionNode.getLabel());
    }

    @Test
    public void test_doubleTapAction() {
        configureActionInNode("doubletap", "Add rating");

        AccessibilityNodeInfoCompat.AccessibilityActionCompat actualActionNode = mNode.getActionList().get(0);
        assertEquals(R.id.action_double_tap, actualActionNode.getId());
        assertEquals("Add rating", actualActionNode.getLabel());
    }

    @Test
    public void test_longPressAction() {
        configureActionInNode("longpress", "Message to server");

        AccessibilityNodeInfoCompat.AccessibilityActionCompat actualActionNode = mNode.getActionList().get(0);
        assertEquals(AccessibilityNodeInfoCompat.ACTION_LONG_CLICK, actualActionNode.getId());
        assertEquals("Message to server", actualActionNode.getLabel());
    }

    @Test
    public void test_swipeAwayAction() {
        configureActionInNode("swipeaway", "Play song");

        AccessibilityNodeInfoCompat.AccessibilityActionCompat actualActionNode = mNode.getActionList().get(0);
        assertEquals(R.id.action_swipe_away, actualActionNode.getId());
        assertEquals("Play song", actualActionNode.getLabel());
    }

    @Test
    public void test_incrementAction() {
        configureRoleInNode(Role.kRoleAdjustable);
        configureActionInNode("increment", "Increment value");

        AccessibilityNodeInfoCompat.AccessibilityActionCompat actualActionNode = mNode.getActionList().get(0);
        assertEquals(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD, actualActionNode.getId());
        assertEquals("Increment value", actualActionNode.getLabel());
    }

    @Test
    public void test_decrementAction() {
        configureRoleInNode(Role.kRoleAdjustable);
        configureActionInNode("decrement", "Decrement value");

        AccessibilityNodeInfoCompat.AccessibilityActionCompat actualActionNode = mNode.getActionList().get(0);
        assertEquals(AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD, actualActionNode.getId());
        assertEquals("Decrement value", actualActionNode.getLabel());
    }

    @Test
    public void test_customAction() {
        configureActionInNode("thumbsup", "Mark positively");

        AccessibilityNodeInfoCompat.AccessibilityActionCompat actualActionNode = mNode.getActionList().get(0);
        assertEquals(0x3f000000, actualActionNode.getId()); // simulating resource id
        assertEquals("Mark positively", actualActionNode.getLabel());
    }

    @Test
    public void test_multipleActions() {
        // configure and run test
        configureActionsInNode(new Pair[] {
                new Pair("thumbsup", "Mark positively"),
                new Pair("activate", "Reply to user"),
                new Pair("highfive", "Congratulations"),
                new Pair("clapping", "Applause"),
        });

        // assertions
        List<AccessibilityNodeInfoCompat.AccessibilityActionCompat> actualActionNodes = mNode.getActionList();

        assertEquals(0x3f000000, actualActionNodes.get(0).getId()); // simulating resource id
        assertEquals("Mark positively", actualActionNodes.get(0).getLabel());
        assertEquals(AccessibilityNodeInfoCompat.ACTION_CLICK, actualActionNodes.get(1).getId());
        assertEquals("Reply to user", actualActionNodes.get(1).getLabel());
        assertEquals(0x3f000000 + 1, actualActionNodes.get(2).getId()); // simulating resource id
        assertEquals("Congratulations", actualActionNodes.get(2).getLabel());
        assertEquals(0x3f000000 + 2, actualActionNodes.get(3).getId()); // simulating resource id
        assertEquals("Applause", actualActionNodes.get(3).getLabel());
    }

    @Test
    public void test_multipleActions_multipleComponents() {
        Component c2 = mock(Component.class);
        View v2 = mock(View.class);
        AccessibilityNodeInfoCompat n2 = AccessibilityNodeInfoCompat.obtain();
        AccessibilityDelegateCompat ad2 = new APLAccessibilityDelegate(c2, getApplication());

        // configure and run tests
        when(c2.getRole()).thenReturn(Role.kRoleNone); //

        configureActionsInNode(new Pair[] {
                new Pair("thumbsup", "Mark positively"),
                new Pair("activate", "Reply to user"),
        }, mAccessibilityDelegate, mComponent, mView, mNode);

        configureActionsInNode(new Pair[] {
                new Pair("highfive", "Congratulations"),
                new Pair("clapping", "Applause"),
        }, ad2, c2, v2, n2);

        // assertions
        List<AccessibilityNodeInfoCompat.AccessibilityActionCompat> actualActionNodes1 = mNode.getActionList();

        assertEquals(0x3f000000, actualActionNodes1.get(0).getId()); // simulating resource id
        assertEquals("Mark positively", actualActionNodes1.get(0).getLabel());
        assertEquals(AccessibilityNodeInfoCompat.ACTION_CLICK, actualActionNodes1.get(1).getId());
        assertEquals("Reply to user", actualActionNodes1.get(1).getLabel());

        // resources ids generated should not conflict among other components
        List<AccessibilityNodeInfoCompat.AccessibilityActionCompat> actualActionNodes2 = n2.getActionList();

        assertEquals(0x3f000000 + 1, actualActionNodes2.get(0).getId()); // simulating resource id
        assertEquals("Congratulations", actualActionNodes2.get(0).getLabel());
        assertEquals(0x3f000000 + 2, actualActionNodes2.get(1).getId()); // simulating resource id
        assertEquals("Applause", actualActionNodes2.get(1).getLabel());
    }

    @Test
    public void test_multipleActions_duplicated() {
        // configure and run test
        configureActionsInNode(new Pair[] {
                new Pair("sing", "Rabbits love carrots"),
                new Pair("activate", "Doggos love walkies"),
                new Pair("activate", "Cats are fluffy"),
                new Pair("sing", "Foxes like laugh"),
                new Pair("activate", "Ponies are pretty"),
        });

        // assertions
        List<AccessibilityNodeInfoCompat.AccessibilityActionCompat> actualActionNodes = mNode.getActionList();

        assertEquals(2,  actualActionNodes.size());
        assertEquals(0x3f000000, actualActionNodes.get(0).getId()); // simulating resource id
        assertEquals("Rabbits love carrots", actualActionNodes.get(0).getLabel());
        assertEquals(AccessibilityNodeInfoCompat.ACTION_CLICK, actualActionNodes.get(1).getId());
        assertEquals("Doggos love walkies", actualActionNodes.get(1).getLabel());
    }

    @Test
    public void test_updateComponent_invoke() {
        configureActionInNode("activate", "Reply to user");

        // "activate" in APL is mapped to ACTION_CLICK in Android
        boolean result = mAccessibilityDelegate.performAccessibilityAction(mView, AccessibilityNodeInfoCompat.ACTION_CLICK, Bundle.EMPTY);

        assertTrue(result);
        verify(mComponent).update(UpdateType.kUpdateAccessibilityAction, "activate"); // it should call update with APL Action's name
    }

    @Test
    public void test_updateComponent_noInvoke() {
        configureActionInNode("activate", "Reply to user");

        // ACTION_FOCUS as actionId is not a valid to call "component.update()"
        boolean result = mAccessibilityDelegate.performAccessibilityAction(mView, AccessibilityNodeInfoCompat.ACTION_FOCUS, Bundle.EMPTY);

        assertFalse(result);
        verify(mComponent, never()).update(eq(UpdateType.kUpdateAccessibilityAction), anyString()); // it should NOT call update with APL Action's name
    }

    private void configureRoleInNode(Role role) {
        when(mComponent.getRole()).thenReturn(role);
        mAccessibilityDelegate.onInitializeAccessibilityNodeInfo(mView, mNode);
    }

    private void configureActionInNode(String name, String label) {
        configureActionsInNode(new Pair[] { new Pair(name, label) });
    }

    private void configureActionsInNode(Pair<String, String>[] actionData) { // tuple data: name, label
        configureActionsInNode(actionData, mAccessibilityDelegate, mComponent, mView, mNode);
    }

    private void configureActionsInNode(Pair<String, String>[] actionData, // tuple data: name, label
                                        AccessibilityDelegateCompat accessibilityDelegate,
                                        Component mockComponent, View view, AccessibilityNodeInfoCompat node) {
        List<AccessibilityActions.AccessibilityAction> actionList = new ArrayList<>();

        for (Pair<String, String> p : actionData) {
            actionList.add(AccessibilityActions.AccessibilityAction.create(p.first, p.second));
        }
        AccessibilityActions accessibilityActions = new AccessibilityActions() {
            @Override
            public List<AccessibilityAction> list() {
                return actionList;
            }
        };

        when(mockComponent.getAccessibilityActions()).thenReturn(accessibilityActions);

        accessibilityDelegate.onInitializeAccessibilityNodeInfo(view, node);
    }
}
