This project is user interface of mon4h dashboard framework. Through this UI, users have the following features:
- view metrics in different types of chart, for example line or bar chart. 
- specify a time range to scan data points
- do OLAP operations, for example slice by specifying tag value, aggregate data points by specifying a few different 
  tags
- support downsampling, for example, sum/min/max/avg of data points belonging to the same time series
- support aggregation, for example, sum/min/max/avg of data points belonging to different time series

Dependent library management:
 - No maven support, will support it in the future
 
Build:
 - import this project in eclipse IDE (the source includes .project and .classpath)
 - make sure this project's facet is dynamic web project
 - make sure this project's binary target is WebContent/WEB-INF/classes
 - the dependent library is in WebContent/WEB-INF/lib (Need to change it to maven)
 
Config:
 - in windows environment, the configuration is now hardcoded to D:/dashboard/conf/commonui.ini
   in linux environment, the configuration is now hardcoded to /etc/dashboard/conf/commonui.ini
   will make it configurable

Debug and Run in eclipse:
 - Install tomcat 7
 - run the project on the tomcat server

Run:
 - copy all files in WebContent directory to tomcat webapps directory
 - configure commonui.ini
