package com.ctrip.framework.dashboard.aggregator.ids;

import com.ctrip.framework.dashboard.aggregator.InvalidTagProcessingException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * User: wenlu
 * Date: 13-7-11
 */

public class RIDSTest {
    private RIDS rids;

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        rids = new RIDS(-1, -1);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception {
    }

    private void putAllString(List<String> strs) {
        for (String s : strs) {
            try {
                rids.getIDByString(s);
            } catch (InvalidTagProcessingException e) {
                assertTrue(false);
            }
        }
    }

    private void queryAllID(List<Integer> ids) {
        for (Integer id : ids) {
            rids.getStringByID(id);
        }
    }

    private List<String> prepareData(String prefix, int size) {
        if (prefix == null) {
            prefix = "";
        }

        List<String> result = new ArrayList<String>(size);
        for (int i = 0; i < size; i++) {
            result.add(prefix + i);
        }
        return result;
    }

    private void multiPut(int size, int round) {
        List<String> buffer = prepareData("", size);
        long before = System.currentTimeMillis();
        for (int i = 0; i < round; i++)
            putAllString(buffer);
        long after = System.currentTimeMillis();
        assertEquals(size, rids.getEstimatedSize());
        assertEquals(size, rids.getSize());
        String result = String.format("1 thread used %f seconds to put %d records for %d times",
                (after - before) / 1000.0, rids.getEstimatedSize(), round);
        System.out.println(result);
    }

    private void multiPutInParallel(int size, int parallel) {
        List<List<String>> buffers = new ArrayList<List<String>>(parallel);
        for (int i = 0; i < parallel; i++) {
            List<String> buffer = prepareData(i + "_", size);
            buffers.add(buffer);
        }
        Thread[] workers = new Thread[parallel];
        for (int i = 0; i < parallel; i++) {
            final List<String> job = buffers.get(i);
            workers[i] = new Thread() {
                @Override
                public void run() {
                    putAllString(job);
                }
            };
        }

        long before = System.currentTimeMillis();
        for (int i = 0; i < parallel; i++)
            workers[i].start();
        for (int i = 0; i < parallel; i++) {
            try {
                workers[i].join();
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }
        long after = System.currentTimeMillis();
        assertEquals(size * parallel, rids.getEstimatedSize());
        assertEquals(size * parallel, rids.getSize());
        String result = String.format("%d threads used %f seconds to put %d records",
                parallel, (after - before) / 1000.0, rids.getEstimatedSize());
        System.out.println(result);
    }

    @Test(enabled = false)
    public void testMultiPut() {
        System.out.println("-------1,000,000 records-------");
        reset();
        multiPut(1000000, 1);
        reset();
        multiPut(100000, 10);
        reset();
        multiPutInParallel(100000, 10);
        System.out.println("-------5,000, 000 records-------");
        reset();
        multiPut(100000, 50);
        reset();
        multiPutInParallel(100000, 50);
    }

    @Test(enabled = false)
    public void testSanity() {
        int size = 10000;
        List<String> buffer = prepareData("", size);
        List<Integer> result = new ArrayList<Integer>(size);
        for (String s : buffer) {
            try {
                result.add(rids.getIDByString(s));
            } catch (InvalidTagProcessingException e) {
                assertTrue(false);
            }
        }
        for (int i = 0; i < size; i++) {
            assertEquals("" + i, rids.getStringByID(result.get(i)));
        }
    }

    @Test(enabled = false)
    public void testGc() {
        System.out.println("current time is: " + System.currentTimeMillis());
        int size = 100000;
        rids = new RIDS(size, -1);
        List<String> buffer = prepareData("", size);
        // half has ttl: 2s, and the others has ttl: 200s
        int i = 0;
        for (String s : buffer) {
            long ttl = 0;
            if (i % 2 == 0) {
                ttl = 2000;
            } else {
                ttl = 200000;
            }

            try {
                rids.getIDByString(s, ttl);
            } catch (InvalidTagProcessingException e) {
                assertTrue(false);
            }
            i++;
        }

        assertEquals(rids.getSize(), size);
        assertEquals(rids.getEstimatedSize(), size);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
        }

        // trigger gc implicitly
        try {
            rids.getIDByString("a", 5000);
        } catch (InvalidTagProcessingException e) {
            assertTrue(false);
        }
        assertEquals(rids.getSize(), 1 + size / 2);
        assertEquals(rids.getEstimatedSize(), 1 + size / 2);
        assertTrue(rids.sanityCheck());

        // force gc to expire all the items
        rids.forceGc(System.currentTimeMillis() + 400000);
        assertEquals(rids.getSize(), 0);
        assertEquals(rids.getEstimatedSize(), 0);
        assertTrue(rids.sanityCheck());

        // ttl for 100s
        try {
            for (String s : buffer) {
                rids.getIDByString(s, 100000);
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
            // no gc
            rids.getIDByString("a");
            assertEquals(rids.getSize(), size + 1);
            assertEquals(rids.getEstimatedSize(), size + 1);

            // gc by capacity
            rids = new RIDS(1000, 10000);
            buffer = prepareData("", 400);
            for (String s : buffer) {
                rids.getIDByString(s, 1000);
            }
            assertEquals(rids.getSize(), 400);
            assertTrue(rids.getCapacity() < 10000);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }

            // capacity overflow
            rids.getIDByString(getLongString((int) rids.getCapacity() / 2), 5000);
            // trigger gc by capacity
            rids.getIDByString("a", 5000);
            assertEquals(rids.getSize(), 2);
        } catch (InvalidTagProcessingException e) {
            assertTrue(false);
        }
    }

    @Test(enabled = false)
    public void testSimpleCase() throws InvalidTagProcessingException {
        RIDS rids = new RIDS(1000, 1000);
        int id1 = rids.getIDByString("aa");
        int id2 = rids.getIDByString("bb");
        int id3 = rids.getIDByString("sdfsdf");
        assertEquals(rids.getStringByID(id1), "aa");
        assertEquals(rids.getStringByID(id2), "bb");
        assertEquals(rids.getStringByID(id3), "sdfsdf");
    }

    private void reset() {
        rids = new RIDS(-1, -1);
    }

    private static String getLongString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append("a");
        }
        return sb.toString();
    }
}
