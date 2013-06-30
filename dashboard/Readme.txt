*****Eliminate all hardcodes, easy guide *******
- How to run the QueryEngine:
  In eclipse, run QueryEngine

- Configuration
  Now just hard code your configurations in QueryEngine.java
  	private static final String demoZKQuorum = "hadoop1";
	private static final String basePath = "/hbase";
	private static final String uidTable = "demo.tsdb-uid";
	private static final String metaTable = "demo.metrictag";
	private static final String dataTable = "demo.tsdb";
	// you should put your queryengine.xml and logback.xml here.
	private static final String defaultConfigDir = "D:/projects/mon4h/dashboard/dashboard-engine/conf";
	private static final Logger log = LoggerFactory.getLogger(QueryEngine.class);
	private static final int port = 8080;
	
 queryengine.xml and logback.xml are in mon4h\dashboard\dashboard-engine\conf directory
	
- Test
  - Put data point (HTTP POST)
    http://localhost:8080/metrics/putdatapoints
    {"version":1,"time-series-list":[{"time-series":{"namespace":null,"metrics-name":"test.sync","tags":{"appid":"920110","collector":"172.16.145.205"}},"force-create":true,"value-type":"double","data-points":[{"timestamp":"2013-06-30 10:03:45","value":33.9},{"timestamp":"2013-06-30 00:03:55","value":39.9},{"timestamp":"2013-06-30 00:04:05","value":40}]}]}
  - Get metrics name autocompletion (HTTP GET)
    http://localhost:8080/jsonp/getmetricstags?reqdata={"version":1,"time-series-pattern":{"namespace":null,"metrics-name":{"start-with":"__n"}}}
  - Get Group data points (HTTP GET)
    http://localhost:8080/jsonp/getgroupeddatapoints?reqdata={"version":1,"time-series-pattern":{"namespace":null,"metrics-name":"__ns-null__test.sync","tag-search-part":{}},"aggregator":{"accept-linear-interpolation":true,"function":"sum"},"downsampler":{"interval":"1m","function":"sum"},"max-datapoint-count":100,"start-time":"2013-06-30 00:00:00","end-time":"2013-06-30 00:10:00","rate":"false"}
    
    
  