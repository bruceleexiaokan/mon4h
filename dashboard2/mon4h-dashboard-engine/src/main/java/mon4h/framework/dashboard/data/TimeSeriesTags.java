package mon4h.framework.dashboard.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.HashSet;
import java.util.Set;


public class TimeSeriesTags {
	private String nameSpace;
	private String metricsName;
	private Set<String> tags = new HashSet<String>();
	
	public String getNameSpace() {
		return nameSpace;
	}
	public void setNameSpace(String nameSpace) {
		this.nameSpace = nameSpace;
	}
	public String getMetricsName() {
		return metricsName;
	}
	public void setMetricsName(String metricsName) {
		this.metricsName = metricsName;
	}
	
	public void removeTag(String tag){
		tags.remove(tag);
	}
	
	public void addTag(String tag){
		tags.add(tag);
	}
	
	public Set<String> getTags(){
		return tags;
	}
	
	public void buildJson(JSONStringer builder) throws JSONException{
		builder.object();
		builder.key("namespace").value(nameSpace);
		builder.key("metrics-name").value(metricsName);
		builder.key("tags");
			builder.array();
			for(String tag:tags){
				builder.value(tag);
			}
			builder.endArray();
		builder.endObject();
	}
	
	public static TimeSeriesTags parseFromJson(JSONObject jsonObj){
		TimeSeriesTags rt = new TimeSeriesTags();
		rt.setNameSpace(jsonObj.optString("namespace", null));
		rt.setMetricsName(jsonObj.optString("metrics-name", null));
		JSONArray tagArray = jsonObj.optJSONArray("tags");
		if(tagArray != null){
			for(int i=0;i<tagArray.length();i++){
				String tag = tagArray.optString(i, null);
				if(tag != null){
					rt.addTag(tag);
				}
			}
		}
		return rt;
	}
}
