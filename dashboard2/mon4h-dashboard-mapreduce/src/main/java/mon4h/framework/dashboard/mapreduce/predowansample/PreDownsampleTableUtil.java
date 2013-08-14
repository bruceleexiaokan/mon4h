package mon4h.framework.dashboard.mapreduce.predowansample;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PreDownsampleTableUtil {
	
	public static class TableRead {
		public String ReadTable;
		public String ZnodePath;
		public String MetricName;
		
		
		public TableRead( String ReadTable,String ZnodePath,String MetricName ) {
			this.ReadTable = ReadTable;
			this.ZnodePath = ZnodePath;
			this.MetricName = MetricName;
		}
		
		@Override
        public int hashCode(){
			String genkey = ReadTable  + "_-_" + ZnodePath;
			return genkey.hashCode();
        }
        
        @Override
        public boolean equals(Object obj){
			if(obj == null){
				return false;
			}
			if(obj instanceof TableRead){
				TableRead other = (TableRead)obj;
				if(equals(ReadTable,other.ReadTable) && 
					equals(ZnodePath,other.ZnodePath)){
			        return true;
				}
			}
			return false;
        }
        
        private boolean equals(Object left,Object right){
	         if(left == null && right == null){
	         	return true;
	         }
	         if(left != null && right != null){
	         	return left.equals(right);
	         }
	         return false;
        }
	}
	
	public static Map<String,Set<TableRead>> conf
		= new HashMap<String,Set<TableRead>>();
	
	public static void addTableReadAndWrite( String hbase,TableRead trhd	) {
		if( conf.get(hbase) != null ) {
			Set<TableRead> set = conf.get(hbase);
			set.add(trhd);
		} else {
			Set<TableRead> set = new HashSet<TableRead>();
			set.add(trhd);
			conf.put(hbase, set);
		}
	}
	
	public static void addTableReadAndWrite( String hbase,String znodepath,String ReadTable,String metricname ) {
		TableRead trhd = new TableRead(ReadTable,znodepath,metricname);
		if( conf.get(hbase) != null ) {
			Set<TableRead> set = conf.get(hbase);
			set.add(trhd);
		} else {
			Set<TableRead> set = new HashSet<TableRead>();
			set.add(trhd);
			conf.put(hbase, set);
		}
	}
}
