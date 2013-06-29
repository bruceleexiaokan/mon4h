package com.mon4h.dashboard.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.log4j.Logger;

import com.highcharts.export.util.DESPlus;
import com.highcharts.export.util.Util;

public class UserIPAdd {
	
	private static final String REQUEST_METHOD_POST = "POST";
	private static final String CONTENT_TYPE_MULTIPART = "multipart/";
	protected static Logger logger = Logger.getLogger("exportservlet");

	public void processrequest(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {

		try {
			String uri = request.getRequestURI();
			
			String userid = "";
			StringBuilder login = new StringBuilder();
			StringBuilder loginresult = new StringBuilder();
			StringBuilder addwriteip = new StringBuilder();
			StringBuilder addreadip = new StringBuilder();
			StringBuilder addwrite = new StringBuilder();
			StringBuilder addread = new StringBuilder();
			
			String flag = request.getParameter("flag");
			if( flag != null && flag.equals("checklogin") ) {
				
				String[] usernames = request.getParameterMap().get("username");
				String[] passwords = request.getParameterMap().get("password");
				if( usernames == null || usernames.length == 0 || usernames[0].length() == 0 ) {
					appendlogin(login);
					loginresult.append("<font color=\"red\">Please input username.</font>");
				} else {
					if( passwords == null || passwords.length == 0 || passwords[0].length() == 0 ) {
						appendlogin(login);
						loginresult.append("<font color=\"red\">Please input password.</font>");
					} else {
						String username = usernames[0];
						String password = passwords[0];
						
						int result = RegisterConfig.checkUser(username,password);
						if( result == -1 ) {
							appendlogin(login);
							loginresult.append("<font color=\"red\">username or password is wrong</font>");
						} else {
							String id1 = DESPlus.DES.get().encrypt(username);
							String id2 = DESPlus.DES.get().encrypt(password);
							
							uri = uri.replace("useripadd.html", "useripadd.html?flag=login&id="+id1+"|"+id2);
							response.sendRedirect(uri);
							return;
						}
					}
				}
			} else if( flag != null && flag.equals("login") ) {
				
				userid = request.getParameter("id");
				if( checkUser(userid) == -1 ) {
					uri = uri.replace("useripadd.html", "useripadd.html?flag=redirct");
					response.sendRedirect(uri);
					return;
				}
				
				appendwrite(addwrite,userid);
				appendread(addread,userid);
			} else if( flag != null && flag.equals("redirct") ) {
				appendlogin(login);
				loginresult.append("<font color=\"red\">Account failure</font>");
			} else if( flag != null && flag.equals("addwriteip") ) {
				
				userid = request.getParameter("id");
				if( checkUser(userid) == -1 ) {
					uri = uri.replace("useripadd.html", "useripadd.html?flag=redirct");
					response.sendRedirect(uri);
					return;
				}
				
				appendwrite(addwrite,userid);
				appendread(addread,userid);
				
				String remoteIP = request.getRemoteAddr();
				if( remoteIP == null || remoteIP.length() == 0 ) {
					addwriteip.append("<font color=\"read\">You don't have access privilege.</font>");
				} else {
					String[] namespaces = request.getParameterMap().get("namespace_add_write");
					String[] writeips = request.getParameterMap().get("addwriteiptextarea");
					if( namespaces == null || namespaces.length == 0 || namespaces[0].length() == 0 ) {
						addwriteip.append("<font color=\"red\">Please input your namespace.</font>");
					} else {
						if( writeips == null || writeips.length == 0 || writeips[0].length() == 0 ) {
							addwriteip.append("<font color=\"red\">Please input your IP.</font>");
						} else {
							String namespace = namespaces[0];
							int result = RegisterConfig.checkNamespace(namespace);
							if( result == -1 ) {
								addwriteip.append("<font color=\"red\">This namespace has no access rights configured;</font>");
							}
							String writeip = writeips[0];
							String[] ips = writeip.split("\\|");
							for( String ip : ips ) {
								RegisterConfig.addWriteIPs(result,ip);
							}
							addwriteip.append("<font color=\"red\">Configuration is ok.</font>");
						}
					}
				}
			} else if( flag != null && flag.equals("addreadip") ) {
				
				userid = request.getParameter("id");
				if( checkUser(userid) == -1 ) {
					uri = uri.replace("useripadd.html", "useripadd.html?flag=redirct");
					response.sendRedirect(uri);
					return;
				}
				
				appendwrite(addwrite,userid);
				appendread(addread,userid);
				
				String remoteIP = request.getRemoteAddr();
				if( remoteIP == null || remoteIP.length() == 0 ) {
					addreadip.append("<font color=\"red\">You don't have access privilege.</font>");
				} else {
					String[] namespaces = request.getParameterMap().get("namespace_add_read");
					String[] readips = request.getParameterMap().get("addreadiptextarea");
					if( namespaces == null || namespaces.length == 0 || namespaces[0].length() == 0 ) {
						addreadip.append("<font color=\"red\">Please input namespace.</font>");
					} else {
						if( readips == null || readips.length == 0 || readips[0].length() == 0 ) {
							addreadip.append("<font color=\"red\">Please input IP</font>");
						} else {
							String namespace = namespaces[0];
							int result = RegisterConfig.checkNamespace(namespace);
							if( result == -1 ) {
								addreadip.append("<font color=\"red\">This namespace is not configured with privilege;");
							} else {
								String readip = readips[0];
								String[] ips = readip.split("\\|");
								for( String ip : ips ) {
									RegisterConfig.addReadIPs(result,ip);
								}
								addreadip.append("<font color=\"red\">Config read IP correct.</font>");
							}
						}
					}
				}
			} else {
				appendlogin(login);
			}
			
			String url = request.getServletContext().getContextPath();
			String path = request.getServletContext().getRealPath(uri.substring(url.length()));
			String content = Config.getReplacedContentFromFile(path,"","");
			content = content.replaceAll("#FIRST_LOGIN", login.toString());
			content = content.replaceAll("#LOGIN_RESULT", loginresult.toString());
			content = content.replaceAll("#RESULT_ADD_WRITE_IP", addwriteip.toString());
			content = content.replaceAll("#RESULT_ADD_READ_IP", addreadip.toString());
			content = content.replaceAll("#ADD_WRITE_IP", addwrite.toString());
			content = content.replaceAll("#ADD_READ_IP", addread.toString());
			content = content.replaceAll("#REQUEST_TIME_NOW", Util.TimeNow());
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
	
	private void appendlogin( StringBuilder login ) {
		
		login.append("<div class=\"check_namespace\">" + 
					"<form id=\"checknamepassword\" name=\"checknamepassword\" action=\"useripadd.html?flag=checklogin\" method=\"post\">" + 
					"<font size=\"5\">username:</font><input type=\"text\" id=\"username\" name=\"username\"></input></br>" + 
					"<font size=\"5\">password:&nbsp;&nbsp;&nbsp;&nbsp;</font><input id=\"password\" name=\"password\" type=\"password\"></input></br>" + 
					"<input type=\"submit\" name=\"checkloginbutton\" value=\"logon\"></input>" + 
					"</form>" + 
					"</div>");
	}
	
	private void appendwrite( StringBuilder addwrite,String id ) {
		
		addwrite.append("<div class=\"add_write_ip\">" +
				"<form id=\"addwriteip\" name=\"addwriteip\" action=\"useripadd.html?flag=addwriteip&id=" + id + "\" method=\"post\">" +
				"<font>input </font><font color=\"red\">Namespace</font><font>and</font><font color=\"red\">write</font><font>IP address(IP '|' to delimit): </font></br>" +
				"<input id=\"namespace_add_write\" name=\"namespace_add_write\" style=\"width:261px\"></input></br>" +
				"<textarea id=\"addwriteiptextarea\" name=\"addwriteiptextarea\" rows=\"5\" cols=\"30\"></textarea></br>" +
				"<input type=\"submit\" name=\"addwriteipbutton\" value=\"add write IP\"></input>" +
				"</form>" +
				"</div>");
	}
	
	private void appendread( StringBuilder addread,String id ) {
		
		addread.append("<div class=\"add_read_ip\">" +
				"<form id=\"addreadip\" name=\"addreadip\" action=\"useripadd.html?flag=addreadip&id=" + id + "\" method=\"post\">" +
				"<font>input </font><font color=\"red\">Namespace</font><font>and</font><font color=\"red\">read</font><font>IP address(IP '|' to delimit): </font></br>" +
				"<input id=\"namespace_add_read\" name=\"namespace_add_read\" style=\"width:261px\" ></input></br>" +
				"<textarea id=\"addreadiptextarea\" name=\"addreadiptextarea\" rows=\"5\" cols=\"30\"></textarea></br>" +
				"<input type=\"submit\" name=\"addreadipbutton\" value=\"add write ip\"></input>" +
				"</form>" +
				"</div>");
	}
	
	private int checkUser( String id ) {
		
		if( id == null || id.length() == 0 ) {
			return -1;
		}
		
		String[] ids = id.split("\\|");
		if( ids.length != 2 ) {
			return -1;
		}
		
		String username = "";
		try {
			username = DESPlus.DES.get().decrypt(ids[0]);
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
		String password = "";
		try {
			password = DESPlus.DES.get().decrypt(ids[1]);
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
		
		return RegisterConfig.checkUser(username,password);
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
