package com.amazon.common.test;

import junit.framework.TestCase;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

import com.amazon.common.NativeBinding;

public class Asserts {

    public static void assertNativeHandle(long handle) {
        // The object has a native handle
        assertTrue("Expected object to have native handle", handle != 0);
        // The object is registered with the PhantomReference queue
        TestCase.assertTrue("Expected bound object to be registered", NativeBinding.testBound(handle));

        // memory free
        performGC();

        NativeBinding.doDeletes();

        assertFalse("expected unbound object", NativeBinding.testBound(handle));
    }

    private static void performGC() {
        CountDownLatch latch = new CountDownLatch(1);
        ReferenceQueue<Object> refQueue = new ReferenceQueue<>();

        PhantomReference<Object> phantomRef =
                new PhantomReference<>(
                        new Object() {
                            @Override
                            protected void finalize() {
                                latch.countDown();
                            }
                        },
                        refQueue);

        try {
            boolean finalized = false;

            // Wait up to 10 seconds for finalization.
            for (int i = 0; i< 10; i++) {
                System.runFinalization();
                System.gc();

                finalized = latch.await(1, TimeUnit.SECONDS);
                if (finalized) {
                    break;
                }
            }

            if (!finalized) {
                throw new RuntimeException("GC timed out.");
            }

            // Wait up to a second for the reference to be released.
            for (int i = 0; i < 10; i++) {
                // Once the queue returns the ref, GC has been confirmed.
                if (refQueue.poll() != null) {
                    return;
                } else {
                    Thread.sleep(100);
                }
            }
        } catch (InterruptedException ex) {
            throw new RuntimeException("GC interrupted");
        }
    }
}
