package com.ctrip.framework.dashboard.aggregator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TIOStreamTransport;
import v3.MetricValueType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Unit test for simple App.
 */
public class ThriftCompatabilityTest
    extends TestCase
{
    private ByteArrayOutputStream os = new ByteArrayOutputStream();
    private TProtocol outProtocol = new TBinaryProtocol(new TIOStreamTransport(os));
    private TProtocol inProtocal = null;
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public ThriftCompatabilityTest(String testName)
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( ThriftCompatabilityTest.class );
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (os != null) os.close();
    }

    /**
     */
    public void testV2toV3()
    {
        v2.MetricEvent e2 = new v2.MetricEvent();
        e2.setName("name");
        e2.setCreatedTime(10000);
        e2.setTags(new HashSet<String>(Arrays.asList("1=1", "2=2")));
        e2.setValue("value");
        e2.setValueType(v2.MetricValueType.TYPE_FLOAT);
        try {
            e2.write(outProtocol);
        } catch (TException e1) {
            assertTrue(false);
        }

        inProtocal = new TBinaryProtocol(new TIOStreamTransport(new ByteArrayInputStream(os.toByteArray())));
        v3.MetricEvent e3 = new v3.MetricEvent();
        try {
            e3.read(inProtocal);
        } catch (TException e1) {
            assertTrue(false);
        }
        System.out.println();
        assertEquals(e3.getName(), "name");
        assertEquals(e3.getCreatedTime(), 10000);
        assertEquals(new HashSet<String>(Arrays.asList("1=1", "2=2")), e3.getTags());
        assertEquals(e3.getValueType(), v3.MetricValueType.TYPE_FLOAT);
    }

    /**
     */
    public void testV3toV2()
    {
        v3.MetricEvent e3 = new v3.MetricEvent();
        e3.setName("name");
        e3.setCreatedTime(10000);
        e3.setValue("value");
        e3.setValueType(MetricValueType.TYPE_RAW);
        Map<String, String> m = new HashMap<String, String>();
        m.put("1", "1");
        m.put("2", "2");
        e3.setTagsMap(m);
        e3.setValueList(Arrays.asList(1.0, 2.0));

        try {
            e3.write(outProtocol);
        } catch (TException e1) {
            assertTrue(false);
        }

        inProtocal = new TBinaryProtocol(new TIOStreamTransport(new ByteArrayInputStream(os.toByteArray())));
        v2.MetricEvent e2 = new v2.MetricEvent();
        try {
            e2.read(inProtocal);
        } catch (TException e1) {
            assertTrue(false);
        }
        System.out.println();
        assertEquals(e2.getName(), "name");
        assertEquals(e2.getCreatedTime(), 10000);
    }
}
