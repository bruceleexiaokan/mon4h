package mon4h.framework.dashboard.command.meta;


import mon4h.framework.dashboard.command.InterfaceException;
import mon4h.framework.dashboard.data.MetricsQuery;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;

public class GetMetaInfoRequest {
	private int version;
	private MetricsQuery metricsQuery;
	
	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public MetricsQuery getMetricsQuery() {
		return metricsQuery;
	}

	public void setMetricsQuery(MetricsQuery metricsQuery) {
		this.metricsQuery = metricsQuery;
	}
	
	public String build() throws InterfaceException {
		JSONStringer builder = new JSONStringer();
		try {
			builder.object();
			builder.key("version").value(version);
			builder.key("time-series-pattern");
			metricsQuery.buildJson(builder);
			builder.endObject();
		} catch (JSONException e) {
			throw new InterfaceException(e.getMessage(),e);
		} catch(NullPointerException e){
			throw new InterfaceException(e.getMessage(),e);
		}
		
		return builder.toString();
	}
	
	public static GetMetaInfoRequest parse(JSONTokener jsonTokener) throws InterfaceException{
		GetMetaInfoRequest rt = new GetMetaInfoRequest();
		try{
			JSONObject jsonObj = new JSONObject(jsonTokener);
			rt.setVersion(jsonObj.getInt("version"));
			rt.setMetricsQuery(MetricsQuery.parseFromJson(jsonObj.getJSONObject("time-series-pattern")));
		}catch(JSONException e){
			throw new InterfaceException(e.getMessage(),e);
		}
		return rt;
	}

}
