/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.VisibleForTesting;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.amazon.apl.android.primitive.AccessibilityActions;
import com.amazon.apl.enums.PropertyKey;
import com.amazon.apl.enums.Role;
import com.amazon.apl.enums.UpdateType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility class that handles the addition of a "role" and "actions" for accessibility to an
 * AccessibilityNodeInfo.
 */
public class APLAccessibilityDelegate<C extends Component> extends AccessibilityDelegateCompat {

    private static final int ACTION_BASE_ID = 0x3f000000;
    private static final Map<String, Integer> STANDARD_ACTION_MAP;  // apl actionName -> android actionId
    private static final Map<String, Integer> ADJUSTABLE_STANDARD_ACTION_MAP;

    private static int sCustomActionCount = ACTION_BASE_ID;

    protected final C mComponent;
    private final Context mContext;
    private final Map<Integer, String> mRegisteredActionIdToActionNameMap;

    static {
        STANDARD_ACTION_MAP = new HashMap<>();
        STANDARD_ACTION_MAP.put("activate", AccessibilityNodeInfoCompat.ACTION_CLICK);
        STANDARD_ACTION_MAP.put("doubletap", R.id.action_double_tap);
        STANDARD_ACTION_MAP.put("longpress", AccessibilityNodeInfoCompat.ACTION_LONG_CLICK);
        STANDARD_ACTION_MAP.put("scrollbackward", AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD);
        STANDARD_ACTION_MAP.put("scrollforward", AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
        STANDARD_ACTION_MAP.put("swipeaway", R.id.action_swipe_away);

        ADJUSTABLE_STANDARD_ACTION_MAP = new HashMap<>();
        ADJUSTABLE_STANDARD_ACTION_MAP.put("activate", AccessibilityNodeInfoCompat.ACTION_CLICK);
        ADJUSTABLE_STANDARD_ACTION_MAP.put("doubletap", R.id.action_double_tap);
        ADJUSTABLE_STANDARD_ACTION_MAP.put("longpress", AccessibilityNodeInfoCompat.ACTION_LONG_CLICK);
        ADJUSTABLE_STANDARD_ACTION_MAP.put("decrement", AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD);
        ADJUSTABLE_STANDARD_ACTION_MAP.put("increment", AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
        ADJUSTABLE_STANDARD_ACTION_MAP.put("swipeaway", R.id.action_swipe_away);
    }

    protected APLAccessibilityDelegate(C c, Context context) {
        mComponent = c;
        mContext = context;
        mRegisteredActionIdToActionNameMap = new HashMap<>();
    }

    public static APLAccessibilityDelegate create(Component component, Context context) {
        return new APLAccessibilityDelegate(component, context);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
        super.onInitializeAccessibilityNodeInfo(host, info);
        // TODO: Currently APL does not support Accessibility Scroll
        setRole(info);
        setActions(info);
        setResourceId(info);
        setText(info);
        setRangeInfo(info);
    }

    @Override
    public boolean performAccessibilityAction(View host, int action, Bundle args) {
        if (mRegisteredActionIdToActionNameMap.containsKey(action)) {
            mComponent.update(UpdateType.kUpdateAccessibilityAction, mRegisteredActionIdToActionNameMap.get(action));
            return true;
        }

        return super.performAccessibilityAction(host, action, args);
    }

    @VisibleForTesting
    void resetCustomActionCount() {
        sCustomActionCount = ACTION_BASE_ID;
    }

    private void setActions(AccessibilityNodeInfoCompat info) {
        AccessibilityActions actions = mComponent.getAccessibilityActions();
        Set<String> actionSet = new HashSet<>();
        Map<String, Integer> actionMap = STANDARD_ACTION_MAP;

        if (mComponent.getRole() == Role.kRoleAdjustable) {
            actionMap = ADJUSTABLE_STANDARD_ACTION_MAP;
        }

        for (AccessibilityActions.AccessibilityAction action : actions) {
            // ignore repeated action names. Consider only the first one.
            if (actionSet.contains(action.name()))
                continue;

            int actionId;
            if (actionMap.containsKey(action.name())) {
                actionId = actionMap.get(action.name());
            } else {
                actionId = sCustomActionCount++;
            }

            mRegisteredActionIdToActionNameMap.put(actionId, action.name());
            actionSet.add(action.name());

            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(actionId, action.label()));
        }
    }

    private void setRole(AccessibilityNodeInfoCompat info) {
        String className = null;
        String description = null;
        boolean isClickable = false;
        boolean isCheckable = false;

        // TODO: For specific components, we’ll likely need to add additional accessibility information,
        //       then we can dive into each component individually to see what needs to be added in the view host.
        switch (mComponent.getRole()) {
            case kRoleAdjustable:
                className = "android.widget.SeekBar";
                break;
            case kRoleAlert:
                description = mContext.getString(R.string.accessibility_role_alert_description);
                break;
            case kRoleButton:
                className = "android.widget.Button";
                description = mContext.getString(R.string.accessibility_role_button_description);
                isClickable = true;
                break;
            case kRoleCheckBox:
                className = "android.widget.CheckBox";
                isClickable = true;
                isCheckable = true;
                break;
            case kRoleComboBox:
                description = mContext.getString(R.string.accessibility_role_combobox_description);
                isClickable = true;
                break;
            case kRoleHeader:
                description = mContext.getString(R.string.accessibility_role_header_description);
                break;
            case kRoleImage:
                className = "android.widget.ImageView";
                description = mContext.getString(R.string.accessibility_role_image_description);
                break;
            case kRoleImageButton:
                className = "android.widget.ImageButton";
                description = mContext.getString(R.string.accessibility_role_imagebutton_description);
                isClickable = true;
                break;
            case kRoleKeyboardKey:
                className = "android.inputmethodservice.Keyboard$Key";
                isClickable = true;
                break;
            case kRoleLink:
                description = mContext.getString(R.string.accessibility_role_link_description);
                isClickable = true;
                break;
            case kRoleMenu:
                description = mContext.getString(R.string.accessibility_role_menu_description);
                break;
            case kRoleMenuItem:
                description = mContext.getString(R.string.accessibility_role_menuitem_description);
                break;
            case kRoleProgressBar:
                description = mContext.getString(R.string.accessibility_role_progressbar_description);
                break;
            case kRoleRadio:
                className = "android.widget.RadioButon";
                isClickable = true;
                isCheckable = true;
                break;
            case kRoleRadioGroup:
                description = mContext.getString(R.string.accessibility_role_radiogroup_description);
                break;
            case kRoleScrollBar:
                description = mContext.getString(R.string.accessibility_role_scrollbar_description);
                break;
            case kRoleSearch:
                className = "android.widget.EditText";
                description = mContext.getString(R.string.accessibility_role_search_description);
                isClickable = true;
                break;
            case kRoleSpinButton:
                className = "android.widget.Spinner";
                isClickable = true;
                break;
            case kRoleSwitch:
                className = "android.widget.Switch";
                isClickable = true;
                isCheckable = true;
                break;
            case kRoleTab:
                description = mContext.getString(R.string.accessibility_role_tab_description);
                break;
            case kRoleTabList:
                description = mContext.getString(R.string.accessibility_role_tablist_description);
                break;
            case kRoleText:
                className = "android.widget.TextView";
                break;
            case kRoleTimer:
                description = mContext.getString(R.string.accessibility_role_timer_description);
                break;
            case kRoleToolBar:
                description = mContext.getString(R.string.accessibility_role_toolbar_description);
                break;
            case kRoleNone:
            default:
        }

        if (className != null) {
            info.setClassName(className);
        }
        if (description != null) {
            info.setRoleDescription(description);
        }
        info.setClickable(isClickable);
        info.setCheckable(isCheckable);
        info.setChecked(isCheckable ? mComponent.isChecked() : false);
        info.setEnabled(!mComponent.isDisabled());
    }

    /**
     * Sets the resource-id field on the node to the component id set by the
     * APL document author if present. Used for finding nodes in automated tests.
     * @param nodeInfo the node to set the value on
     */
    private void setResourceId(AccessibilityNodeInfoCompat nodeInfo) {
        String componentId = mComponent.getId();

        if (!"".equals(componentId)) {
            nodeInfo.setViewIdResourceName(componentId);
        }
    }

    private void setText(AccessibilityNodeInfoCompat nodeInfo) {
        if (mComponent.hasTextProperty()) {
            nodeInfo.setText(mComponent.getAccessibilityNodeText());
        }
    }

    private void setRangeInfo(AccessibilityNodeInfoCompat nodeInfo) {
        if (mComponent.hasProperty(PropertyKey.kPropertyAccessibilityAdjustableValue) &&
            (mComponent.getAccessibilityAdjustableValue() != null && !mComponent.getAccessibilityAdjustableValue().isEmpty()))
            return;

        if (mComponent.getRole() == Role.kRoleAdjustable && mComponent.hasProperty(PropertyKey.kPropertyAccessibilityAdjustableRange)) {
            nodeInfo.setRangeInfo(AccessibilityNodeInfoCompat.RangeInfoCompat.obtain(
                AccessibilityNodeInfoCompat.RangeInfoCompat.RANGE_TYPE_FLOAT,
                mComponent.getAccessibilityAdjustableRange().minValue(),
                mComponent.getAccessibilityAdjustableRange().maxValue(),
                mComponent.getAccessibilityAdjustableRange().currentValue()));
        }
    }
}
