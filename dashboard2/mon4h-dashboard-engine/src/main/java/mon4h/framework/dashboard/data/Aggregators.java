package mon4h.framework.dashboard.data;

import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Utility class that provides common, generally useful aggregators.
 */

public class Aggregators {
	/** Aggregator that sums up all the data points. */
	  public static final AggregateFunc SUM = new Sum();

	  /** Aggregator that returns the minimum data point. */
	  public static final AggregateFunc MIN = new Min();

	  /** Aggregator that returns the maximum data point. */
	  public static final AggregateFunc MAX = new Max();

	  /** Aggregator that returns the average value of the data point. */
	  public static final AggregateFunc AVG = new Avg();

	  /** Aggregator that returns the Standard Deviation of the data points. */
	  public static final AggregateFunc DEV = new StdDev();

	  /** Maps an aggregator name to its instance. */
	  private static final HashMap<String, AggregateFunc> aggregators;

	  static {
	    aggregators = new HashMap<String, AggregateFunc>(6);
	    aggregators.put("sum", SUM);
	    aggregators.put("min", MIN);
	    aggregators.put("max", MAX);
	    aggregators.put("avg", AVG);
	    aggregators.put("dev", DEV);
	  }

	  private Aggregators() {
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
	  public static AggregateFunc get(final String name) {
	    final AggregateFunc agg = aggregators.get(name);
	    if (agg != null) {
	      return agg;
	    }
	    throw new NoSuchElementException("No such aggregator: " + name);
	  }
	  
	  private static Double addDouble(Double left,Double right){
		  Double rt = null;
		  if(left == null){
			  if(right != null){
				  rt = right;
			  }
		  }else{
			  if(right == null){
				  rt = left;
			  }else{
				  rt = left + right;
			  }
		  }
		  return rt;
	  }
	  
	  private static Double minDouble(Double left,Double right){
		  Double rt = null;
		  if(left == null){
			  if(right != null){
				  rt = right;
			  }
		  }else{
			  if(right == null){
				  rt = left;
			  }else{
				  rt = Math.min(left, right);
			  }
		  }
		  return rt;
	  }
	  
	  private static Double maxDouble(Double left,Double right){
		  Double rt = null;
		  if(left == null){
			  if(right != null){
				  rt = right;
			  }
		  }else{
			  if(right == null){
				  rt = left;
			  }else{
				  rt = Math.max(left, right);
			  }
		  }
		  return rt;
	  }

	  private static final class Sum implements AggregateFunc {

		@Override
		public void aggregate(InterAggInfo interInfo,Double delta) {
			interInfo.sum = addDouble(interInfo.sum,delta);
		}

		@Override
		public Double getValue(InterAggInfo interInfo) {
			return interInfo.sum;
		}

	  }

	  private static final class Min implements AggregateFunc {

		  @Override
			public void aggregate(InterAggInfo interInfo,Double delta) {
			  interInfo.min = minDouble(interInfo.min,delta);
			}

			@Override
			public Double getValue(InterAggInfo interInfo) {
				return interInfo.min;
			}

	  }
	  
	  private static final class Max implements AggregateFunc {

		  @Override
			public void aggregate(InterAggInfo interInfo,Double delta) {
			  interInfo.max = maxDouble(interInfo.max,delta);
			}

			@Override
			public Double getValue(InterAggInfo interInfo) {
				return interInfo.max;
			}
	  }

	
	  private static final class Avg implements AggregateFunc {

	   
		  @Override
			public void aggregate(InterAggInfo interInfo,Double delta) {
			  interInfo.sum = addDouble(interInfo.sum,delta);
			  interInfo.count = addDouble(interInfo.count,1d);
			}

			@Override
			public Double getValue(InterAggInfo interInfo) {
				if(interInfo.count != null && interInfo.sum != null){
					return interInfo.sum/interInfo.count;
				}
				return null;
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
	  private static final class StdDev implements AggregateFunc {


	    public String toString() {
	      return "dev";
	    }

	    @Override
		public void aggregate(InterAggInfo interInfo,Double delta) {
	    	if(delta != null){
		    	if(interInfo.dev == null){
		    		interInfo.sum = delta;
	    			interInfo.count = 1d;
	    			interInfo.dev = 0d;
		    	}else{
		    		double oldavg = interInfo.sum/interInfo.count;
		    		double newavg = (interInfo.sum+delta)/(interInfo.count + 1d);
		    		interInfo.dev = interInfo.dev + interInfo.count*(oldavg-newavg)*(oldavg-newavg)+(delta-newavg)*(delta-newavg);
		    		interInfo.count = interInfo.count + 1d;
		    		interInfo.sum = interInfo.sum + delta;
		    	}
	    	}
		}

		@Override
		public Double getValue(InterAggInfo interInfo) {
			return interInfo.dev;
		}
	  }
}
