package mon4h.collector.resources;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class CollectorExceptionMapper implements ExceptionMapper<Exception>  {
    private static final Logger LOGGER = LoggerFactory.getLogger(CollectorExceptionMapper.class);

    @Override
    public Response toResponse(Exception throwable) {
        Response.Status resStatus = Response.Status.INTERNAL_SERVER_ERROR;
        String resStr;

        resStr = getStackTrace(throwable);

        LOGGER.warn("There was an exception and converted it as a http code:" + resStatus, throwable);

        return Response.status(resStatus)
                .type(MediaType.TEXT_PLAIN_TYPE)
                .entity(resStr).build();
    }

    private String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString(); // stack trace as a string
    }

}
