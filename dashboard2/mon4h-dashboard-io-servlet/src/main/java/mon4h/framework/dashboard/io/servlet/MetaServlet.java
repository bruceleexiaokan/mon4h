package mon4h.framework.dashboard.io.servlet;


import mon4h.framework.dashboard.common.CommandNames;
import mon4h.framework.dashboard.common.io.CommandProcessorProvider;
import mon4h.framework.dashboard.common.util.StringUtil;
import mon4h.framework.dashboard.io.ServletInputAdapter;
import mon4h.framework.dashboard.io.ServletOutputAdapter;
import mon4h.framework.dashboard.io.context.Util;

import org.apache.commons.lang.StringUtils;
import org.json.JSONStringer;

import javax.servlet.AsyncContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

@WebServlet(urlPatterns = {"/meta/*"}, asyncSupported = true)
public class MetaServlet extends HttpServlet {

    private static final long serialVersionUID = -2152498153163223478L;

    @Override
    public void service(ServletRequest request, ServletResponse response) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        Map<String, String> params = new HashMap<String, String>();
        ServletInputAdapter inputAdapter = new ServletInputAdapter(httpRequest);
        String httpMethod = httpRequest.getMethod();
        String url = httpRequest.getRequestURI();
        String limitStr = request.getParameter("limit");
        if (StringUtils.isBlank(limitStr)) {
            limitStr = "25";
        }
        params.put("limit", limitStr);
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (url.endsWith("/meta") || url.endsWith("/meta/")) {
            return;
        }
        String clientIp = request.getRemoteAddr();
        params.put("clientIp", clientIp);
        String cmd = url.substring(url.lastIndexOf("/meta") + 6);
        inputAdapter.setCommandType(getCommand(cmd, params));
        if ("get".equalsIgnoreCase(httpMethod)) {
            byte[] json = buildJsonFromParam(params);
            ByteArrayInputStream bais = new ByteArrayInputStream(json);
            inputAdapter.setRequestInputStream(bais);
        } else if ("post".equalsIgnoreCase(httpMethod)
                || "put".equalsIgnoreCase(httpMethod)) {
            try {
                InputStream is = request.getInputStream();
                inputAdapter.setRequestInputStream(is);
            } catch (IOException e) {
                Util.responseError((HttpServletResponse) response, e);
            }
        } else {
            Util.responseError((HttpServletResponse) response, new IOException("unsupported http method:" + httpMethod));
        }
        AsyncContext context = request.startAsync();
        ServletOutputAdapter outputAdapter;
        try {
            outputAdapter = new ServletOutputAdapter(context);
            CommandProcessorProvider.getInstance().getCommandProcessor().processCommand(inputAdapter, outputAdapter);
        } catch (IOException e) {
            Util.responseError((HttpServletResponse) response, e);
        }
    }

    private byte[] buildJsonFromParam(Map<String, String> params) {
        JSONStringer builder = new JSONStringer();
        try {
            builder.object();
            for (String key : params.keySet()) {
                builder.key(key).value(params.get(key));
            }
            builder.endObject();
        } catch (Exception e) {
            throw new RuntimeException("Cannot parse params: ", e);
        }
        return builder.toString().getBytes(Charset.forName("UTF-8"));
    }

    private String getCommand(String cmd, Map<String, String> params) {
        String rt;
        String[] cmds = cmd.split("/");
        if (cmd.indexOf("/tagValues") > 0 && cmds.length >= 7) {
            rt = CommandNames.GET_TAG_VALUE;
            params.put("namespace", StringUtil.trimAndLowerCase(StringUtil.decode(cmds[1])));
            params.put("metricName", StringUtil.trimAndLowerCase(StringUtil.decode(cmds[3])));
            params.put("tagName", StringUtil.trimAndLowerCase(StringUtil.decode(cmds[5])));
            params.put("tagValue", cmds.length == 8 ? StringUtil.trimAndLowerCase(StringUtil.decode(cmds[7])) : "");
        } else if (cmd.indexOf("/tagNames") > 0 && cmds.length >= 5) {
            rt = CommandNames.GET_TAG_NAME;
            params.put("namespace", StringUtil.trimAndLowerCase(StringUtil.decode(cmds[1])));
            params.put("metricName", StringUtil.trimAndLowerCase(StringUtil.decode(cmds[3])));
            params.put("tagName", cmds.length == 6 ? StringUtil.trimAndLowerCase(StringUtil.decode(cmds[5])) : "");
        } else if (cmd.indexOf("/metrics") > 0 && cmds.length >= 3) {
            rt = CommandNames.GET_METRIC_NAME;
            params.put("namespace", StringUtil.trimAndLowerCase(StringUtil.decode(cmds[1])));
            params.put("metricName", cmds.length == 4 ? StringUtil.trimAndLowerCase(StringUtil.decode(cmds[3])) : "");
        } else if (cmd.startsWith("namespaces") && cmds.length >= 1) {
            rt = CommandNames.GET_NAMESPACE;
            params.put("namespace", cmds.length == 2 ? StringUtil.trimAndLowerCase(StringUtil.decode(cmds[1])) : null);
        } else {
            throw new RuntimeException("Not support this restful url.");
        }
        return rt;
    }
}
