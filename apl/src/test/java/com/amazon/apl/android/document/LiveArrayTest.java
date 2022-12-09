/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.document;

import android.view.View;

import com.amazon.apl.android.APLJSONData;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.LiveArray;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.Text;
import com.amazon.apl.android.component.ComponentViewAdapter;
import com.amazon.apl.android.component.ComponentViewAdapterFactory;
import com.amazon.apl.enums.ComponentType;
import com.amazon.apl.enums.PropertyKey;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

public class LiveArrayTest extends LiveObjectTest {

    private LiveArray mLiveArray;
    private Text mText;

    void registerLiveData(RootConfig rootConfig) {
        rootConfig.liveData("Signs", mLiveArray);
    }

    private String getText() {
        return mText.getText();
    }

    /**
     * Compare this array with the LiveArray and the APL data in the live array.  This method
     * will handle basic types and sub-arrays.  It does not handle internal maps.
     * @param values An array of values to compare to the live array
     */
    private void assertArrayMatches(Object... values) {
        assertEquals(values.length, mLiveArray.size());
        assertEquals(mLiveArray.size(), mLiveArray.innerSize());

        for (int i = 0 ; i < mLiveArray.size() ; i++)
            assertObjectTriple(values[i], mLiveArray.get(i), mLiveArray.innerGet(i));
    }

    @Before
    public void Setup() {
        mLiveArray = LiveArray.create();
    }

    /**
     * Constructor checks
     */
    @Test
    public void test_LiveDataConstructors() {
        mLiveArray = LiveArray.create();
        assertArrayMatches();

        mLiveArray = LiveArray.create(Arrays.asList("tango", "foxtrot"));
        assertArrayMatches("tango", "foxtrot");

        mLiveArray = LiveArray.create(new String[]{"alpha", "bravo"});
        assertArrayMatches("alpha", "bravo");

        mLiveArray = LiveArray.create(new String[]{"alpha", "golf", "hotel"});
        assertArrayMatches("alpha", "golf", "hotel");
    }

    // Test content
    private static final String DOC = "{\n" +
            "    \"type\": \"APL\",\n" +
            "    \"version\": \"1.0\",\n" +
            "    \"mainTemplate\": {\n" +
            "        \"item\": {\n" +
            "            \"type\": \"Text\",\n" +
            "            \"id\": \"MyText\",\n" +
            "            \"width\": 100,\n" +
            "            \"height\": 100,\n" +
            "            \"text\": \"${Signs[0]}-${Signs[1]}\"\n" +
            "        }\n" +
            "    }\n" +
            "}";

    /**
     * Test all of the modification functions on LiveArray to verify that changing
     * the LiveArray will propagate to a data-binding expression in the document.
     */
    @Test
    public void test_LiveData() {
        mLiveArray.addAll(Arrays.asList("alpha", "bravo", "foxtrot"));
        assertArrayMatches("alpha", "bravo", "foxtrot");

        loadDocument(DOC);

        Component c = mRootContext.getTopComponent();
        assertNotNull(c);
        Assert.assertEquals(ComponentType.kComponentTypeText, c.getComponentType());
        mText = (Text) c;
        Assert.assertEquals("alpha-bravo", getText());
        assertArrayMatches("alpha", "bravo", "foxtrot");

        mLiveArray.remove(0);
        advance();
        Assert.assertEquals("bravo-foxtrot", getText());
        assertArrayMatches("bravo", "foxtrot");

        mLiveArray.add(0, "Fuzzy");
        advance();
        Assert.assertEquals("Fuzzy-bravo", getText());
        assertArrayMatches("Fuzzy", "bravo", "foxtrot");

        mLiveArray.addAll(0, Arrays.asList("golf", "hotel"));
        advance();
        Assert.assertEquals("golf-hotel", getText());
        assertArrayMatches("golf", "hotel", "Fuzzy", "bravo", "foxtrot");

        mLiveArray.removeRange(1, 2);
        advance();
        Assert.assertEquals("golf-bravo", getText());
        assertArrayMatches("golf", "bravo", "foxtrot");

        mLiveArray.set(1, "lima");
        advance();
        Assert.assertEquals("golf-lima", getText());
        assertArrayMatches("golf", "lima", "foxtrot");

        mLiveArray.setRange(0, Arrays.asList("charlie", "delta"));
        advance();
        Assert.assertEquals("charlie-delta", getText());
        assertArrayMatches("charlie", "delta", "foxtrot");

        mLiveArray.clear();  // -
        advance();
        Assert.assertEquals("-", getText());
        Assert.assertTrue(mLiveArray.isEmpty());
        assertArrayMatches();

        mLiveArray.add("echo");  // echo
        mLiveArray.addAll(Arrays.asList("mike", "november"));
        advance();
        Assert.assertEquals("echo-mike", getText());
        assertArrayMatches("echo", "mike", "november");
    }

    @Test
    public void test_LiveDataException() {
        mLiveArray.addAll(Arrays.asList("alpha", "bravo", "foxtrot"));

        try {
            mLiveArray.add(10, "golf");  // Array out of bounds
            fail();
        } catch (IndexOutOfBoundsException ignore) {
        }

        try {
            mLiveArray.remove(10);
            fail();
        } catch (IndexOutOfBoundsException ignore) {
        }

        try {
            mLiveArray.set(10, "golf");
            fail();
        } catch (IndexOutOfBoundsException ignore) {
        }

        try {
            mLiveArray.removeRange(10, 1);
            fail();
        } catch (IndexOutOfBoundsException ignore) {
        }

        try {
            mLiveArray.removeRange(-1, 1);
            fail();
        } catch (IndexOutOfBoundsException ignore) {
        }

        try {
            mLiveArray.removeRange(0, 10);
            fail();
        } catch (IndexOutOfBoundsException ignore) {
        }

        try {
            mLiveArray.removeRange(0, -1);
            fail();
        } catch (IndexOutOfBoundsException ignore) {
        }

        try {
            mLiveArray.setRange(2, Arrays.asList("tango", "golf"));  // Too many elements
            fail();
        } catch (IndexOutOfBoundsException ignore) {
        }

        try {
            mLiveArray.setRange(-1, Arrays.asList("tango", "golf"));  // Too many elements
            fail();
        } catch (IndexOutOfBoundsException ignore) {
        }

        assertArrayMatches("alpha", "bravo", "foxtrot");
    }

    /**
     * The LiveArray modification functions take an Object argument, which internally
     * is converted into the correct C++ type.  These tests verify that a boxed boolean,
     * integer, and double are properly handled.
     */
    @Test
    public void test_LiveDataPrimitiveTypes() {
        mLiveArray.addAll(Arrays.asList("alpha", "bravo", "foxtrot"));
        loadDocument(DOC);

        Component c = mRootContext.getTopComponent();
        assertNotNull(c);

        mText = (Text) c;
        Assert.assertEquals("alpha-bravo", getText());

        mLiveArray.clear();

        mLiveArray.add(true);
        mLiveArray.add(23);
        advance();
        Assert.assertEquals("true-23", getText());
        assertArrayMatches(true, 23);

        mLiveArray.add(1, 100.25);
        advance();
        Assert.assertEquals("true-100.25", getText());
        assertArrayMatches(true, 100.25, 23);
    }

    @Test
    public void test_LiveDataRemove() {
        mLiveArray = LiveArray.create(Arrays.asList(
                "alpha", "bravo", "bravo", "foxtrot", "golf", "hotel", "hotel", "kilo"));
        loadDocument(DOC);

        Component c = mRootContext.getTopComponent();
        assertNotNull(c);

        mText = (Text) c;
        Assert.assertEquals("alpha-bravo", getText());

        mLiveArray.remove("bravo");  // This will remove the first "bravo"
        assertArrayMatches("alpha", "bravo", "foxtrot", "golf", "hotel", "hotel", "kilo");
        advance();
        Assert.assertEquals("alpha-bravo", getText());

        mLiveArray.remove("bravo");  // This will remove the next "bravo"
        assertArrayMatches("alpha", "foxtrot", "golf", "hotel", "hotel", "kilo");
        advance();
        Assert.assertEquals("alpha-foxtrot", getText());

        Assert.assertFalse(mLiveArray.remove("bravo"));  // No more bravos

        Assert.assertTrue(mLiveArray.removeAll(Arrays.asList("alpha", "hotel", "golf")));
        assertArrayMatches("foxtrot", "kilo");
        advance();
        Assert.assertEquals("foxtrot-kilo", getText());
    }

    private static final String DOC_MAP = "{\n" +
            "    \"type\": \"APL\",\n" +
            "    \"version\": \"1.0\",\n" +
            "    \"mainTemplate\": {\n" +
            "        \"item\": {\n" +
            "            \"type\": \"Text\",\n" +
            "            \"id\": \"MyText\",\n" +
            "            \"width\": 100,\n" +
            "            \"height\": 100,\n" +
            "            \"text\": \"${Signs[0].name}-${Signs[1].name}\"\n" +
            "        }\n" +
            "    }\n" +
            "}";

    /**
     * Pass Map objects to the LiveArray rather than primitive types and strings.  The
     * properties inside the Map will be exposed in the data-binding context.
     */
    @Test
    public void test_LiveDataMapType() {
        Map<String, Object> map1 = new HashMap<>();
        Map<String, Object> map2 = new HashMap<>();

        map1.put("name", "Fred");
        map2.put("name", "Pat");

        mLiveArray.addAll(Arrays.asList(map1, map2));
        loadDocument(DOC_MAP);

        Component c = mRootContext.getTopComponent();
        assertNotNull(c);

        mText = (Text) c;
        Assert.assertEquals("Fred-Pat", getText());

        map2.put("name", "Amit");
        mLiveArray.set(1, map2);
        advance();
        Assert.assertEquals("Fred-Amit", getText());

        Assert.assertEquals(mLiveArray.size(), mLiveArray.innerSize());
    }


    private static final String DOC_ARRAY = "{\n" +
            "    \"type\": \"APL\",\n" +
            "    \"version\": \"1.0\",\n" +
            "    \"mainTemplate\": {\n" +
            "        \"item\": {\n" +
            "            \"type\": \"Text\",\n" +
            "            \"id\": \"MyText\",\n" +
            "            \"width\": 100,\n" +
            "            \"height\": 100,\n" +
            "            \"text\": \"${Signs[0][0]}-${Signs[1][1]}\"\n" +
            "        }\n" +
            "    }\n" +
            "}";

    /**
     * Pass a Java Object array to the LiveArray rather than primitive types and strings.
     * The LiveArray is itself an array, so we're creating an array of arrays.
     */
    @Test
    public void test_LiveDataArrayType() {
        Object[] array1 = new Object[]{"alpha", "bravo"};
        Object[] array2 = new Object[]{"charlie", "delta"};

        mLiveArray.addAll(Arrays.asList(array1, array2));
        loadDocument(DOC_ARRAY);

        Component c = mRootContext.getTopComponent();
        assertNotNull(c);

        mText = (Text) c;
        Assert.assertEquals("alpha-delta", getText());

        array2[1] = "echo";
        mLiveArray.set(1, array2);
        advance();
        Assert.assertEquals("alpha-echo", getText());

        assertArrayMatches(new String[]{"alpha", "bravo"}, new String[]{"charlie", "echo"});
        Assert.assertEquals(mLiveArray.size(), mLiveArray.innerSize());
    }

    /**
     * Pass Java List-class objects into the LiveArray. These are objects that subclass
     * from Java List and behave like arrays.
     */
    @Test
    public void test_LiveDataListType() {
        ArrayList<String> array1 = new ArrayList<>();
        ArrayList<String> array2 = new ArrayList<>();

        array1.add("alpha");
        array1.add("bravo");
        array2.add("charlie");
        array2.add("delta");

        mLiveArray.addAll(Arrays.asList(array1, array2));
        assertArrayMatches(new String[]{"alpha", "bravo"}, new String[]{"charlie", "delta"});

        loadDocument(DOC_ARRAY);

        Component c = mRootContext.getTopComponent();
        assertNotNull(c);

        mText = (Text) c;
        Assert.assertEquals("alpha-delta", getText());

        array2.set(1, "echo");
        mLiveArray.set(1, array2);
        advance();
        Assert.assertEquals("alpha-echo", getText());

        assertArrayMatches(new String[]{"alpha", "bravo"}, new String[]{"charlie", "echo"});
    }

    /**
     * Pass various primitive Java arrays into the LiveArray.  The only primitive Java
     * array type we don't handle is the bytearray.
     */
    @Test
    public void test_LiveDataIntArrayType() {
        int[] array1 = { 10, 20 };
        int[] array2 = { 30, 40 };

        mLiveArray.addAll(Arrays.asList(array1, array2));
        assertArrayMatches(new Object[]{10,20}, new Object[]{30, 40});

        loadDocument(DOC_ARRAY);

        Component c = mRootContext.getTopComponent();
        assertNotNull(c);

        mText = (Text) c;
        Assert.assertEquals("10-40", getText());

        array2[1] = 50;
        mLiveArray.set(1, array2);
        assertArrayMatches(new Object[]{10,20}, new Object[]{30, 50});
        advance();
        Assert.assertEquals("10-50", getText());

        float[] floatArray = { 2.5f, 3.5f };
        mLiveArray.set(0, floatArray);
        assertArrayMatches(new Object[]{2.5, 3.5}, new Object[]{30, 50});
        advance();
        Assert.assertEquals("2.5-50", getText());

        double[] doubleArray = { 10.25, 12 };
        mLiveArray.set(0, doubleArray);
        assertArrayMatches(new Object[]{10.25, 12}, new Object[]{30, 50});
        advance();
        Assert.assertEquals("10.25-50", getText());

        short[] shortArray = { 99, 98 };
        mLiveArray.set(1, shortArray);
        assertArrayMatches(new Object[]{10.25, 12}, new Object[]{99, 98});
        advance();
        Assert.assertEquals("10.25-98", getText());

        long[] longArray = { 1000000, 123 };
        mLiveArray.set(0, longArray);
        assertArrayMatches(new Object[]{1000000, 123}, new Object[]{99, 98});
        advance();
        Assert.assertEquals("1000000-98", getText());

        boolean[] booleanArray = { true, false };
        mLiveArray.setRange(0, Arrays.asList(booleanArray, booleanArray));
        assertArrayMatches(new Object[]{true, false}, new Object[]{true, false});
        advance();
        Assert.assertEquals("true-false", getText());
    }

    /**
     * Pass a string containing a JSON expression into the LiveArray.  The APLJSONData object
     * serves as a wrapper for the string so that it will be treated as JSON data (if we didn't
     * wrap the string, the LiveArray would treat it as a simple string and not parse the
     * JSON).
     */
    @Test
    public void test_LiveDataJSONType() throws IOException {
        mLiveArray.add(APLJSONData.create("[1001,2,3]"));
        mLiveArray.add(APLJSONData.create("[10,20,30]"));

        loadDocument(DOC_ARRAY);
        Component c = mRootContext.getTopComponent();
        assertNotNull(c);

        mText = (Text) c;
        Assert.assertEquals("1001-20", getText());

        mLiveArray.set(0, APLJSONData.create("[99,101]"));
        advance();
        Assert.assertEquals("99-20", getText());

        Assert.assertEquals(mLiveArray.size(), mLiveArray.innerSize());
    }

    @Test
    public void test_LiveDataSublist() {
        mLiveArray = LiveArray.create(Arrays.asList(
                "alpha", "bravo", "bravo", "foxtrot",
                "golf", "hotel", "hotel", "kilo"));

        loadDocument(DOC);

        Component c = mRootContext.getTopComponent();
        assertNotNull(c);

        mText = (Text) c;
        Assert.assertEquals("alpha-bravo", getText());

        List<Object> sublist = mLiveArray.subList(1, 5);
        Assert.assertEquals("bravo", sublist.get(0));
        Assert.assertEquals(4, sublist.size());

        try {
            sublist.set(0, "zulu");
            fail();
        } catch (UnsupportedOperationException ignore) {
        }

        try {
            sublist.add("zulu");
            fail();
        } catch (UnsupportedOperationException ignore) {
        }

        try {
            sublist.remove(0);
            fail();
        } catch (UnsupportedOperationException ignore) {
        }

        try {
            sublist.clear();
            fail();
        } catch (UnsupportedOperationException ignore) {
        }

        try {
            sublist.get(-1);
            fail();
        } catch (IndexOutOfBoundsException ignore) {
        }

        try {
            sublist.get(4);
            fail();
        } catch (IndexOutOfBoundsException ignore) {
        }

        advance();
        Assert.assertEquals("alpha-bravo", getText());

        try {
            sublist = mLiveArray.subList(-1, 4);
            fail();
        } catch (IndexOutOfBoundsException ignore) {
        }

        try {
            sublist = mLiveArray.subList(0, 10);
            fail();
        } catch (IndexOutOfBoundsException ignore) {
        }
    }

    @Test
    public void test_LiveDataIterator() {
        mLiveArray = LiveArray.create(Arrays.asList(
                "alpha", "bravo", "bravo", "foxtrot",
                "golf", "hotel", "hotel", "kilo"));

        loadDocument(DOC);

        Component c = mRootContext.getTopComponent();
        assertNotNull(c);

        mText = (Text) c;
        Assert.assertEquals("alpha-bravo", getText());

        ListIterator<Object> iterator = mLiveArray.listIterator(6);
        Assert.assertTrue(iterator.hasNext());
        Assert.assertTrue(iterator.hasPrevious());
        Assert.assertEquals(6, iterator.nextIndex());
        Assert.assertEquals(5, iterator.previousIndex());

        Assert.assertEquals("hotel", iterator.next());
        Assert.assertEquals("kilo", iterator.next());
        Assert.assertFalse(iterator.hasNext());
        Assert.assertTrue(iterator.hasPrevious());
        Assert.assertEquals("kilo", iterator.previous());
        Assert.assertEquals("hotel", iterator.previous());

        try {
            iterator.add("zulu");
            fail();
        } catch (UnsupportedOperationException ignore) {
        }

        try {
            iterator.remove();
            fail();
        } catch (UnsupportedOperationException ignore) {
        }

        try {
            iterator.set("zulu");
            fail();
        } catch (UnsupportedOperationException ignore) {
        }

        advance();
        Assert.assertEquals("alpha-bravo", getText());

        try {
            iterator = mLiveArray.listIterator(-1);
            fail();
        } catch (IndexOutOfBoundsException ignore) {
        }

        try {
            iterator = mLiveArray.listIterator(9);
            fail();
        } catch (IndexOutOfBoundsException ignore) {
        }
    }

    private static final String DOC_WITH_SEQUENCE = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"1.2\",\n" +
            "  \"mainTemplate\": {\n" +
            "    \"items\": [\n" +
            "      {\n" +
            "        \"type\": \"Sequence\",\n" +
            "        \"id\": \"MySequence\",\n" +
            "        \"width\": \"100%\",\n" +
            "        \"height\": \"100%\",\n" +
            "        \"data\": \"${Signs}\",\n" +
            "        \"items\": [\n" +
            "          {\n" +
            "            \"type\": \"Text\",\n" +
            "            \"id\": \"MyText\",\n" +
            "            \"textAlign\": \"center\",\n" +
            "            \"textAlignVertical\": \"center\",\n" +
            "            \"text\": \"${data}\",\n" +
            "            \"width\": \"100%\",\n" +
            "            \"height\": \"20%\"\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";

    /**
     * Test LiveArray with Sequence, verify that adding an item in the the LiveArray
     * adds a child component in the Sequence.
     */
    @Test
    public void test_LiveDataWithSequence() {

        mLiveArray.addAll(Arrays.asList("alpha", "bravo"));
        assertArrayMatches("alpha", "bravo");

        loadDocument(DOC_WITH_SEQUENCE);

        final Component c = mRootContext.getTopComponent();
        assertNotNull(c);
        Assert.assertEquals(ComponentType.kComponentTypeSequence, c.getComponentType());
        Assert.assertEquals(2, c.getChildCount());

        // Set up the mock presenter to pass dirty properties over to the Sequence.
        doAnswer(invocation -> {
            Component component = invocation.getArgument(0);
            ComponentViewAdapter viewAdapter = ComponentViewAdapterFactory.getAdapter(component);
            return viewAdapter.createView(mContext, mAPLPresenter);
        }).when(mAPLPresenter).inflateComponentHierarchy(any());
        final View view = ComponentViewAdapterFactory.getAdapter(c).createView(mContext, mAPLPresenter);
        ComponentViewAdapter viewAdapter = ComponentViewAdapterFactory.getAdapter(c);
        viewAdapter.applyAllProperties(c, view);
        doAnswer(invocation -> {
            List<PropertyKey> dirtyProperties = invocation.getArgument(1);
            viewAdapter.refreshProperties(c, view, dirtyProperties);
            return null;
        }).when(mAPLPresenter).onComponentChange(eq(c), any(List.class));

        mLiveArray.add("foxtrot");
        advance();
        assertArrayMatches("alpha", "bravo", "foxtrot");
        Assert.assertEquals(3, c.getChildCount());
        assertEquals("alpha", ((Text) c.getChildAt(0)).getText());
        assertEquals("bravo", ((Text) c.getChildAt(1)).getText());
        assertEquals("foxtrot", ((Text) c.getChildAt(2)).getText());
    }

}
