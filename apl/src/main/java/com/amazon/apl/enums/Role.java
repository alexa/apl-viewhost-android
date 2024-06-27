/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

/*
 * AUTOGENERATED FILE. DO NOT MODIFY!
 * This file is autogenerated by enumgen.
 */

package com.amazon.apl.enums;

import android.util.SparseArray;

public enum Role implements APLEnum {

    kRoleNone(0),
    kRoleAdjustable(1),
    kRoleAlert(2),
    kRoleButton(3),
    kRoleCheckBox(4),
    kRoleComboBox(5),
    kRoleHeader(6),
    kRoleImage(7),
    kRoleImageButton(8),
    kRoleKeyboardKey(9),
    kRoleLink(10),
    kRoleList(11),
    kRoleListItem(12),
    kRoleMenu(13),
    kRoleMenuBar(14),
    kRoleMenuItem(15),
    kRoleProgressBar(16),
    kRoleRadio(17),
    kRoleRadioGroup(18),
    kRoleScrollBar(19),
    kRoleSearch(20),
    kRoleSpinButton(21),
    kRoleSummary(22),
    kRoleSwitch(23),
    kRoleTab(24),
    kRoleTabList(25),
    kRoleText(26),
    kRoleTimer(27),
    kRoleToolBar(28);

    private static SparseArray<Role> values = null;

    static {
        Role.values = new SparseArray<>();
        Role[] values = Role.values();
        for (Role value : values) {
            Role.values.put(value.getIndex(), value);
        }
    }

    public static Role valueOf(int idx) {
        return Role.values.get(idx);
    }

    private final int index;

    Role (int index) {
        this.index = index;
    }

    @Override
    public int getIndex() { return this.index; }
}