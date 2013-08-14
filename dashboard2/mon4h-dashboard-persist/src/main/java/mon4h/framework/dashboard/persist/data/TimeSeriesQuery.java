package mon4h.framework.dashboard.persist.data;

import mon4h.framework.dashboard.common.util.StringUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.*;
import java.util.Map.Entry;

public class TimeSeriesQuery {
    private String nameSpace;
    private String metricsName;
    private boolean matchAllTags = false;
    private Map<String, Set<String>> filterTags = new HashMap<String, Set<String>>();

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

    public boolean isMatchAllTags() {
        return matchAllTags;
    }

    public void setMatchAllTags(boolean matchAllTags) {
        this.matchAllTags = matchAllTags;
    }

    public void addFilterTagValue(String tag, String value) {
        Set<String> stored = filterTags.get(tag);
        if (stored == null) {
            stored = new HashSet<String>();
            filterTags.put(tag, stored);
        } else {
            if (stored.size() == 0) {
                return; // wildcard
            }
        }
        stored.add(value);
    }

    public void setFilterTags(Map<String, Set<String>> filterTags) {
        this.filterTags = filterTags;
    }

    public void setWildcardFilterTagValue(String tag) {
        Set<String> stored = new HashSet<String>();
        filterTags.put(tag, stored);
    }

    public Set<String> getFilterTagValues(String tag) {
        return filterTags.get(tag);
    }

    public Map<String, Set<String>> getFilterTags() {
        return filterTags;
    }

    public void buildJson(JSONStringer builder) throws JSONException {
        builder.object();
        builder.key("namespace").value(nameSpace);
        builder.key("metrics-name").value(metricsName);
        String tagsKey = null;
        if (matchAllTags == false) {
            tagsKey = "tag-search-part";
        } else {
            tagsKey = "tag-search-all";
        }
        builder.key(tagsKey);
        builder.object();
        for (Entry<String, Set<String>> entry : filterTags.entrySet()) {
            builder.key(entry.getKey());
            builder.array();
            for (String value : entry.getValue()) {
                builder.value(value);
            }
            builder.endArray();
        }
        builder.endObject();
        builder.endObject();
    }

    @SuppressWarnings("unchecked")
    public static TimeSeriesQuery parseFromJson(JSONObject jsonObj) {
        TimeSeriesQuery rt = new TimeSeriesQuery();
        rt.setNameSpace(StringUtil.trimAndLowerCase(jsonObj.optString("namespace", null)));
        rt.setMetricsName(StringUtil.trimAndLowerCase(jsonObj.optString("metrics-name", null)));
        Set<String> keys = jsonObj.keySet();
        JSONObject tagsObj = null;
        if (keys.contains("tag-search-all")) {
            rt.setMatchAllTags(true);
            tagsObj = jsonObj.optJSONObject("tag-search-all");
        } else if (keys.contains("tag-search-part")) {
            rt.setMatchAllTags(false);
            tagsObj = jsonObj.optJSONObject("tag-search-part");
        }
        if (tagsObj != null) {
            Iterator<String> it = tagsObj.keys();
            while (it.hasNext()) {
                String tag = it.next();
                JSONArray valueArray = tagsObj.optJSONArray(tag);
                if (valueArray == null) {
                    rt.setWildcardFilterTagValue(tag);
                } else {
                    if (valueArray.length() == 0) {
                        rt.setWildcardFilterTagValue(tag);
                    } else {
                        for (int i = 0; i < valueArray.length(); i++) {
                            rt.addFilterTagValue(StringUtil.trimAndLowerCase(tag), StringUtil.trimAndLowerCase(valueArray.optString(i, null)));
                        }
                    }
                }
            }
        }
        return rt;
    }

}
