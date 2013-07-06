package com.mon4h.framework.hbase.client;

import com.mon4h.framework.hbase.client.constant.ConfConstant;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTableInterfaceFactory;
import org.apache.hadoop.hbase.util.PoolMap;

import java.io.IOException;

/**
 * @author: xingchaowang
 * @date: 4/26/13 1:03 PM
 */
public class HTableFactory implements HTableInterfaceFactory {
    static volatile int cid = 0;

    @Override
    public HTableInterface createHTableInterface(Configuration config,
                                                 byte[] tableName) {
        try {
//            config.setInt(HConstants.HBASE_CLIENT_INSTANCE_ID, (cid++));
            config.setInt(HConstants.HBASE_CLIENT_IPC_POOL_SIZE, config.getInt(HConstants.HBASE_CLIENT_IPC_POOL_SIZE, 10));
            config.setInt(ConfConstant.CONF_TABLE_WRITER_BUFFER_SIZE, config.getInt(ConfConstant.CONF_TABLE_WRITER_BUFFER_SIZE, ConfConstant.DEFAULT_TABLE_WRITER_BUFFER_SIZE));

            HTable hTable = new CtripHTable(config, tableName);
            hTable.setAutoFlush(config.getBoolean(ConfConstant.CONF_TABLE_AUTO_FLUSH, false));

            return hTable;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public void releaseHTableInterface(HTableInterface table) throws IOException {
        if (table != null) {
            table.close();
        }
    }
}
