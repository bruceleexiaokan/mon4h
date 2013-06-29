package com.mon4h.dashboard.ui;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.Enumeration;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.log4j.Logger;

import com.highcharts.export.util.Util;
import com.mon4h.dashboard.ui.Config;

public class ExportFile {	

	private static final String REQUEST_METHOD_POST = "POST";
	private static final String CONTENT_TYPE_MULTIPART = "multipart/";
	protected static Logger logger = Logger.getLogger("exportservlet");
	
	public void processrequest(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {

		// exportfile?reqdata={}&filename={}&callback={}&width={}&mime={}
		try {

			String reqdata = request.getParameter("reqdata");
			String filename = request.getParameter("filename");
			String reqtime = request.getParameter("reqtime");
			
			if( reqdata != null && reqdata.length() != 0 ) {
			
				filename = compareFile(filename);
				reqtime = compareTime(reqtime);
				StringBuilder cmd = new StringBuilder();
				String resultPath = "";
				if( System.getProperty("os.name").toUpperCase().indexOf("WIN") != -1 ) {
					resultPath = Util.WindowsSavePath + filename;
					cmd.append(Util.WindowsPath + " [[export_svg_server/]/]/dashboard-ui/common/dashboard-pic.html?flag=image&reqdata="
							+ URLEncoder.encode(reqdata, "ISO-8859-1") + " " + resultPath + " " + reqtime);
				} else {
					resultPath = Util.LinuxSavePath + filename;
					cmd.append(Util.LinuxPath + " [[export_svg_server/]/]/dashboard-ui/common/dashboard-pic.html?flag=image&reqdata="
							+ URLEncoder.encode(reqdata, "ISO-8859-1") + " " + resultPath + " " + reqtime);
				}
				
				String type = compareType(filename);
				byte[] result = null;
				try {
					Process process = Runtime.getRuntime().exec(Config.replace(cmd.toString()));
					process.waitFor();
					result = Util.readFile(resultPath);
					Util.delFile(resultPath);
				} catch( Exception e ) {
					logger.error(e.getMessage());
					return;
				}
				
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				stream.write(result);
				response.reset();
				response.setContentLength(stream.size());
				response.setCharacterEncoding("utf-8");
				response.setHeader("Content-disposition", "attachment; filename=" + filename );
				response.setHeader("Content-type", type);
				ServletOutputStream out = response.getOutputStream();
				out.write(stream.toByteArray());
				out.flush();
			}
			
		} catch (Exception e) {
			logger.error("Oops something happened here redirect to error-page, " + e.getMessage());
		}
	}
	
	private String compareType( String filename ) {
		String file = filename.toUpperCase();
		if( file.indexOf("PDF") != -1 ) {
			return "application/pdf";
		} else if( file.indexOf("PNG") != -1 ) {
			return "image/png";
		} else if( file.indexOf("JPEG") != -1 ) {
			return "image/jpeg";
		} else if( file.indexOf("SVG") != -1 ) {
			return "image/svg+xml";
		}
		return "image/png";
	}
	
	private String compareFile( String filename ) {
		
		if( filename == null || filename.length() == 0 ) {
			return "chart_" + System.currentTimeMillis() + ".png";
		}

		int pos = -1;
		if( (pos=filename.indexOf(".")) != -1 ) {
			return filename.substring(0,pos) + "_" + System.currentTimeMillis() + filename.substring(pos);
		}
		return filename + "_" + System.currentTimeMillis() + ".png";
	}
	
	private String compareTime( String time ) {
		
		if( time == null || time.length() == 0 ) {
			return "10000";
		}
		return time;
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
