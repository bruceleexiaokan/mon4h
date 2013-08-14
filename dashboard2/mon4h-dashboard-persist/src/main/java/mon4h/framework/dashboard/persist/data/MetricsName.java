package mon4h.framework.dashboard.persist.data;

public class MetricsName {
	public String namespace;
	public String name;
	
	public String getFullName(){
		String spliter = "__";
		if(namespace == null){
			return spliter + "ns-null" + spliter + name;
		}
		return spliter + namespace + spliter + name;
	}
	
	@Override
    public int hashCode() {
		String spliter = "__";
		String genkey = spliter + namespace + spliter + name;
		return genkey.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
		if(obj == null){
			return false;
		}
		if(obj instanceof MetricsName) {
			MetricsName other = (MetricsName)obj;
			if(equals(namespace,other.namespace)
		    	&& equals(name,other.name)){
		        return true;
			}
		}
		return false;
    }
    
    private boolean equals(Object left,Object right) {
         if(left == null && right == null){
         	return true;
         }
         if(left != null && right != null){
         	return left.equals(right);
         }
         return false;
    }
}
