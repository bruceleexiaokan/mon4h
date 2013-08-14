1. Table schema:
create 'DASHBOARD_METRICS_NAME', {NAME => 'm', BLOOMFILTER => 'ROW', VERSIONS => '1'};
create 'DASHBOARD_TIME_SERIES', {NAME => 'm', BLOOMFILTER => 'ROW', VERSIONS => '1'}
create 'DASHBOARD_TS_DATA', {NAME => 'm', BLOOMFILTER => 'ROW', VERSIONS => '1', TTL => '2592000'}


2. Configuration:
The configuration files are in mon4h-dashboard-assembly/src/main/resources/environment/{env}/{*.xml}

3. Query engine uses leveldb as cache implementation. Please note that only windows/linux platform is tested. 


