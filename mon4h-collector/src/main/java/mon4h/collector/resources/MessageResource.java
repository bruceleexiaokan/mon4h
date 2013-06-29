package mon4h.collector.resources;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import mon4h.collector.configuration.Constants;

@Path("messages")
public class MessageResource {

	
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public void addMessage(InputStream input, @HeaderParam("Content-Length") final int length, @HeaderParam("MessageType") final String type) throws IOException  {
    	if (length > Constants.MAX_MESSAGE_SIZE) {
    		throw new IOException("The message size exceeds the max message size limit (" + Constants.MAX_MESSAGE_SIZE + "), size = " + length);
    	}
    }

}
