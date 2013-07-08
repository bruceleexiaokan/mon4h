package org.apache.hadoop.hbase.master.newbalancer;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.HServerLoad.RegionLoad;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.master.RegionPlan;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.EnvironmentEdgeManager;

public class TableBasedStochasticLoadBalancer extends StochasticLoadBalancer {
	private static final Log LOG = LogFactory
			.getLog(TableBasedStochasticLoadBalancer.class);

    /**
     * StochasticLoadBalance cluster in table level
     * @param clusterState
     * @return
     */
	@Override
	public List<RegionPlan> balanceCluster(
			Map<ServerName, List<HRegionInfo>> clusterState) {

        LOG.info("updateRegionLoad before sleep");
        // update current region load
        updateRegionLoad();

        LOG.info("updateRecentRegionLoad");
        // update regionLoadStatusMap for recent region load
        updateRecentRegionLoad();

        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        LOG.info("updateRegionLoad for calculate cost");
        // update current region load
        updateRegionLoad();

        Map<String, BalancedClusterResult> resultMap = new HashMap<String, BalancedClusterResult>();
		
		long startTime = EnvironmentEdgeManager.currentTimeMillis();
        long logStartTime = EnvironmentEdgeManager.currentTimeMillis();

        LOG.info("split table Map state");
        // create table based clusterState
		Map<String, Map<ServerName, List<HRegionInfo>>> tableSplitMapState = split(clusterState);

        LOG.info("balance empty region ");
        // balance empty region
		List<RegionPlan> plans = balanceEmptyRegions(tableSplitMapState);

        // for each table level,we use StochasticLoadBalancer's createBalancedCluster to create balance plans
		for (Map.Entry<String, Map<ServerName, List<HRegionInfo>>> entry : tableSplitMapState
				.entrySet()) {

			if (EnvironmentEdgeManager.currentTimeMillis() - startTime > maxRunningTime) {
				break;
			}

            if (EnvironmentEdgeManager.currentTimeMillis() - logStartTime > logRunningTime) {
                LOG.info("still running computing cost ...");
                logStartTime = EnvironmentEdgeManager.currentTimeMillis();
            }

			String tableName = entry.getKey();
			Map<ServerName, List<HRegionInfo>> tabledClusterState = entry.getValue();

            LOG.info("begin balance table "+entry.getKey());
            // reset one balance time
			BalancedClusterResult result = createBalancedCluster(tabledClusterState, 0);
			resultMap.put(tableName, result);
		}

        LOG.info("updateLastRegionLoad");
        // update regionLoadStatusMap for last region load
        updateLastRegionLoad();

        LOG.info("begin merge table result");
		BalancedClusterResult result = mergeResults(resultMap);
		long endTime = EnvironmentEdgeManager.currentTimeMillis();

		if ((result.initCost > result.newCost && result.plans != null) ||
                (plans != null && plans.size() > 0)) {
			if (plans != null) {
				result.plans.addAll(plans);
			}
			LOG.info("Finished computing new load balance plan.  Computation took "
					+ (endTime - startTime)
					+ "ms to try "
					+ result.step
					+ " different iterations.  Found a solution that moves "
					+ result.plans.size()
					+ " regions; Going from a computed cost of "
					+ result.initCost + " to a new cost of " + result.newCost);

			return result.plans;
		}

		LOG.info("Could not find a better load balance plan.  Tried "
				+ result.step
				+ " different configurations in "
				+ (endTime - startTime)
				+ "ms, and did not find anything with a computed cost less than "
				+ result.initCost);

		return null;
	}

    /**
     * update recent region load ,should do this before calculate cost
     */
    private void updateRecentRegionLoad(){
        for (Map.Entry<String, List<RegionLoad>> regionStatusEntry : loads.entrySet()) {
            String regionName = regionStatusEntry.getKey();
            List<RegionLoad> regionLoads = regionStatusEntry.getValue();
            RegionLoadStatus regionLoadStatus = regionLoadStatusMap.get(regionName);
            if (regionLoadStatus == null){
                regionLoadStatus = new RegionLoadStatus();
                regionLoadStatusMap.put(regionName,regionLoadStatus);
            }

            if (regionLoads.size() > 0){
                regionLoadStatus.setRecentRegionLoad(regionLoads.get(regionLoads.size() - 1)); // get last one
//                LOG.info("setRecentRegionLoad " + regionLoads.get(regionLoads.size() - 1).toString());
            }
        }
    }

    /**
     *  update last balance region load ,should do this before calculate cost
     */
    private void updateLastRegionLoad(){
        for (Map.Entry<String, List<RegionLoad>> regionStatusEntry : loads.entrySet()) {
            String regionName = regionStatusEntry.getKey();
            List<RegionLoad> regionLoads = regionStatusEntry.getValue();
            RegionLoadStatus regionLoadStatus = regionLoadStatusMap.get(regionName);
            if (regionLoadStatus == null){
                regionLoadStatus = new RegionLoadStatus();
                regionLoadStatusMap.put(regionName,regionLoadStatus);
            }

            if (regionLoads.size() > 0){
                regionLoadStatus.setLastBalanceRegionLoad(regionLoads.get(regionLoads.size() - 1)); // get last one
//                LOG.info("setLastBalanceRegionLoad " + regionLoads.get(regionLoads.size() - 1).toString());
            }
        }
    }

    /**
     *   create table based cluster state
     */
	private Map<String, Map<ServerName, List<HRegionInfo>>> split(
			Map<ServerName, List<HRegionInfo>> clusterState) {

        Collection<ServerName> onlineServers = clusterStatus.getServers();
		
		Map<String, Map<ServerName, List<HRegionInfo>>> result = new HashMap<String, Map<ServerName, List<HRegionInfo>>>();
		for (Map.Entry<ServerName, List<HRegionInfo>> entry : clusterState.entrySet()) {
			ServerName serverName = entry.getKey();
			List<HRegionInfo> states = entry.getValue();
			for (HRegionInfo info : states) {
				String tableName = info.getTableNameAsString();
				Map<ServerName, List<HRegionInfo>> tabledStateMap = result.get(tableName);
				if (tabledStateMap == null) {
					tabledStateMap = new HashMap<ServerName, List<HRegionInfo>>();
                    for (ServerName onlineServer : onlineServers) {
                        List<HRegionInfo> tabledState = tabledStateMap.get(onlineServer);
                        if (tabledState == null) {
                            tabledState = new ArrayList<HRegionInfo>(states.size());
                            tabledStateMap.put(onlineServer, tabledState);
                        }
                    }
                    result.put(tableName, tabledStateMap);
				}
				List<HRegionInfo> tabledState = tabledStateMap.get(serverName);
				if (tabledState == null) {
					continue; // should not be here
				}
				tabledState.add(info);
			}
		}
		return result;
	}

    /**
     *   merge region plans since plans are for each table
     */
	private BalancedClusterResult mergeResults(
			Map<String, BalancedClusterResult> resultMap) {

		BalancedClusterResult result = new BalancedClusterResult();
        result.plans = new ArrayList<RegionPlan>();

		for (BalancedClusterResult r : resultMap.values()) {
			if (r.plans != null) {
				result.initCost += r.initCost;
				result.newCost += r.newCost;
				result.step += r.step;
				
				if (result.plans == null) {
					result.plans = r.plans;
				} else {
					result.plans.addAll(r.plans);
				}
			}
		}
		return result;
	}

    /**
     *   balance empty regions
     *   1.get and save empty region info
     *   2.remove empty region info from table base cluster state
     *   3.create region plans from empty region to balance empty region
     */
	private List<RegionPlan> balanceEmptyRegions(
			Map<String, Map<ServerName, List<HRegionInfo>>> tableSplitMapState) {

        Collection<ServerName> onlineServers = clusterStatus.getServers();
		List<RegionPlan> result = null;
		for (Map.Entry<String,Map<ServerName, List<HRegionInfo>>> tabledStateEntry : tableSplitMapState.entrySet()) {

            Map<ServerName, List<HRegionInfo>> tabledState = tabledStateEntry.getValue();

			int totalEmptyRegionNumber = 0;
			
			HashMap<ServerName, List<HRegionInfo>> emtpyRegions = new HashMap<ServerName, List<HRegionInfo>>();
            for (ServerName serverName : onlineServers) {
                emtpyRegions.put(serverName, new ArrayList<HRegionInfo>());
            }

			for (Map.Entry<ServerName, List<HRegionInfo>> entry: tabledState.entrySet()) {
				ServerName serverName = entry.getKey();
				List<HRegionInfo> infos = entry.getValue();
				List<HRegionInfo> emptyInfos = emtpyRegions.get(serverName);
                if (emptyInfos == null) {
                    // Why did it happen?
                    continue;
                }

				for (HRegionInfo info : infos) {
					String regionName = Bytes.toString(info.getRegionName());
					List<RegionLoad> load = loads.get(regionName);

					if (load != null && load.size() > 0) {
						RegionLoad rl = load.get(load.size() - 1); // Get last one
						if (rl.getTotalCompactingKVs() == 0 && rl.getStorefiles() == 0) { // Empty region numberOfStorefiles=0 && totalCompactingKVs=0
							++totalEmptyRegionNumber;
							emptyInfos.add(info);
						}
					}
				}
				infos.removeAll(emptyInfos);
			}

            LOG.info("Empty region info:");
            for (ServerName serverName : emtpyRegions.keySet()) {
                LOG.info("Table " + tabledStateEntry.getKey() + " Server "+serverName + " has "+ emtpyRegions.get(serverName).size() + " empty regions");
            }

			int serverNumber = emtpyRegions.size();
			Cluster cluster = new Cluster(emtpyRegions, loads, regionFinder);
			int numberOfRegionsPerServer = totalEmptyRegionNumber / serverNumber + 1;
			int regionNumberPerServer[] = new int[cluster.regionsPerServer.length];

			for (int i = 0; i < cluster.regionsPerServer.length; ++i) {
				regionNumberPerServer[i] = cluster.regionsPerServer[i].length;
			}

			int swapCount = 0;
            while (true) {
                boolean swapped = false;
                for (int i = 0; (!swapped) && i < cluster.regionsPerServer.length; ++i) {
                    if (cluster.regionsPerServer[i].length > numberOfRegionsPerServer) {
                        for (int j = numberOfRegionsPerServer; (!swapped) && j < cluster.regionsPerServer[i].length; ++j) {
                            for (int k = 0; (!swapped) && k < cluster.regionsPerServer.length; ++k) {
                                if (regionNumberPerServer[k] < numberOfRegionsPerServer) {
                                    ++swapCount;
                                    ++regionNumberPerServer[k];
                                    try {
                                        cluster.moveOrSwapRegion(i, k, cluster.regionsPerServer[i][j], -1);
                                        swapped = true;
                                    }catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                }
                            }
                        }
                    }
                }
                if (!swapped) {
                    break;
                }
            }

            LOG.info("Begin create empty region plan:");
			if (swapCount > 0) {
				List<RegionPlan> plans = createRegionPlans(cluster);
				if (plans != null) {
					if (result == null) {
						result = plans;
					} else {
						result.addAll(plans);
					}
				}
			}
		}
		return result;
	}

}
