package org.apache.hadoop.hbase.master.ctripbalancer;

import org.apache.hadoop.hbase.HServerLoad;

/**
 * @author: qmhu
 * @date: 5/20/13 1:47 PM
 */
public class RegionLoadStatus {

    private HServerLoad.RegionLoad lastBalanceRegionLoad;

    private HServerLoad.RegionLoad recentRegionLoad;


    public HServerLoad.RegionLoad getLastBalanceRegionLoad() {
        return lastBalanceRegionLoad;
    }

    public void setLastBalanceRegionLoad(HServerLoad.RegionLoad lastBalanceRegionLoad) {
        this.lastBalanceRegionLoad = lastBalanceRegionLoad;
    }

    public HServerLoad.RegionLoad getRecentRegionLoad() {
        return recentRegionLoad;
    }

    public void setRecentRegionLoad(HServerLoad.RegionLoad recentRegionLoad) {
        this.recentRegionLoad = recentRegionLoad;
    }
}
