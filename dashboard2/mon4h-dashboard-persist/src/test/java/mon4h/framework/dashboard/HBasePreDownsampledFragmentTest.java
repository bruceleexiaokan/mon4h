package mon4h.framework.dashboard;

import mon4h.framework.dashboard.persist.dao.impl.HBasePreDownsampledFragment;
import mon4h.framework.dashboard.persist.data.DataPointStream;
import mon4h.framework.dashboard.persist.data.IntervalType;
import mon4h.framework.dashboard.persist.data.TimeRange;

import org.junit.Before;
import org.junit.Test;


public class HBasePreDownsampledFragmentTest extends DashboardAbstractTest {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    HBasePreDownsampledFragment fragment = null;
    TimeRange timeRange = new TimeRange();

    @SuppressWarnings("unused")
	@Test
    public void run() {

        try {
            long time = System.currentTimeMillis();
            timeRange.startTime = time - 3600000;
            timeRange.endTime = time;

            fragment = new HBasePreDownsampledFragment(IntervalType.FEATURE_INTERVAL_TYPE_HOUR);
//		fragment.setDataFilterInfo(mid, tsids, timeRange, FeatureDataType.SUM);
            DataPointStream dpStream = null;
            dpStream = fragment.getTimeSeriesResultFragment();
        } catch (Exception e) {
//            e.printStackTrace();
        }
    }
}
