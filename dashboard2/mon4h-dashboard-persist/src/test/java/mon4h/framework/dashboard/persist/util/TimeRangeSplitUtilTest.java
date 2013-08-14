package mon4h.framework.dashboard.persist.util;

import mon4h.framework.dashboard.persist.data.TimeRange;
import mon4h.framework.dashboard.persist.util.TimeRangeSplitUtil;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * User: huang_jie
 * Date: 7/16/13
 * Time: 9:53 AM
 */
public class TimeRangeSplitUtilTest {
    private TimeRange baseTimeRange;
    private List<TimeRange> baseTimeRanges;

    @Before
    public void setUp() throws Exception {
        baseTimeRange = new TimeRange();
        baseTimeRange.startTime = 100;
        baseTimeRange.endTime = 200;

        baseTimeRanges = new ArrayList<TimeRange>();
        baseTimeRanges.add(baseTimeRange);

        TimeRange baseTR = new TimeRange();
        baseTR.startTime = 300;
        baseTR.endTime = 400;
        baseTimeRanges.add(baseTR);
    }

    @Test
    public void testSplit_1() throws Exception {

        List<TimeRange> result = TimeRangeSplitUtil.split(baseTimeRange, null);
        assert result.size() == 1;
        assert result.get(0).startTime == 100 && result.get(0).endTime == 200;
    }


    @Test
    public void testSplit_2() throws Exception {
        List<TimeRange> scope = new ArrayList<TimeRange>();
        TimeRange timeRange1 = new TimeRange();
        timeRange1.startTime = 150;
        timeRange1.endTime = 160;
        scope.add(timeRange1);

        TimeRange timeRange2 = new TimeRange();
        timeRange2.startTime = 175;
        timeRange2.endTime = 180;
        scope.add(timeRange2);

        List<TimeRange> result = TimeRangeSplitUtil.split(baseTimeRange, scope);
        assert result.size() == 3;
        assert result.get(0).startTime == 100 && result.get(0).endTime == 150;
        assert result.get(1).startTime == 160 && result.get(1).endTime == 175;
        assert result.get(2).startTime == 180 && result.get(2).endTime == 200;
    }

    @Test
    public void testSplit_3() throws Exception {
        List<TimeRange> scope = new ArrayList<TimeRange>();
        TimeRange timeRange1 = new TimeRange();
        timeRange1.startTime = 50;
        timeRange1.endTime = 260;
        scope.add(timeRange1);

        List<TimeRange> result = TimeRangeSplitUtil.split(baseTimeRange, scope);
        assert result.size() == 0;
    }

    @Test
    public void testSplit_4() throws Exception {
        List<TimeRange> scope = new ArrayList<TimeRange>();
        TimeRange timeRange1 = new TimeRange();
        timeRange1.startTime = 50;
        timeRange1.endTime = 60;
        scope.add(timeRange1);

        List<TimeRange> result = TimeRangeSplitUtil.split(baseTimeRange, scope);
        assert result.size() == 1;
        assert result.get(0).startTime == 100 && result.get(0).endTime == 200;
    }

    @Test
    public void testSplit_5() throws Exception {
        List<TimeRange> scope = new ArrayList<TimeRange>();
        TimeRange timeRange1 = new TimeRange();
        timeRange1.startTime = 250;
        timeRange1.endTime = 260;
        scope.add(timeRange1);

        List<TimeRange> result = TimeRangeSplitUtil.split(baseTimeRange, scope);
        assert result.size() == 1;
        assert result.get(0).startTime == 100 && result.get(0).endTime == 200;
    }

    @Test
    public void testSplit_6() throws Exception {
        List<TimeRange> scope = new ArrayList<TimeRange>();
        TimeRange timeRange1 = new TimeRange();
        timeRange1.startTime = 50;
        timeRange1.endTime = 60;
        scope.add(timeRange1);
        TimeRange timeRange2 = new TimeRange();
        timeRange2.startTime = 250;
        timeRange2.endTime = 260;
        scope.add(timeRange2);

        List<TimeRange> result = TimeRangeSplitUtil.split(baseTimeRange, scope);
        assert result.size() == 1;
        assert result.get(0).startTime == 100 && result.get(0).endTime == 200;
    }

    @Test
    public void testSplit_7() throws Exception {
        List<TimeRange> scope = new ArrayList<TimeRange>();
        TimeRange timeRange1 = new TimeRange();
        timeRange1.startTime = 50;
        timeRange1.endTime = 60;
        scope.add(timeRange1);
        TimeRange timeRange2 = new TimeRange();
        timeRange2.startTime = 80;
        timeRange2.endTime = 160;
        scope.add(timeRange2);

        List<TimeRange> result = TimeRangeSplitUtil.split(baseTimeRange, scope);
        assert result.size() == 1;
        assert result.get(0).startTime == 160 && result.get(0).endTime == 200;
    }

    @Test
    public void testSplit_8() throws Exception {
        List<TimeRange> scope = new ArrayList<TimeRange>();
        TimeRange timeRange1 = new TimeRange();
        timeRange1.startTime = 250;
        timeRange1.endTime = 260;
        scope.add(timeRange1);
        TimeRange timeRange2 = new TimeRange();
        timeRange2.startTime = 80;
        timeRange2.endTime = 160;
        scope.add(timeRange2);

        List<TimeRange> result = TimeRangeSplitUtil.split(baseTimeRange, scope);
        assert result.size() == 1;
        assert result.get(0).startTime == 160 && result.get(0).endTime == 200;
    }

    @Test
    public void testSplit_list_1() throws Exception {
        List<TimeRange> scope = new ArrayList<TimeRange>();
        TimeRange timeRange1 = new TimeRange();
        timeRange1.startTime = 250;
        timeRange1.endTime = 260;
        scope.add(timeRange1);
        TimeRange timeRange2 = new TimeRange();
        timeRange2.startTime = 80;
        timeRange2.endTime = 160;
        scope.add(timeRange2);

        List<TimeRange> result = TimeRangeSplitUtil.split(new ArrayList<TimeRange>(), scope);
        assert result.size() == 0;
    }

    @Test
    public void testSplit_list_2() throws Exception {
        List<TimeRange> scope = new ArrayList<TimeRange>();
        TimeRange timeRange1 = new TimeRange();
        timeRange1.startTime = 250;
        timeRange1.endTime = 260;
        scope.add(timeRange1);
        TimeRange timeRange2 = new TimeRange();
        timeRange2.startTime = 80;
        timeRange2.endTime = 160;
        scope.add(timeRange2);

        List<TimeRange> result = TimeRangeSplitUtil.split(baseTimeRanges, scope);
        assert result.size() == 2;
        assert result.get(0).startTime == 160 && result.get(0).endTime == 200;
        assert result.get(1).startTime == 300 && result.get(1).endTime == 400;
    }

    @Test
    public void testSplit_list_3() throws Exception {
        List<TimeRange> scope = new ArrayList<TimeRange>();
        TimeRange timeRange1 = new TimeRange();
        timeRange1.startTime = 350;
        timeRange1.endTime = 360;
        scope.add(timeRange1);
        TimeRange timeRange2 = new TimeRange();
        timeRange2.startTime = 80;
        timeRange2.endTime = 160;
        scope.add(timeRange2);

        List<TimeRange> result = TimeRangeSplitUtil.split(baseTimeRanges, scope);
        assert result.size() == 3;
        assert result.get(0).startTime == 160 && result.get(0).endTime == 200;
        assert result.get(1).startTime == 300 && result.get(1).endTime == 350;
        assert result.get(2).startTime == 360 && result.get(2).endTime == 400;
    }
}
