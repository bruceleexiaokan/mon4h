- How to run the QueryEngine:
  In eclipse, run QueryEngine

- Configuration
  queryengine.xml is bootstrap configuration file. This should be put into the classpath.
  (In eclipse, add "conf" directory to be source, which will put queryengine.xml in the classpath)
  There are 2 important configurations:
  1. tsdb config
     <tsdb>
		<zkQuorum>hadoop1</zkQuorum>
		<zkBasepath>/hbase</zkBasepath>
		<uidTable>demo.tsdb-uid</uidTable>
		<metaTable>demo.metrictag</metaTable>
		<!-- the format is <namespace>:<tablename>,<namespace>:<tablename> -->
		<tsTables>:demo.tsdb</tsTables>
	</tsdb>
  2. engine config
     <engine>
		<server>
			<port>8080</port>
			<log-home>D:\dashboard\log</log-home>
			<log-config-file>D:\projects\mon4h\dashboard\dashboard-engine\conf\queryengine-logback.xml</log-config-file>
			<meta-uptime-interval>300000</meta-uptime-interval>
	  ... ...
	
- Test
  - Put data point (HTTP POST)
    http://localhost:8080/metrics/putdatapoints
    {"version":1,"time-series-list":[{"time-series":{"namespace":null,"metrics-name":"test.sync","tags":{"appid":"920110","collector":"172.16.145.205"}},"force-create":true,"value-type":"double","data-points":[{"timestamp":"2013-06-30 10:03:45","value":33.9},{"timestamp":"2013-06-30 00:03:55","value":39.9},{"timestamp":"2013-06-30 00:04:05","value":40}]}]}
  - Get metrics name autocompletion (HTTP GET)
    http://localhost:8080/jsonp/getmetricstags?reqdata={"version":1,"time-series-pattern":{"namespace":null,"metrics-name":{"start-with":"test"}}}
  - Get Group data points (HTTP GET)
    http://localhost:8080/jsonp/getgroupeddatapoints?reqdata={"version":1,"time-series-pattern":{"namespace":null,"metrics-name":"test.sync","tag-search-part":{}},"aggregator":{"accept-linear-interpolation":true,"function":"sum"},"downsampler":{"interval":"1m","function":"sum"},"max-datapoint-count":100,"start-time":"2013-06-30 00:00:00","end-time":"2013-06-30 00:10:00","rate":"false"}
    
 - TSDB dump utility
   com.mon4h.dashboard.tools.datascanner.ScanDirect, use the same configuration as QueryEngine main class.
 
 - Package and deploy
   use "mvn clean install" to package all the dependent jars and scripts to be dashboard-engine-1.0.0-prod.tar.gz. 
   The directory layout is like:
   - bin\
      - start-queryengine.sh
   - lib\
      - .jar
      - .jar
   For now, manually create the directory /etc/dashboard/conf, and copy the following files:
    - queryengine-logback.xml
    - queryengine-version.info
    - queryengine.xml
     
  