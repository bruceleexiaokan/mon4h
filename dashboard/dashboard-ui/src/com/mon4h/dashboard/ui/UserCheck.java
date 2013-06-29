package com.mon4h.dashboard.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.log4j.Logger;
import org.json.JSONTokener;

import com.highcharts.export.util.Util;
import com.highcharts.export.util.Util.GetLastDataResponse;
import com.highcharts.export.util.Util.GetMetricnameResponse;
import com.mon4h.dashboard.ui.Config;

public class UserCheck {
	
	private static final String REQUEST_METHOD_POST = "POST";
	private static final String CONTENT_TYPE_MULTIPART = "multipart/";
	protected static Logger logger = Logger.getLogger("exportservlet");
	
	public void processrequest(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {

		try {
			String uri = request.getRequestURI();
			String url = request.getServletContext().getContextPath();
			String path = request.getServletContext().getRealPath(uri.substring(url.length()));
			String content = "";
			String backContent = "";
			StringBuilder writelist = new StringBuilder();
			StringBuilder readlist = new StringBuilder();
			StringBuilder lastdata = new StringBuilder();
			
			String flag = request.getParameter("flag");
			if( flag == null || flag.length() == 0 ) {
				
				content = Config.getReplacedContentFromFile(path,"#Check_Namespace_In_Tsdb","");
			} else if( flag.equals("checknamespace") ) {
				
	 			String[] namespaces = request.getParameterMap().get("namespace");
				String[] metricnames = request.getParameterMap().get("metricname");
				if( metricnames == null || metricnames.length == 0 || metricnames[0].length() == 0 ) {
					content = Config.getReplacedContentFromFile(path,"#Check_Namespace_In_Tsdb","request failure");
				} else {
					
					String getDashboardMetricnameUrl = Config.getCacheConf("[[query_engine_server/]/]") + Util.metricsUrl;
					String getMetricTagsData = Util.metricParam_namespace;
					int result = -1;
					String namespace = "";
					if( namespaces != null && namespaces.length != 0 && namespaces[0].length() != 0 ) {
						getMetricTagsData += namespaces[0];
						result = RegisterConfig.checkNamespace(namespaces[0]);
						namespace = namespaces[0];
						logger.error(namespace);
					} else {
						getMetricTagsData += "null";
						namespace = "null";
					}
					if( result < 0 ) {
						content = Config.getReplacedContentFromFile(path,"#Check_Namespace_In_Tsdb","namespace is wrong.");
					} else {
						getMetricTagsData += Util.metricParam_metricname + metricnames[0] + Util.metricParam_metricnameEnd;
						String getResult = Util.HttpGet(getDashboardMetricnameUrl,getMetricTagsData,Util.callBack_end);
						if( getResult.length() == 0 ) {
							content = Config.getReplacedContentFromFile(path,"#Check_Namespace_In_Tsdb","server failure, check your data and retry");						
						} else {
							String rs = getResult.substring((getResult.indexOf("(")+1),getResult.lastIndexOf(")"));
							GetMetricnameResponse gr = null;
							try {
								gr = Util.GetMetricnameResponse.parse(new JSONTokener(rs));
							} catch ( Exception e ) {
								gr = null;
								logger.error("Get Metrics Name Error, Parse Json Error.");
							} finally {
								if( gr == null ) {
									content = Config.getReplacedContentFromFile(path,"#Check_Namespace_In_Tsdb","server failure, check your data and retry");
								} else if ( gr.tags.size() > 1 ) {
									content = Config.getReplacedContentFromFile(path,"#Check_Namespace_In_Tsdb","namespace or metrics name is wrong.");
								} else {
									Set<String> tags = gr.tags.get(0);
									String replace = "Metric Tags: ";
									if( tags != null && tags.size() != 0 ) {
										for( String t : tags ) {
											replace += t + " ,";
										}
									}
									replace = replace.substring(0, replace.length()-1)+".";
									
									backContent = "<br/><font size=\"4\">can't get data, please check </font><font size=\"4\" color=\"red\">write privilege IP</font><font>list. </font>";
									backContent += "<form action=\"usercheck.html?flag=readwritelist&metricname=" + metricnames[0] + 
											"&namespaceid=" + result + "&namespace=" + namespace;
									backContent += "\" name=\"readwritelistform\" method=\"post\">";
									backContent += "<input type=\"submit\" name=\"readwriterightlist\" value=\"read write-privilege list\"></input>";
									backContent += "</form>";
									
									content = Config.getReplacedContentFromFile(path,"#Check_Namespace_In_Tsdb",replace);
								}
							}
						}
					}
				}
			} else if( flag.equals("readwritelist") ) {
				
				content = Config.getReplacedContentFromFile(path,"#Check_Namespace_In_Tsdb","");
				String namespaceid = request.getParameter("namespaceid");
				if( namespaceid == null || namespaceid.length() == 0 || namespaceid.equals("-1") ) {
					content = content.replaceAll("#Check_Write_Right_List", "failure please check namespace and metrics name");
				} else {
					String iplist = "";
					writelist.append("</br><font size=\"4\" color=\"red\">write privilege list: </font><font size=\"4\" color=\"blue\">");
					Set<String> set = RegisterConfig.readWriteRightList(Integer.valueOf(namespaceid));
					for( String str : set ) {
						iplist += str + " ,";
					}
					writelist.append(iplist.substring(0, iplist.length()-1));
					writelist.append("</font>");
					
					String namespace = request.getParameter("namespace");
					String metricname = request.getParameter("metricname");
					if( namespace != null && metricname != null && namespace.length() != 0 && metricname.length() != 0 ) {
						writelist.append("<br/><font size=\"4\">can't get data, please check</font><font size=\"4\" color=\"red\">read previlege IP</font><font>list. </font>");
						writelist.append("<form action=\"usercheck.html?flag=readreadlist&namespaceid=" + namespaceid + 
								"&namespace=" + namespace + "&metricname=" + metricname);
						writelist.append("\" name=\"readreadlistform\" method=\"post\">");
						writelist.append("<input type=\"submit\" name=\"readreadrightlist\" value=\"read read-privilege list\"></input>");
						writelist.append("</form>");
					}
				}
			} else if( flag.equals("readreadlist") ) {
				
				content = Config.getReplacedContentFromFile(path,"#Check_Namespace_In_Tsdb","");
				String namespaceid = request.getParameter("namespaceid");
				if( namespaceid == null || namespaceid.length() == 0 || namespaceid.equals("-1") ) {
					content = content.replaceAll("#Check_Write_Right_List", "failure, please check namespace and metrics name");
				} else {			
					String iplist = "";
					readlist.append("</br><font size=\"4\" color=\"red\">read privilege list: </font><font size=\"4\" color=\"blue\">");
					Set<String> set = RegisterConfig.readReadRightList(Integer.valueOf(namespaceid));
					for( String str : set ) {
						iplist += str + " ,";
					}
					readlist.append(iplist.substring(0, iplist.length()-1));
					readlist.append("</font>");
					
					String namespace = request.getParameter("namespace");
					String metricname = request.getParameter("metricname");
					if( namespace != null && metricname != null && namespace.length() != 0 && metricname.length() != 0 ) {
						readlist.append("<br/><font size=\"4\">can't get information? check the time for the last data point(at most one hour).</font>");
						readlist.append("<form action=\"usercheck.html?flag=readlastdata&namespaceid=" + namespaceid + 
								"&namespace=" + namespace + "&metricname=" + metricname);
						readlist.append("\" name=\"readlastdataform\" method=\"post\" onsubmit=\"return lastDataSend();\">");
						readlist.append("Start Time: &nbsp <input id=\"time_start\" name=\"time_start\" type=\"text\" size=\"21\" " +
								"maxlength=\"19\" onclick=\"calendar.show(this);\" />" +
								"End time &nbsp <input id=\"time_end\" name=\"time_end\" type=\"text\" size=\"21\" maxlength=\"19\" " +
								"onclick=\"calendar.show(this);\" />");
						readlist.append("<input type=\"submit\" name=\"readlastdata\" value=\"get time and data of last data point\"></input>");
						readlist.append("</form>");
					}
				}
			} else if( flag.equals("readlastdata") ) {
				
				content = Config.getReplacedContentFromFile(path,"#Check_Namespace_In_Tsdb","");
				String namespace = request.getParameter("namespace");
				String metricname = request.getParameter("metricname");
				if( namespace == null || namespace.length() == 0 ||
					metricname == null || metricname.length() == 0 ) {
					lastdata.append("<font color=\"red\">fails, please check namespace and metricname.</font>");
				} else {
					String starttime = null, endtime = null;
					String[] starttimes = request.getParameterMap().get("time_start");
					String[] endtimes = request.getParameterMap().get("time_end");
					if( starttimes == null || starttimes[0].length() == 0 ||
						endtimes == null || endtimes[0].length() == 0 ) {
						long l = System.currentTimeMillis()/1000;
						endtime = Long.toString(l);
						starttime = Long.toString(l-60*60);
					}
					if( starttime == null || endtime == null ) {
						starttime = starttimes[0];
						endtime = endtimes[0];
					}
	
					String getLastDataUrl = Config.getCacheConf("[[query_engine_server/]/]") + Util.lastdataUrl;
					String getLastData = Util.metricParam2_namespace;
					getLastData += namespace + Util.metricParam2_starttime + starttime + Util.metricParam2_endTime + endtime
							+ Util.metricParam2_metricname + metricname + Util.metricParam2_metricnameEnd;
					String getResult = Util.HttpGet(getLastDataUrl,getLastData,Util.callBack_end);
					
					if( getResult.length() == 0 ) {
						lastdata.append("<font color=\"red\">no data returned</font>");
					} else {
						String rs = getResult.substring((getResult.indexOf("(")+1),getResult.lastIndexOf(")"));
						GetLastDataResponse gl = null;
						try {
							gl = Util.GetLastDataResponse.parse(new JSONTokener(rs));
						} catch ( Exception e ) {
							gl = null;
							logger.error("Get Metrics Name Error, Parse Json Error.");
						} finally {
							if( gl == null ) {
								lastdata.append("<font color=\"red\">failure, please check input and retry</font>");
							} else {
								lastdata.append("<a>Namespace: </a><font color=\"red\">" + namespace + "</font>");
								lastdata.append("<a> MetricName: </a><font color=\"red\">" + metricname + "</font><br/>");
								if( gl.getLastTime().length() != 0 ) {
									lastdata.append("<font>time and data of last data sent: </font><font color=\"red\"> time=" + gl.getLastTime());
								} else {
									lastdata.append("<font>time and data of last data sent: </font><font color=\"red\"> time is null");
								}
								if( gl.getLastData().length() != 0 ) {
									lastdata.append("  ||  number = " + gl.getLastData() + "</font>");
								} else {
									lastdata.append("  ||  number is null</font>");
								}
							}
						}
					}
				}
			} 
			
			content = content.replaceAll("#REQUEST_TIME_NOW", Util.TimeNow());
			content = content.replaceAll("#Check_Back_Content", backContent);
			content = content.replaceAll("#Check_Write_Right_List", writelist.toString());
			content = content.replaceAll("#Check_Read_Right_List", readlist.toString());
			content = content.replaceAll("#Check_Last_Input_Data", lastdata.toString());
			if( content == null || content.length() == 0 ) {
				throw new Exception("File Read Error!");
			}
			byte[] result = content.getBytes("UTF-8");
			response.reset();
			response.setContentLength(result.length);
			response.setCharacterEncoding("UTF-8");
			response.setContentType("text/html");
			ServletOutputStream out = response.getOutputStream();
			out.write(result);
			out.flush();

		} catch (IOException ioe) {
			logger.error("Oops something happened here redirect to error-page, "
					+ ioe.getMessage());
			sendError(request, response, ioe);
		} catch ( ServletException sce) {
			logger.error("Oops something happened here redirect to error-page, "
					+ sce.getMessage());
			sendError(request, response, sce);
		} catch (Exception e) {
			logger.error("Oops something happened here redirect to error-page, "
					+ e.getMessage());
			sendError(request, response, e);
		}
		
	}
	
	private String getParameter(HttpServletRequest request, String name,
			Boolean multi) throws IOException, ServletException {
		if (multi && request.getPart(name) != null) {
			return getValue(request.getPart(name));
		} else {
			return request.getParameter(name);
		}
	}
	
	private static String getValue(Part part) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				part.getInputStream(), "UTF-8"));
		StringBuilder value = new StringBuilder();
		char[] buffer = new char[1024];
		for (int length = 0; (length = reader.read(buffer)) > 0;) {
			value.append(buffer, 0, length);
		}
		return value.toString();
	}

	public static final boolean isMultipartRequest(HttpServletRequest request) {
		// inspired by org.apache.commons.fileupload
		logger.debug("content-type " + request.getContentType());
		return REQUEST_METHOD_POST.equalsIgnoreCase(request.getMethod())
				&& request.getContentType() != null
				&& request.getContentType().toLowerCase()
						.startsWith(CONTENT_TYPE_MULTIPART);
	}

	protected void sendError(HttpServletRequest request,
			HttpServletResponse response, Throwable ex) throws IOException,
			ServletException {
		String headers = null;
		String htmlHeader = "<HTML><HEAD><TITLE>Highcharts Export error</TITLE><style type=\"text/css\">"
				+ "body {font-family: \"Trebuchet MS\", Arial, Helvetica, sans-serif;} table {border-collapse: collapse;}th {background-color:green;color:white;} td, th {border: 1px solid #98BF21;} </style></HEAD><BODY>";
		String htmlFooter = "</BODY></HTML>";

		response.setContentType("text/html");

		PrintWriter out = response.getWriter();
		Enumeration<String> e = request.getHeaderNames();
		String svg = this.getParameter(request, "svg",
				isMultipartRequest(request));

		out.println(htmlHeader);
		out.println("<h3>Error while converting SVG</h3>");
		out.println("<h4>Error message</h4>");
		out.println("<p>" + ex.getMessage() + "</p>");
		out.println("<h4>Debug steps</h4><ol>"
				+ "<li>Copy the SVG:<br/><textarea cols=100 rows=5>"
				+ svg
				+ "</textarea></li>"
				+ "<li>Go to <a href='http://validator.w3.org/#validate_by_input' target='_blank'>validator.w3.org/#validate_by_input</a></li>"
				+ "<li>Paste the SVG</li>"
				+ "<li>Click More Options and select SVG 1.1 for Use Doctype</li>"
				+ "<li>Click the Check button</li></ol>");

		out.println("<h4>Request Headers</h4>");
		out.println("<TABLE>");
		out.println("<tr><th> Header </th><th> Value </th>");

		while (e.hasMoreElements()) {
			headers = (String) e.nextElement();
			if (headers != null) {
				out.println("<tr><td align=center><b>" + headers + "</td>");
				out.println("<td align=center>" + request.getHeader(headers)
						+ "</td></tr>");
			}
		}
		out.println("</TABLE><BR>");
		out.println(htmlFooter);

	}
}
