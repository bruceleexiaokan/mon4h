package mon4h.framework.dashboard.persist.store.util;

import mon4h.framework.dashboard.persist.AbstractHBaseTest;
import mon4h.framework.dashboard.persist.store.util.HBaseAdminUtil;

import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.junit.Before;
import org.junit.Test;


/**
 * User: huang_jie
 * Date: 7/3/13
 * Time: 1:19 PM
 */
public class HBaseAdminUtilTest extends AbstractHBaseTest {
    private HTableInterface table;
    private HTableInterface tableMax;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        if (admin.tableExists("unit_test_ttl") || admin.isTableEnabled("unit_test_ttl")) {
            if (admin.isTableEnabled("unit_test_ttl")) {
                admin.disableTable("unit_test_ttl");
            }
            admin.deleteTable("unit_test_ttl");
        }
        HTableDescriptor tableDesc = new HTableDescriptor("unit_test_ttl");
        HColumnDescriptor family = new HColumnDescriptor("m");
        family.setTimeToLive(604800);
        tableDesc.addFamily(family);
        admin.createTable(tableDesc);

        if (admin.tableExists("unit_test_ttl_max") || admin.isTableEnabled("unit_test_ttl_max")) {
            if (admin.isTableEnabled("unit_test_ttl_max")) {
                admin.disableTable("unit_test_ttl_max");
            }
            admin.deleteTable("unit_test_ttl_max");
        }
        HTableDescriptor tableDescMax = new HTableDescriptor("unit_test_ttl_max");
        HColumnDescriptor familyMax = new HColumnDescriptor("m");
        tableDescMax.addFamily(familyMax);
        admin.createTable(tableDescMax);

        if (admin.tableExists("unit_test_data") || admin.isTableEnabled("unit_test_data")) {
            if (admin.isTableEnabled("unit_test_data")) {
                admin.disableTable("unit_test_data");
            }
            admin.deleteTable("unit_test_data");
        }
        HTableDescriptor dataTable = new HTableDescriptor("unit_test_data");
        HColumnDescriptor family1 = new HColumnDescriptor("m");
        family1.setTimeToLive(604800);
        dataTable.addFamily(family1);
        admin.createTable(dataTable);

        table = tablePool.getTable("unit_test_ttl");
        tableMax = tablePool.getTable("unit_test_ttl_max");
    }

    @Test
    public void testGetDayOfTTL() throws Exception {
        int dayOfTTL = HBaseAdminUtil.getDayOfTTL(table);
        assert dayOfTTL == 7;
    }


    @Test
    public void testGetDayOfTTL_max() throws Exception {
        int dayOfTTL = HBaseAdminUtil.getDayOfTTL(tableMax);
        assert dayOfTTL == 24855;
    }

    @Test
    public void testGetDayOfTTL_namespace() throws Exception {
        int dayOfTTL = HBaseAdminUtil.getDayOfTTL("__test__data");
        assert dayOfTTL == 7;
    }

}
