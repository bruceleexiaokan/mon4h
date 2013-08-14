package mon4h.framework.dashboard.data;

import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Set;

import mon4h.framework.dashboard.persist.data.DataPoint;


/**
 * Utility class that provides common, generally useful aggregators.
 */

public class Downsamplers {
	/** Aggregator that sums up all the data points. */
	  public static final DownsampleFunc SUM = new Sum();

	  /** Aggregator that returns the minimum data point. */
	  public static final DownsampleFunc MIN = new Min();

	  /** Aggregator that returns the maximum data point. */
	  public static final DownsampleFunc MAX = new Max();

	  /** Aggregator that returns the average value of the data point. */
	  public static final DownsampleFunc AVG = new Avg();

	  /** Aggregator that returns the Standard Deviation of the data points. */
	  public static final DownsampleFunc DEV = new StdDev();
	  
	  /** Aggregator that returns the first of the data points */
	  public static final DownsampleFunc RAT = new Rate();

	  /** Maps an aggregator name to its instance. */
	  private static final HashMap<String, DownsampleFunc> aggregators;

	  static {
	    aggregators = new HashMap<String, DownsampleFunc>(6);
	    aggregators.put("sum", SUM);
	    aggregators.put("min", MIN);
	    aggregators.put("max", MAX);
	    aggregators.put("avg", AVG);
	    aggregators.put("dev", DEV);
	    aggregators.put("rat", RAT);
	  }

	  private Downsamplers() {
	    // Can't create instances of this utility class.
	  }

	  /**
	   * Returns the set of the names that can be used with {@link #get get}.
	   */
	  public static Set<String> set() {
	    return aggregators.keySet();
	  }

	  /**
	   * Returns the aggregator corresponding to the given name.
	   * @param name The name of the aggregator to get.
	   * @throws NoSuchElementException if the given name doesn't exist.
	   * @see #set
	   */
	  public static DownsampleFunc get(final String name) {
	    final DownsampleFunc agg = aggregators.get(name);
	    if (agg != null) {
	      return agg;
	    }
	    throw new NoSuchElementException("No such aggregator: " + name);
	  }

	  private static final class Sum implements DownsampleFunc {

		@Override
		public DataPoint downsample(DataPoint current, DataPoint[] delta) {
			return FuncUtils.streamSum(current, delta);
		}

		@Override
		public Double getValue(DataPoint dp) {
			return FuncUtils.getSumValue(dp);
		}

	  }
	  
	  
	  private static final class Rate implements DownsampleFunc {

		@Override
		public DataPoint downsample(DataPoint current, DataPoint[] delta) {
			return FuncUtils.streamRate(current, delta);
		}

		@Override
		public Double getValue(DataPoint dp) {
			return FuncUtils.getRateValue(dp);
		}
	  }

	  private static final class Min implements DownsampleFunc {

		@Override
		public DataPoint downsample(DataPoint current, DataPoint[] delta) {
			return FuncUtils.streamMin(current, delta);
		}

		@Override
		public Double getValue(DataPoint dp) {
			return FuncUtils.getMinValue(dp);
		}

	  }
	  
	  private static final class Max implements DownsampleFunc {

		@Override
		public DataPoint downsample(DataPoint current, DataPoint[] delta) {
			return FuncUtils.streamMax(current, delta);
		}

		@Override
		public Double getValue(DataPoint dp) {
			return FuncUtils.getMaxValue(dp);
		}
	  }

	  private static final class Avg implements DownsampleFunc {

	   
	    public String toString() {
	      return "avg";
	    }

		@Override
		public DataPoint downsample(DataPoint current, DataPoint[] delta) {
			return FuncUtils.streamAvg(current, delta);
		}

		@Override
		public Double getValue(DataPoint dp) {
			return FuncUtils.getAvgValue(dp);
		}
	  }

	  /**
	   * Standard Deviation aggregator.
	   * Can compute without storing all of the data points in memory at the same
	   * time.  This implementation is based upon a
	   * <a href="http://www.johndcook.com/standard_deviation.html">paper by John
	   * D. Cook</a>, which itself is based upon a method that goes back to a 1962
	   * paper by B.  P. Welford and is presented in Donald Knuth's Art of
	   * Computer Programming, Vol 2, page 232, 3rd edition
	   */
	  private static final class StdDev implements DownsampleFunc {


	    public String toString() {
	      return "dev";
	    }

		@Override
		public DataPoint downsample(DataPoint current, DataPoint[] delta) {
			return FuncUtils.streamDev(current, delta);
		}

		@Override
		public Double getValue(DataPoint dp) {
			return FuncUtils.getDevValue(dp);
		}
	  }
}
