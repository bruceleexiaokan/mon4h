package mon4h.framework.dashboard;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;

import mon4h.framework.dashboard.command.ui.GetUiStoreDataCommand;
import mon4h.framework.dashboard.common.io.InputAdapter;
import mon4h.framework.dashboard.common.io.OutputAdapter;

import org.junit.Test;


public class GetUiStoreDataCommandTest {

	InputAdapter inputAdapter;
	OutputAdapter outputAdapter;
	
	HttpServletRequest request;
	
	@Test
	public void run() throws UnsupportedEncodingException {

        try {
            MockInputAdapter inputAdapter = new MockInputAdapter();
            InputStream input = new ByteArrayInputStream("{\"version\":\"1\"}".getBytes("ISO-8859-1"));
            inputAdapter.setInputStream(input);
            GetUiStoreDataCommand command = new GetUiStoreDataCommand(inputAdapter,outputAdapter);
            command.execute();
        } catch (Exception e) {
        }
    }
}
