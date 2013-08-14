package mon4h.framework.dashboard.io.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import javax.servlet.AsyncContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import mon4h.framework.dashboard.common.CommandNames;
import mon4h.framework.dashboard.common.io.CommandProcessorProvider;
import mon4h.framework.dashboard.io.ServletInputAdapter;
import mon4h.framework.dashboard.io.ServletOutputAdapter;
import mon4h.framework.dashboard.io.context.Util;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@WebServlet(urlPatterns = {"/data/*"}, asyncSupported = true)
public class DataServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataServlet.class);
    private static final long serialVersionUID = -2152498153163223478L;

    @Override
    public void service(ServletRequest request, ServletResponse response) throws UnsupportedEncodingException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        ServletInputAdapter inputAdapter = new ServletInputAdapter(httpRequest);
        String httpMethod = httpRequest.getMethod();
        inputAdapter.setCommandType(CommandNames.GET_GROUPED_DATA_POINTS);
        if ("get".equalsIgnoreCase(httpMethod)) {
            byte[] json = buildJsonFromURI(httpRequest);
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


    private byte[] buildJsonFromURI(HttpServletRequest request) throws UnsupportedEncodingException {
        JSONStringer builder = new JSONStringer();
        builder.object();
        builder.key("version").value(2);
        builder.key("time-series-pattern");
        builder.object();
        builder.key("namespace").value(request.getParameter("namespace"));
        builder.key("metrics-name").value(request.getParameter("metric-name"));
        String tags = request.getParameter("tags");
        if (StringUtils.isNotBlank(tags)) {
            builder.key("tag-search-part");
            JSONTokener tokener = new JSONTokener(tags);
            JSONObject jsonObject = new JSONObject(tokener);
            builder.value(jsonObject);
        }
        builder.endObject();
        String groupBy = request.getParameter("group-by");
        if (StringUtils.isNotBlank(groupBy)) {
            builder.key("group-by");
            JSONTokener tokener = new JSONTokener(groupBy);
            JSONArray jsonObject = new JSONArray(tokener);
            builder.value(jsonObject);
        }
        String aggregator = request.getParameter("aggregator");
        if (aggregator != null) {
            builder.key("aggregator");
            builder.object();
            builder.key("function").value(aggregator);
            builder.endObject();
        }
        String downsampler = request.getParameter("downsampler");
        boolean isRate = false;
        if (downsampler != null) {
            builder.key("downsampler");
            builder.object();
            builder.key("interval").value(request.getParameter("interval"));
            if ("rat".equals(downsampler)) {
                isRate = true;
            }

            builder.key("function").value(downsampler);
            builder.endObject();
        }
        String maxCount = request.getParameter("max-datapoint-count");
        if (StringUtils.isBlank(maxCount)) {
            maxCount = "100";
        }
        builder.key("max-datapoint-count").value(maxCount);
        builder.key("start-time").value(request.getParameter("start-time"));
        builder.key("end-time").value(request.getParameter("end-time"));
        if (isRate == true) {
            builder.key("rate").value(isRate);
        }
        builder.endObject();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Dashboard data request json is: " + builder.toString());
        }
        return builder.toString().getBytes(Charset.forName("UTF-8"));
    }

}
