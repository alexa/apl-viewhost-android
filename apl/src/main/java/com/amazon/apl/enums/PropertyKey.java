/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

/*
 * AUTOGENERATED FILE. DO NOT MODIFY!
 * This file is autogenerated by enumgen.
 */

package com.amazon.apl.enums;

import android.util.SparseArray;

public enum PropertyKey implements APLEnum {

    /// SequenceComponent scrolling direction (see #ScrollDirection)
    kPropertyScrollDirection(0),
    /// An array of accessibility actions associated with this component
    kPropertyAccessibilityActions(1),
    /// An array of assigned accessibility actions
    kPropertyAccessibilityActionsAssigned(2),
    /// Range configuration for a TouchableComponent with an adjustable role
    kPropertyAccessibilityAdjustableRange(3),
    /// Current value for a TouchableComponent with an adjustable role
    kPropertyAccessibilityAdjustableValue(4),
    /// Component accessibility label
    kPropertyAccessibilityLabel(5),
    /// ImageComponent and VectorGraphicComponent alignment (see #ImageAlign, #VectorGraphicAlign)
    kPropertyAlign(6),
    /// ContainerComponent alignment of items (see #FlexboxAlign)
    kPropertyAlignItems(7),
    /// ContainerComponent child alignment (see #FlexboxAlign)
    kPropertyAlignSelf(8),
    /// VideoComponent audio track (see #AudioTrack)
    kPropertyAudioTrack(9),
    /// VideoComponent autoplay
    kPropertyAutoplay(10),
    /// VideoComponent muted
    kPropertyMuted(11),
    /// FrameComponent background color
    kPropertyBackgroundColor(12),
    /// FrameComponent background assigned
    kPropertyBackgroundAssigned(13),
    /// FrameComponent background
    kPropertyBackground(14),
    /// FrameComponent border bottom-left radius (input only)
    kPropertyBorderBottomLeftRadius(15),
    /// FrameComponent border bottom-right radius (input only)
    kPropertyBorderBottomRightRadius(16),
    /// FrameComponent | EditTextComponent border color
    kPropertyBorderColor(17),
    /// ImageComponent border radius; also used by FrameComponent to set overall border radius (input only)
    kPropertyBorderRadius(18),
    /// FrameComponent border radii (output only)
    kPropertyBorderRadii(19),
    /// FrameComponent | EditTextComponent width of the border stroke (input only)
    kPropertyBorderStrokeWidth(20),
    /// FrameComponent border top-left radius (input only)
    kPropertyBorderTopLeftRadius(21),
    /// FrameComponent border top-right radius (input only)
    kPropertyBorderTopRightRadius(22),
    /// FrameComponent | EditTextComponent border width
    kPropertyBorderWidth(23),
    /// ContainerComponent child absolute bottom position
    kPropertyBottom(24),
    /// Component bounding rectangle (output only)
    kPropertyBounds(25),
    /// Sequence preserve scroll position by ID
    kPropertyCenterId(26),
    /// Sequence preserve scroll position by index
    kPropertyCenterIndex(27),
    /// GridSequenceComponent child height(s)
    kPropertyChildHeight(28),
    /// GridSequenceComponent child width(s)
    kPropertyChildWidth(29),
    /// Component checked state
    kPropertyChecked(30),
    /// TextComponent | EditTextComponent color
    kPropertyColor(31),
    /// TextComponent color for karaoke target
    kPropertyColorKaraokeTarget(32),
    /// TextComponent color for text that isn't subject to Karaoke
    kPropertyColorNonKaraoke(33),
    /// PagerComponent current page display
    kPropertyCurrentPage(34),
    /// Component description
    kPropertyDescription(35),
    /// ContainerComponent layout direction (see #ContainerDirection)
    kPropertyDirection(36),
    /// Component disabled state
    kPropertyDisabled(37),
    /// Component general display (see #Display)
    kPropertyDisplay(38),
    /// FrameComponent | EditTextComponent drawn border width (output only)
    kPropertyDrawnBorderWidth(39),
    /// Embedded document state
    kPropertyEmbeddedDocument(40),
    /// ContainerComponent child absolute right position for LTR layout or left position for RTL layout
    kPropertyEnd(41),
    /// Component array of opaque entity data
    kPropertyEntities(42),
    /// HostComponent Environment overrides applicable to the embedded document
    kPropertyEnvironment(43),
    /// SequenceComponent fast scroll scaling setting
    kPropertyFastScrollScale(44),
    /// ImageComponent array of filters
    kPropertyFilters(45),
    /// Sequence preserve scroll position by ID
    kPropertyFirstId(46),
    /// Sequence preserve scroll position by index
    kPropertyFirstIndex(47),
    /// Property that identifies that component is focusable and as a result of it navigable
    kPropertyFocusable(48),
    /// TextComponent | EditTextComponent valid font families
    kPropertyFontFamily(49),
    /// TextComponent font size
    kPropertyFontSize(50),
    /// TextComponent font style (see #FontStyle)
    kPropertyFontStyle(51),
    /// TextComponent | EditTextComponent font weight
    kPropertyFontWeight(52),
    /// Component handler for tick
    kPropertyHandleTick(53),
    /// Component handler for visibility changes
    kPropertyHandleVisibilityChange(54),
    /// EditTextComponent highlight color behind selected text.
    kPropertyHighlightColor(55),
    /// EditTextComponent hint text,displayed when no text has been entered
    kPropertyHint(56),
    /// EditTextComponent color for hint text
    kPropertyHintColor(57),
    /// EditTextComponent style of the hint font
    kPropertyHintStyle(58),
    /// EditTextComponent weight of the hint font
    kPropertyHintWeight(59),
    /// Gesture handlers
    kPropertyGestures(60),
    /// VectorGraphicComponent calculated graphic data (output only)
    kPropertyGraphic(61),
    /// ContainerComponent child flexbox grow
    kPropertyGrow(62),
    /// Handlers to check on key down events.
    kPropertyHandleKeyDown(63),
    /// Handlers to check on key up events.
    kPropertyHandleKeyUp(64),
    /// Component height
    kPropertyHeight(65),
    /// Component assigned ID
    kPropertyId(66),
    /// PagerComponent initial page to display
    kPropertyInitialPage(67),
    /// Component calculated inner bounds rectangle [applies padding and border] (output only)
    kPropertyInnerBounds(68),
    /// GridSequenceComponent number of Columns if vertical, number of Rows if horizontal
    kPropertyItemsPerCourse(69),
    /// ContainerComponent flexbox content justification (see #FlexboxJustifyContent)
    kPropertyJustifyContent(70),
    /// EditTextComponent the keyboard behavior on component gaining focus
    kPropertyKeyboardBehaviorOnFocus(71),
    /// EditTextComponent keyboard type
    kPropertyKeyboardType(72),
    /// Calculated Component layout direction (output only)
    kPropertyLayoutDirection(73),
    /// Component layout direction assigned
    kPropertyLayoutDirectionAssigned(74),
    /// ContainerComponent child absolute left position
    kPropertyLeft(75),
    /// TextComponent letter spacing
    kPropertyLetterSpacing(76),
    /// TextComponent line height
    kPropertyLineHeight(77),
    /// Component maximum height
    kPropertyMaxHeight(78),
    /// EditTextComponent maximum number of characters that can be displayed
    kPropertyMaxLength(79),
    /// TextComponent maximum number of lines
    kPropertyMaxLines(80),
    /// Component maximum width
    kPropertyMaxWidth(81),
    /// VectorGraphicComponent bounding rectangle for displayed graphic (output only)
    kPropertyMediaBounds(82),
    /// State of media required by the component
    kPropertyMediaState(83),
    /// Component minimum height
    kPropertyMinHeight(84),
    /// Component minimum width
    kPropertyMinWidth(85),
    /// PagerComponent valid navigation mode (see #Navigation)
    kPropertyNavigation(86),
    /// Component to switch to when a keyboard down command is receive
    kPropertyNextFocusDown(87),
    /// Component to switch to when a keyboard forward command is received
    kPropertyNextFocusForward(88),
    /// Component to switch to when a keyboard left command is received
    kPropertyNextFocusLeft(89),
    /// Component to switch to when a keyboard right command is received
    kPropertyNextFocusRight(90),
    /// Component to switch to when a keyboard up command is received
    kPropertyNextFocusUp(91),
    /// Synthetic notification for dirty flag that the children of a component have changed (dirty only)
    kPropertyNotifyChildrenChanged(92),
    /// SequenceComponent or ContainerComponent has ordinal numbers
    kPropertyNumbered(93),
    /// SequenceComponent or ContainerComponent child numbered marker (see #Numbering)
    kPropertyNumbering(94),
    /// ActionableComponent handler when focus is lost
    kPropertyOnBlur(95),
    /// TouchableComponent handler for cancel
    kPropertyOnCancel(96),
    /// Multi-child component children changed
    kPropertyOnChildrenChanged(97),
    /// TouchableComponent handler for down
    kPropertyOnDown(98),
    /// VideoComponent handler for video end
    kPropertyOnEnd(99),
    /// MediaComponent handler for failure
    kPropertyOnFail(100),
    /// ActionableComponent handler when focus is gained
    kPropertyOnFocus(101),
    /// Component handler invoked on mount and on layout changes.
    kPropertyOnLayout(102),
    /// MediaComponent handler for when the media loads
    kPropertyOnLoad(103),
    /// to the hierarchy.
    kPropertyOnMount(104),
    /// TouchableComponent handler for move
    kPropertyOnMove(105),
    /// Component handler for speechmarks
    kPropertyOnSpeechMark(106),
    /// PagerComponent handler for the page change animation
    kPropertyHandlePageMove(107),
    /// PagerComponent handler for when the page changes
    kPropertyOnPageChanged(108),
    /// VideoComponent handler for video pause
    kPropertyOnPause(109),
    /// VideoComponent handler for video play
    kPropertyOnPlay(110),
    /// TouchableComponent handler for press
    kPropertyOnPress(111),
    /// ScrollViewComponent or SequenceComponent handler for scroll events.
    kPropertyOnScroll(112),
    /// EditTextComponent commands to execute when the submit button is pressed.
    kPropertyOnSubmit(113),
    /// EditTextComponent Commands to execute when the text changes from a user event.
    kPropertyOnTextChange(114),
    /// Component handler invoked on text layout changes.
    kPropertyOnTextLayout(115),
    /// TouchableComponent handler for up
    kPropertyOnUp(116),
    /// VideoComponent handler for video time updates
    kPropertyOnTimeUpdate(117),
    /// VideoComponent handler for media errors
    kPropertyOnTrackFail(118),
    /// VideoComponent handler for media ready events
    kPropertyOnTrackReady(119),
    /// VideoComponent handler for video track updates
    kPropertyOnTrackUpdate(120),
    /// Component opacity (just the current opacity; not the cumulative)
    kPropertyOpacity(121),
    /// ImageComponent overlay color
    kPropertyOverlayColor(122),
    /// ImageComponent overlay gradient
    kPropertyOverlayGradient(123),
    /// Component padding [user-defined array of values]
    kPropertyPadding(124),
    /// Component bottom padding
    kPropertyPaddingBottom(125),
    /// Component layoutDirection aware end padding
    kPropertyPaddingEnd(126),
    /// Component left padding
    kPropertyPaddingLeft(127),
    /// Component right padding
    kPropertyPaddingRight(128),
    /// Component top padding
    kPropertyPaddingTop(129),
    /// Component layoutDirection aware start padding
    kPropertyPaddingStart(130),
    /// Pager page direction
    kPropertyPageDirection(131),
    /// Pager virtual property for the ID of the current page
    kPropertyPageId(132),
    /// Pager virtual property for the index of the current page
    kPropertyPageIndex(133),
    /// Explicit parameter-passing property map for HostComponent and VectorGraphicComponent
    kPropertyParameters(134),
    /// VideoComponent current playing state
    kPropertyPlayingState(135),
    /// ContainerComponent child absolute or relative position (see #Position)
    kPropertyPosition(136),
    /// Controls whether the component can be the target of touch events
    kPropertyPointerEvents(137),
    /// Component properties to preserve over configuration changes
    kPropertyPreserve(138),
    /// TextComponent range for karaoke target
    kPropertyRangeKaraokeTarget(139),
    /// The unique identifier of the resource associated with extension component
    kPropertyResourceId(140),
    // ExtensionComponent handler on error
    kPropertyResourceOnFatalError(141),
    // The state of the rendered resource of an extension component
    kPropertyResourceState(142),
    /// The type of the system resource for an extension component
    kPropertyResourceType(143),
    /// ContainerComponent child absolute right position
    kPropertyRight(144),
    /// Component accessibility role
    kPropertyRole(145),
    /// ImageComponent, VideoComponent, and VectorGraphicComponent scale property (see #ImageScale, #VectorGraphicScale, #VideoScale)
    kPropertyScale(146),
    /// VideoComponent screen lock
    kPropertyScreenLock(147),
    /// SequenceComponent scroll animation setting
    kPropertyScrollAnimation(148),
    /// Scrollable preserve position by absolute scroll position
    kPropertyScrollOffset(149),
    /// Scrollable preserve position by percentage
    kPropertyScrollPercent(150),
    /// Scroll position of the Scrollable component.
    kPropertyScrollPosition(151),
    /// EditTextComponent hide characters as typed if true
    kPropertySecureInput(152),
    /// EditTextComponent the text is selected on focus when true
    kPropertySelectOnFocus(153),
    /// Component shadow color
    kPropertyShadowColor(154),
    /// Component shadow horizontal offset
    kPropertyShadowHorizontalOffset(155),
    /// Component shadow radius
    kPropertyShadowRadius(156),
    /// Component shadow vertical offset
    kPropertyShadowVerticalOffset(157),
    /// ContainerComponent child flexbox shrink property
    kPropertyShrink(158),
    /// EditTextComponent specifies approximately how many characters can be displayed
    kPropertySize(159),
    /// SequenceComponent snap location (see #Snap)
    kPropertySnap(160),
    /// ImageComponent, VideoComponent, and VectorGraphic source URL(s)
    kPropertySource(161),
    /// ContainerComponent and SequenceComponent child spacing
    kPropertySpacing(162),
    /// Component opaque speech data
    kPropertySpeech(163),
    /// ContainerComponent child absolute left position for LTR layout or right position for RTL layout
    kPropertyStart(164),
    /// EditTextComponent label of the return key
    kPropertySubmitKeyType(165),
    /// TextComponent | EditTextComponent assigned rich text
    kPropertyText(166),
    /// Calculated TextComponent horizontal alignment (output only)
    kPropertyTextAlign(167),
    /// TextComponent assigned horizontal alignment (see #TextAlign)
    kPropertyTextAlignAssigned(168),
    /// TextComponent vertical alignment (see #TextAlignVertical)
    kPropertyTextAlignVertical(169),
    /// For example to select the japanese characters of the "Noto Sans CJK" font family set this to "ja-JP"
    kPropertyLang(170),
    /// whenever a new set of tracks is assigned to the video component.
    kPropertyTrackCount(171),
    /// This property will be updated whenever the current time of the active track changes.
    kPropertyTrackCurrentTime(172),
    /// component.
    kPropertyTrackDuration(173),
    /// not playing.
    kPropertyTrackEnded(174),
    /// will be updated whenever there is a track change.
    kPropertyTrackIndex(175),
    /// Boolean property.  True if the current track in the video component is not playing.
    kPropertyTrackPaused(176),
    /// State of a media track for video component.
    kPropertyTrackState(177),
    /// Component 2D graphics transformation
    kPropertyTransform(178),
    /// Calculated Component 2D graphics transformation (output-only)
    kPropertyTransformAssigned(179),
    /// ContainerComponent child absolute top position
    kPropertyTop(180),
    /// Component user-defined properties assembled into a single map.
    kPropertyUser(181),
    /// Component width
    kPropertyWidth(182),
    /// Component handler for cursor enter
    kPropertyOnCursorEnter(183),
    /// Component handler for cursor exit
    kPropertyOnCursorExit(184),
    /// Component attached to Yoga tree and has flexbox properties calculated.
    kPropertyLaidOut(185),
    /// EditTextComponent restrict the characters that can be entered
    kPropertyValidCharacters(186),
    /// Visual hash
    kPropertyVisualHash(187),
    /// Flexbox wrap
    kPropertyWrap(188);

    private static SparseArray<PropertyKey> values = null;

    static {
        PropertyKey.values = new SparseArray<>();
        PropertyKey[] values = PropertyKey.values();
        for (PropertyKey value : values) {
            PropertyKey.values.put(value.getIndex(), value);
        }
    }

    public static PropertyKey valueOf(int idx) {
        return PropertyKey.values.get(idx);
    }

    private final int index;

    PropertyKey (int index) {
        this.index = index;
    }

    @Override
    public int getIndex() { return this.index; }
}
