/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.viewhost.primitives;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.viewhost.primitives.decoder.KeyedContainerDecoder;
import com.amazon.apl.viewhost.primitives.decoder.SingleValueDecoder;
import com.amazon.apl.viewhost.primitives.decoder.UnkeyedContainerDecoder;
import com.amazon.apl.viewhost.primitives.transcoder.Transcoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class CodableTest extends ViewhostRobolectricTest {
    private static final String DECODABLE_JSON = "{" +
        "  \"id\": \"foo\"," +
        "  \"version\": 2," +
        "  \"flags\": {" +
        "    \"interactionType\": \"standard\"," +
        "    \"nestedArrays\": [" +
        "      [ \"a\" ]," +
        "      [ \"b\", \"c\" ]," +
        "      [ true, false ]" +
        "    ]," +
        "    \"nestedObjects\": [" +
        "      { \"a\": null }," +
        "      { \"b\": 2 }," +
        "      { \"c\": true }," +
        "      { \"d\": 3.3 }" +
        "    ]," +
        "    \"items\": [" +
        "      1," +
        "      2," +
        "      3," +
        "      4" +
        "    ]" +
        "  }" +
        "}";

    private Decodable mPayload;

    @Mock
    private Transcoder mMockTranscoder;

    @Before
    public void setup() throws JSONException {
        MockitoAnnotations.openMocks(this);
        mPayload = new JsonDecodable(new JSONObject(DECODABLE_JSON));
    }

    @Test
    public void testObjectPayloadOnlyDecodableAsObject() {
        // Not supported
        assertNull(mPayload.decodeSingleValue());
        assertNull(mPayload.decodeUnkeyedContainer());
        assertFalse(mPayload.transcode(mMockTranscoder));

        // Supported
        assertNotNull(mPayload.decodeKeyedContainer());
    }

    @Test
    public void testArrayPayloadDecodableAsArray() throws JSONException {
        Decodable arrayPayload = new JsonDecodable(new JSONArray("[1, 2, 3]"));

        // Not supported
        assertNull(arrayPayload.decodeSingleValue());
        assertNull(arrayPayload.decodeKeyedContainer());
        assertFalse(arrayPayload.transcode(mMockTranscoder));

        // Supported
        UnkeyedContainerDecoder decoder = arrayPayload.decodeUnkeyedContainer();
        assertNotNull(decoder);
        assertEquals(3, decoder.size());
        assertEquals(1, decoder.decodeSingleValue().decodeInteger().intValue());
        assertEquals(2, decoder.decodeSingleValue().decodeInteger().intValue());
        assertEquals(3, decoder.decodeSingleValue().decodeInteger().intValue());
    }

    @Test
    public void testBasicKeyedContainerDecoder() {
        KeyedContainerDecoder decoder = mPayload.decodeKeyedContainer();

        // Keys are inspectable
        assertEquals(3, decoder.size());
        assertTrue(decoder.hasKey("id"));
        assertTrue(decoder.hasKey("version"));
        assertTrue(decoder.hasKey("flags"));
        assertFalse(decoder.hasKey("missing"));

        // Keys are iterable
        Set<String> actual = new HashSet<>();
        Iterator<String> iterator = decoder.keys();
        while (iterator.hasNext()) {
            actual.add(iterator.next());
        }
        Set<String> expected = new HashSet<>();
        expected.add("id");
        expected.add("version");
        expected.add("flags");
        assertEquals(expected, actual);

        // Can decode a particular value
        assertNull(decoder.decodeKeyedContainer("id"));
        assertNull(decoder.decodeUnkeyedContainer("id"));
        assertNotNull(decoder.decodeSingleValue("id"));
        assertEquals("foo", decoder.decodeSingleValue("id").decodeString());
    }

    @Test
    public void testBasicUnkeyedContainerDecoder() {
        UnkeyedContainerDecoder decoder = mPayload.decodeKeyedContainer()
            .decodeKeyedContainer("flags")
            .decodeUnkeyedContainer("items");

        // Initial state
        assertEquals(4, decoder.size());
        assertEquals(0, decoder.index());
        assertFalse(decoder.atEnd());

        // Cannot decode wrong type
        assertNull(decoder.decodeKeyedContainer());
        assertNull(decoder.decodeUnkeyedContainer());

        // Extract some single values
        ArrayList<Integer> items = new ArrayList<>();
        while (!decoder.atEnd()) {
            items.add(decoder.decodeSingleValue().decodeInteger());
        }
        assertArrayEquals(new Integer[]{1, 2, 3, 4}, items.toArray());

        // Final state
        assertEquals(4, decoder.size());
        assertEquals(4, decoder.index());
        assertTrue(decoder.atEnd());

        // Cannot decode anything
        assertNull(decoder.decodeKeyedContainer());
        assertNull(decoder.decodeUnkeyedContainer());
        assertNull(decoder.decodeSingleValue());
    }

    @Test
    public void testNumericUnkeyedContainerDecoder() {
        UnkeyedContainerDecoder decoder = mPayload.decodeKeyedContainer()
            .decodeKeyedContainer("flags")
            .decodeUnkeyedContainer("items");

        assertEquals(1, decoder.decodeSingleValue().decodeLong().longValue());
        assertEquals(2.0, decoder.decodeSingleValue().decodeFloat().floatValue(), 0.1);
        assertEquals(3.0, decoder.decodeSingleValue().decodeDouble().doubleValue(), 0.1);
    }

    @Test
    public void testNestedUnkeyedContainerDecoder() {
        UnkeyedContainerDecoder arraysDecoder = mPayload.decodeKeyedContainer()
            .decodeKeyedContainer("flags")
            .decodeUnkeyedContainer("nestedArrays");

        // Contains nested unkeyed containers
        assertEquals(3, arraysDecoder.size());
        assertNotNull(arraysDecoder.decodeUnkeyedContainer());
        assertNull(arraysDecoder.decodeKeyedContainer());

        // Not a single value, but can be decoded as a string
        SingleValueDecoder singleValueDecoder = arraysDecoder.decodeSingleValue();
        assertNotNull(singleValueDecoder);
        assertFalse(singleValueDecoder.decodeNull());
        assertNull(singleValueDecoder.decodeBoolean());
        assertNull(singleValueDecoder.decodeFloat());
        assertNull(singleValueDecoder.decodeDouble());
        assertNull(singleValueDecoder.decodeInteger());
        assertNull(singleValueDecoder.decodeLong());
        assertEquals("[\"b\",\"c\"]", singleValueDecoder.decodeString());
        
        // Decode booleans
        UnkeyedContainerDecoder boolsDecoder = arraysDecoder.decodeUnkeyedContainer();
        assertNotNull(boolsDecoder);
        assertEquals(2, boolsDecoder.size());
        assertTrue(boolsDecoder.decodeSingleValue().decodeBoolean().booleanValue());
        assertFalse(boolsDecoder.decodeSingleValue().decodeBoolean().booleanValue());

        UnkeyedContainerDecoder objectsDecoder = mPayload.decodeKeyedContainer()
            .decodeKeyedContainer("flags")
            .decodeUnkeyedContainer("nestedObjects");

        // Contains nested keyed containers
        assertEquals(4, objectsDecoder.size());
        assertNotNull(objectsDecoder.decodeKeyedContainer());
        assertNull(objectsDecoder.decodeUnkeyedContainer());

        // Not a single value, but can be decoded as a string
        singleValueDecoder = objectsDecoder.decodeSingleValue();
        assertNotNull(singleValueDecoder);
        assertNull(singleValueDecoder.decodeBoolean());
        assertNull(singleValueDecoder.decodeFloat());
        assertNull(singleValueDecoder.decodeDouble());
        assertNull(singleValueDecoder.decodeInteger());
        assertNull(singleValueDecoder.decodeLong());
        assertEquals("{\"b\":2}", singleValueDecoder.decodeString());
    }

    @Test
    public void testVariousSingleTypes() {
        UnkeyedContainerDecoder objectsDecoder = mPayload.decodeKeyedContainer()
                .decodeKeyedContainer("flags")
                .decodeUnkeyedContainer("nestedObjects");
        KeyedContainerDecoder decoder = objectsDecoder.decodeKeyedContainer();
        assertTrue(decoder.decodeSingleValue("a").decodeNull());

        decoder = objectsDecoder.decodeKeyedContainer();
        assertEquals(2, decoder.decodeSingleValue("b").decodeInteger().intValue());
        assertEquals(2, decoder.decodeSingleValue("b").decodeLong().longValue());
        assertNull(decoder.decodeSingleValue("b").decodeBoolean());

        decoder = objectsDecoder.decodeKeyedContainer();
        assertTrue(decoder.decodeSingleValue("c").decodeBoolean().booleanValue());
        assertNull(decoder.decodeSingleValue("c").decodeInteger());
        assertNull(decoder.decodeSingleValue("c").decodeLong());
        assertNull(decoder.decodeSingleValue("c").decodeFloat());
        assertNull(decoder.decodeSingleValue("c").decodeDouble());

        decoder = objectsDecoder.decodeKeyedContainer();
        assertEquals(3.3, decoder.decodeSingleValue("d").decodeFloat().floatValue(), 0.1);
        assertEquals(3.3, decoder.decodeSingleValue("d").decodeDouble().doubleValue(), 0.1);
    }
    
    @Test
    public void testStringDecodable() {
        JsonStringDecodable decodable = new JsonStringDecodable("payload");
        assertFalse(decodable.transcode(mMockTranscoder));
        assertNull(decodable.decodeSingleValue());
        assertNull(decodable.decodeKeyedContainer());
        assertNull(decodable.decodeUnkeyedContainer());
        assertEquals("payload", decodable.getString());
    }

    @Test
    public void testJsonObjectTranscoderSkeleton() throws JSONException {
        JSONObject payload = new JSONObject("{ \"hello\": 3 }");
        JsonDecodable decodable = new JsonDecodable(payload);
        JsonTranscoder transcoder = new JsonTranscoder();

        // This is a skeleton class, the transcoder methods don't do anything
        transcoder.transcodeNull();
        transcoder.transcode(true);
        transcoder.transcode((float)1.2);
        transcoder.transcode((double)99.9);
        transcoder.transcode((int)123);
        transcoder.transcode((long)456);
        transcoder.transcodeUnsigned((int)123);
        transcoder.transcodeUnsigned((long)456);
        transcoder.transcode("What is it");
        assertNull(transcoder.transcodeSingleValue());
        assertNull(transcoder.transcodeKeyedContainer());
        assertNull(transcoder.transcodeUnkeyedContainer());

        assertTrue(decodable.transcode(transcoder));
        assertEquals(payload, transcoder.getJsonObject());
    }
}
