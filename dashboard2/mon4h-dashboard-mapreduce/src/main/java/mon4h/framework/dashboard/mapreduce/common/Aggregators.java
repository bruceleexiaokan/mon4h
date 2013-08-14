// This file is part of OpenTSDB.
// Copyright (C) 2010-2012  The OpenTSDB Authors.
//
// This program is free software: you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 2.1 of the License, or (at your
// option) any later version.  This program is distributed in the hope that it
// will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
// General Public License for more details.  You should have received a copy
// of the GNU Lesser General Public License along with this program.  If not,
// see <http://www.gnu.org/licenses/>.
package mon4h.framework.dashboard.mapreduce.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;

/**
 * Utility class that provides common, generally useful aggregators.
 */

public class Aggregators {
	/** Aggregator that sums up all the data points. */
	  public static final Aggregator SUM = new Sum();

	  /** Aggregator that returns the minimum data point. */
	  public static final Aggregator MIN = new Min();

	  /** Aggregator that returns the maximum data point. */
	  public static final Aggregator MAX = new Max();
	  
	  /** Aggregator that returns the middle data point. */
	  public static final Aggregator MID = new Mid();

	  /** Aggregator that returns the average value of the data point. */
	  public static final Aggregator AVG = new Avg();

	  /** Aggregator that returns the Standard Deviation of the data points. */
	  public static final Aggregator DEV = new StdDev();
	  
	  /** Aggregator that returns the first of the data points */
	  public static final Aggregator RAT = new Rate();

	  /** Maps an aggregator name to its instance. */
	  private static final HashMap<String, Aggregator> aggregators;

	  static {
	    aggregators = new HashMap<String, Aggregator>(6);
	    aggregators.put("sum", SUM);
	    aggregators.put("min", MIN);
	    aggregators.put("max", MAX);
	    aggregators.put("mid", MID);
	    aggregators.put("avg", AVG);
	    aggregators.put("dev", DEV);
	    aggregators.put("rat", RAT);
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
	  public static Aggregator get(final String name) {
	    final Aggregator agg = aggregators.get(name);
	    if (agg != null) {
	      return agg;
	    }
	    throw new NoSuchElementException("No such aggregator: " + name);
	  }
	  
	  private static Long addLong(Long left,Long right){
			if(left == null){
				return right;
			}else {
				if(right == null){
					return left;
				}else{
					return left+right;
				}
			}
		}
	  
	  private static Double addDouble(Double left,Double right){
			if(left == null){
				return right;
			}else {
				if(right == null){
					return left;
				}else{
					return left+right;
				}
			}
		}

	  private static final class Sum implements Aggregator {

	    public Long runLong(final Longs values) {
	      Long result = values.nextLongValue();
	      while (values.hasNextValue()) {
	        result = addLong(result,values.nextLongValue());
	      }
	      return result;
	    }

	    public Double runDouble(final Doubles values) {
	      Double result = values.nextDoubleValue();
	      while (values.hasNextValue()) {
	        result = addDouble(result,values.nextDoubleValue());
	      }
	      return result;
	    }

	    public String toString() {
	      return "sum";
	    }

	  }
	  
	  private static final class Rate implements Aggregator {

		@Override
		public Long runLong(final Longs values) {
			if( values.hasNextValue() ) {
				return values.nextLongValue();
			}
			return null;
		}

		@Override
		public Double runDouble(final Doubles values) {
			if( values.hasNextValue() ) {
				return values.nextDoubleValue();
			}
			return null;
		}
		
		public String toString() {
		     return "rat";
		}
		 
	  }

	  private static final class Min implements Aggregator {

	    public Long runLong(final Longs values) {
	      Long min = values.nextLongValue();
	      while (values.hasNextValue()) {
	        final Long val = values.nextLongValue();
	        if(min == null){
	        	min = val;
	        }else{
		        if (val != null && val < min) {
		          min = val;
		        }
	        }
	      }
	      return min;
	    }

	    public Double runDouble(final Doubles values) {
	      Double min = values.nextDoubleValue();
	      while (values.hasNextValue()) {
	        final Double val = values.nextDoubleValue();
	        if(min == null){
	        	min = val;
	        }else{
		        if (val != null && val < min) {
		          min = val;
		        }
	        }
	      }
	      return min;
	    }

	    public String toString() {
	      return "min";
	    }

	  }
	  
	  private static final class Max implements Aggregator {

		    public Long runLong(final Longs values) {
		      Long max = values.nextLongValue();
		      while (values.hasNextValue()) {
		        final Long val = values.nextLongValue();
		        if(max == null){
		        	max = val;
		        }else{
			        if (val != null && val > max) {
			        	max = val;
			        }
		        }
		      }
		      return max;
		    }

		    public Double runDouble(final Doubles values) {
		    	Double max = values.nextDoubleValue();
		      while (values.hasNextValue()) {
		        final Double val = values.nextDoubleValue();
		        if(max == null){
		        	max = val;
		        }else{
			        if (val != null && val > max) {
			        	max = val;
			        }
		        }
		      }
		      return max;
		    }

		    public String toString() {
		      return "max";
		    }

	  }

	  private static final class Mid implements Aggregator {

	    public Long runLong(final Longs values) {
	      TreeSet<Long> valueSet = new TreeSet<Long>();
	      Long val = values.nextLongValue();
	      Long rt = null;
	      if(val != null){
	    	  rt = val;
	    	  valueSet.add(val);
	      }
	      while (values.hasNextValue()) {
	    	  val = values.nextLongValue();
	    	  if(val != null){
	    		  rt = val;
		    	  valueSet.add(val);
		      }
	      }
	      if(valueSet.size()>2){
	    	  int pos = valueSet.size()/2;
	    	  int index = 0;
	    	  Iterator<Long> it = valueSet.iterator();
	    	  while(it.hasNext()){
	    		  if(index == pos){
	    			  return it.next();
	    		  }
	    		  it.next();
	    		  index++;
	    	  }
	      }
	      return rt;
	    }

	    public Double runDouble(final Doubles values) {
	    	TreeSet<Double> valueSet = new TreeSet<Double>();
	        Double val = values.nextDoubleValue();
	        Double rt = null;
	        if(val != null){
	        	rt = val;
	        	valueSet.add(val);
	        }
	        while (values.hasNextValue()) {
	        	val = values.nextDoubleValue();
	        	if(val != null){
		        	rt = val;
		        	valueSet.add(val);
		        }
	        }
	        if(valueSet.size()>2){
	      	  int pos = valueSet.size()/2;
	      	  int index = 0;
	      	  Iterator<Double> it = valueSet.iterator();
	      	  while(it.hasNext()){
	      		  if(index == pos){
	      			  return it.next();
	      		  }
	      		  it.next();
	      		  index++;
	      	  }
	        }
	        return rt;
	    }

	    public String toString() {
	      return "mid";
	    }

	  }

	  private static final class Avg implements Aggregator {

	    public Long runLong(final Longs values) {
	      Long result = values.nextLongValue();
	      int n = 0;
	      if(result != null){
	    	  n++;
	      }
	      while (values.hasNextValue()) {
	    	Long val = values.nextLongValue();
	        result = addLong(result,val);
	        if(val != null){
	        	n++;
	        }
	      }
	      if(result == null){
	    	  return null;
	      }
	      return result / n;
	    }

	    public Double runDouble(final Doubles values) {
	      Double result = values.nextDoubleValue();
	      int n = 0;
	      if(result != null){
	    	  n++;
	      }
	      while (values.hasNextValue()) {
	    	Double val = values.nextDoubleValue();
	        result = addDouble(result,val);
	        if(val != null){
	        	n++;
	        }
	      }
	      if(result == null){
	    	  return null;
	      }
	      return result / n;
	    }

	    public String toString() {
	      return "avg";
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
	  private static final class StdDev implements Aggregator {

	    public Long runLong(final Longs values) {
	    	List<Long> notnulls = new ArrayList<Long>();
	    	while(values.hasNextValue()){
	    		Long val = values.nextLongValue();
	    		if(val != null){
	    			notnulls.add(val);
	    		}
	    	}
	    	if(notnulls.size() == 0){
	    		return null;
	    	}else if(notnulls.size() == 1){
	    		return 0L;
	    	}
	      double old_mean = notnulls.get(0);

	      int pos = 1;
	      long n = 2;
	      double new_mean = 0;
	      double variance = 0;
	      do {
	        final double x = notnulls.get(pos);
	        new_mean = old_mean + (x - old_mean) / n;
	        variance += (x - old_mean) * (x - new_mean);
	        old_mean = new_mean;
	        n++;
	        pos++;
	      } while (pos<notnulls.size());

	      return (long) Math.sqrt(variance / (n - 1));
	    }

	    public Double runDouble(final Doubles values) {
	    	List<Double> notnulls = new ArrayList<Double>();
	    	while(values.hasNextValue()){
	    		Double val = values.nextDoubleValue();
	    		if(val != null){
	    			notnulls.add(val);
	    		}
	    	}
	    	if(notnulls.size() == 0){
	    		return null;
	    	}else if(notnulls.size() == 1){
	    		return (double)0;
	    	}
	    	double old_mean = notnulls.get(0);
	     
	    	int pos = 0;
	      long n = 2;
	      double new_mean = 0;
	      double variance = 0;
	      do {
	        final double x = notnulls.get(pos);
	        new_mean = old_mean + (x - old_mean) / n;
	        variance += (x - old_mean) * (x - new_mean);
	        old_mean = new_mean;
	        n++;
	        pos++;
	      } while (pos<notnulls.size());

	      return Math.sqrt(variance / (n - 1));
	    }

	    public String toString() {
	      return "dev";
	    }
	  }
}
