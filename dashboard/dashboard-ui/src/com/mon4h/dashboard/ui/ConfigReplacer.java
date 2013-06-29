package com.mon4h.dashboard.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.log4j.Logger;

import com.highcharts.export.util.Util;
import com.mon4h.dashboard.ui.Config;

@WebServlet(name = "ConfigReplacer", urlPatterns = { "*.js","*.html"})
public class ConfigReplacer extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	private static final String REQUEST_METHOD_POST = "POST";
	private static final String CONTENT_TYPE_MULTIPART = "multipart/";
	protected static Logger logger = Logger.getLogger("exportservlet");
	

	public ConfigReplacer() {
		super();
	}

	public void init() {		
	}

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		processrequest(request, response);
	}

	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		processrequest(request, response);
	}

	public void processrequest(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		
		try {
			String content = "";
			String uri = request.getRequestURI();
			String filename = uri.substring(uri.lastIndexOf("/")+1);
			String contentType = "";
			if( filename.endsWith("js") ) {
				contentType = "text/javascript";
				content = RegisterConfig.readJS(filename.substring(0,filename.length()-3));
				if( content != null ) {
					System.out.println("1 " + content.length());
				}
			} else if( filename.endsWith("html") ) {
				contentType = "text/html";
				if( filename.endsWith("dashboard-conf.html") ) {
					DashboardConf conf = new DashboardConf();
					conf.processrequest(request, response);
					return;
				} else if( filename.endsWith("dashboard.html") ) {
					Dashboard dc = new Dashboard();
					dc.processrequest(request, response);
					return;
				} else if( filename.endsWith("registeradd.html") ) {
					RegisterAdd ra = new RegisterAdd();
					ra.processrequest(request, response);
					return;
				} else if( filename.endsWith("registerdel.html") ) {
					RegisterDel rd = new RegisterDel();
					rd.processrequest(request, response);
					return;
				}else if( filename.endsWith("usercheck.html") ) {
					UserCheck cn = new UserCheck();
					cn.processrequest(request, response);
					return;
				} else if( filename.endsWith("useripadd.html") ) {
					UserIPAdd cn = new UserIPAdd();
					cn.processrequest(request, response);
					return;
				} else if( filename.endsWith("exportfile.html") ) {
					ExportFile ef = new ExportFile();
					ef.processrequest(request, response);
					return;
				} else if( filename.endsWith("dashboard-pic.html") ) {
					DashboardPic dp = new DashboardPic();
					dp.processrequest(request, response);
					return;
				}
			}
			
			if( content == null || content.length() == 0 ) {
				String url = request.getServletContext().getContextPath();
				String path = request.getServletContext().getRealPath(uri.substring(url.length()));
				content = Config.getReplacedContentFromFile(path,"#REQUEST_TIME_NOW", Util.TimeNow());
				System.out.println("2 " + content.length());
				if( content == null || content.length() == 0 ) {
					throw new Exception("File Read Error!");
				}
			}

			byte[] result = content.getBytes("UTF-8");
			response.reset();
			response.setContentLength(result.length);
			response.setCharacterEncoding("UTF-8");
			response.setContentType(contentType);
			ServletOutputStream out = response.getOutputStream();
			out.write(result);
			out.flush();

		} catch (IOException ioe) {
			logger.error("Oops something happened here redirect to error-page, "
					+ ioe.getMessage());
			System.out.println(ioe.getMessage());
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
