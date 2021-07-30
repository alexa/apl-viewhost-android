/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.document;

import android.graphics.Rect;

import com.amazon.apl.android.Component;
import com.amazon.apl.android.LiveMap;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.Text;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.amazon.apl.enums.PropertyKey.kPropertyText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LiveMapTest extends LiveObjectTest {
    private LiveMap mLiveMap;
    private Text mText;

    @Override
    public long createBoundObjectHandle() {
        LiveMap liveMap = LiveMap.create();
        return liveMap.getNativeHandle();
    }

    @Override
    public void registerLiveData(RootConfig rootConfig) {
        rootConfig.liveData("Signs", mLiveMap);
    }

    @Before
    public void setUp() {
        mLiveMap = LiveMap.create();
    }

    private void assertMapMatches(Map<String,Object> map) {
        assertEquals(map.size(), mLiveMap.size());
        assertEquals(mLiveMap.size(), mLiveMap.innerSize());
        assertEquals(mLiveMap.isEmpty(), mLiveMap.innerEmpty());

        for (Map.Entry<String,Object> entry: map.entrySet()) {
            String key = entry.getKey();
            Object expectedValue = entry.getValue();
            assertObjectTriple(expectedValue, mLiveMap.get(key), mLiveMap.innerGet(key));
            assertEquals(mLiveMap.containsKey(key), mLiveMap.innerHas(key));
        }
        assertEquals(map.hashCode(), mLiveMap.hashCode());
        assertEquals(map.keySet(), mLiveMap.keySet());
        assertEquals(map.values().size(), mLiveMap.values().size());
        assertTrue(map.values().containsAll(mLiveMap.values()));
        assertTrue(mLiveMap.values().containsAll(map.values()));
    }

    private String getText() {
        return mText.getText(mText.mProperties.getStyledText(kPropertyText)).toString();
    }

    /**
     * Constructor checks
     */
    @Test
    public void test_LiveMapConstructors() {
        mLiveMap = LiveMap.create();
        assertMapMatches(Collections.emptyMap());

        Map<String,Object> map = new HashMap<>();
        map.put("alpha", "a");
        map.put("bravo", 1);
        map.put("charlie", true);
        mLiveMap = LiveMap.create(map);
        assertMapMatches(map);
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
            "            \"text\": \"${Signs.alpha}-${Signs.bravo}-${Signs.charlie}-${Signs.delta.a}-${Signs.epsilon[0]}\"\n" +
            "        }\n" +
            "    }\n" +
            "}";

    /**
     * Test all of the modification functions on LiveMap to verify that changing
     * the LiveMap will propagate to a data-binding expression in the document.
     */
    @Test
    public void test_LiveData() {
        Map<String,Object> map = new HashMap<>();
        map.put("alpha", "a");
        map.put("bravo", 1);
        map.put("charlie", true);
        Map<String,Object> innerMap = new HashMap<>();
        map.put("delta", innerMap);
        innerMap.put("a", 0.5);
        List<Object> innerList = new ArrayList<>();
        innerList.add("FOO");
        map.put("epsilon", innerList);

        mLiveMap.putAll(map);
        assertMapMatches(map);

        loadDocument(DOC);

        Component c = mRootContext.getTopComponent();
        assertNotNull(c);
        mText = (Text) c;
        Assert.assertEquals("a-1-true-0.5-FOO", getText());

        mLiveMap.remove("charlie");
        map.remove("charlie");
        advance();
        Assert.assertEquals("a-1--0.5-FOO", getText());
        assertMapMatches(map);

        mLiveMap.put("charlie", "zed");
        mLiveMap.put("alpha", 3.14);
        map.put("charlie", "zed");
        map.put("alpha", 3.14);
        advance();
        assertEquals("3.14-1-zed-0.5-FOO", getText());
        assertMapMatches(map);

        // inner object changes don't propagate without express call to LiveMap.put
        innerMap.put("a", 0.7);
        innerList.set(0, "bar");
        advance();
        assertEquals("3.14-1-zed-0.5-FOO", getText());

        mLiveMap.put("delta", innerMap);
        mLiveMap.put("epsilon", innerList);
        advance();
        assertEquals("3.14-1-zed-0.7-bar", getText());
        assertMapMatches(map);

        mLiveMap.clear();
        map.clear();
        advance();
        assertEquals("----", getText());
        assertMapMatches(map);

        mLiveMap.put("bravo", "B");
        mLiveMap.put("charlie", false);
        map.put("bravo", "B");
        map.put("charlie", false);
        advance();
        assertEquals("-B-false--", getText());
    }

    @Test
    public void test_keySetOperations() {
        Map<String,Object> map = new HashMap<>();
        map.put("alpha", "a");
        map.put("bravo", 1);
        map.put("charlie", true);

        mLiveMap.putAll(map);
        loadDocument(DOC);

        Component c = mRootContext.getTopComponent();
        assertNotNull(c);
        mText = (Text) c;
        assertEquals("a-1-true--", getText());

        Set<String> keySet = mLiveMap.keySet();
        keySet.remove("alpha");
        map.remove("alpha");
        advance();
        assertEquals("-1-true--", getText());
        assertMapMatches(map);

        Iterator<String> keyItr = keySet.iterator();
        while (keyItr.hasNext()) {
            if (keyItr.next().equals("charlie")) {
                keyItr.remove();
            }
        }
        map.remove("charlie");
        advance();
        assertEquals("-1---", getText());
        assertMapMatches(map);

        keySet.clear();
        advance();
        map.clear();
        assertEquals("----", getText());
        assertMapMatches(map);

        try {
            keySet.add("delta");
            fail();
        } catch (UnsupportedOperationException expected) {

        }

        try {
            keySet.addAll(Arrays.asList("foo", "bar"));
            fail();
        } catch (UnsupportedOperationException expected) {

        }
    }

    @Test
    public void test_valueOperations() {
        Map<String,Object> map = new HashMap<>();
        map.put("alpha", "a");
        map.put("bravo", 1);
        map.put("charlie", true);
        mLiveMap.putAll(map);
        loadDocument(DOC);

        Component c = mRootContext.getTopComponent();
        assertNotNull(c);
        mText = (Text) c;
        assertEquals("a-1-true--", getText());

        Collection<Object> values = mLiveMap.values();
        values.remove("a");
        map.values().remove("a");
        advance();
        assertEquals("-1-true--", getText());
        assertMapMatches(map);

        Iterator<Object> valueItr = values.iterator();
        while (valueItr.hasNext()) {
            if (valueItr.next().equals(true)) {
                valueItr.remove();
            }
        }
        map.values().remove(true);
        advance();
        assertEquals("-1---", getText());

        values.clear();
        map.clear();
        advance();
        assertEquals("----", getText());
        assertMapMatches(map);

        try {
            values.add("delta");
            fail();
        } catch (UnsupportedOperationException expected) {

        }

        try {
            values.addAll(Arrays.asList("foo", "bar"));
            fail();
        } catch (UnsupportedOperationException expected) {

        }
    }

    @Test
    public void test_entrySetOperations() {
        Map<String,Object> map = new HashMap<>();
        map.put("alpha", "a");
        map.put("bravo", 1);
        map.put("charlie", true);
        mLiveMap.putAll(map);
        loadDocument(DOC);

        Component c = mRootContext.getTopComponent();
        assertNotNull(c);
        mText = (Text) c;
        assertEquals("a-1-true--", getText());

        Set<Map.Entry<String,Object>> entrySet = mLiveMap.entrySet();
        // remove alpha
        Map.Entry<String,Object> toRemove = null;
        for (Map.Entry<String,Object> entry : entrySet) {
            if (entry.getKey().equals("alpha")) {
                toRemove = entry;
            }
        }
        entrySet.remove(toRemove);
        map.remove("alpha");
        advance();
        assertEquals("-1-true--", getText());
        assertMapMatches(map);

        //remove charlie
        Iterator<Map.Entry<String,Object>> entryItr = entrySet.iterator();
        while (entryItr.hasNext()) {
            if (entryItr.next().getKey().equals("charlie")) {
                entryItr.remove();
            }
        }
        map.remove("charlie");
        advance();
        assertEquals("-1---", getText());
        assertMapMatches(map);

        //update bravo
        entryItr = entrySet.iterator();
        while (entryItr.hasNext()) {
            Map.Entry<String,Object> entry = entryItr.next();
            if (entry.getKey().equals("bravo")) {
                entry.setValue("foo");
            }
        }
        map.put("bravo", "foo");
        advance();
        assertEquals("-foo---", getText());
        assertMapMatches(map);

        entrySet.clear();
        map.clear();
        advance();
        assertEquals("----", getText());
        assertMapMatches(map);
    }

    @Test
    public void test_putUnsupportedObjectIsNull() {
        mLiveMap.put("alpha", new Rect(1, 2, 3, 4));
        loadDocument(DOC);

        Component c = mRootContext.getTopComponent();
        assertNotNull(c);
        mText = (Text) c;
        assertEquals("----", getText());
    }

}
