package mon4h.framework.dashboard;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;

import mon4h.framework.dashboard.command.ui.GetUiMetaStoreDataCommand;
import mon4h.framework.dashboard.common.io.InputAdapter;
import mon4h.framework.dashboard.common.io.OutputAdapter;

import org.junit.Test;


public class GetUiMetaStoreDataCommandTest {

	InputAdapter inputAdapter;
	OutputAdapter outputAdapter;
	
	HttpServletRequest request;
	
	@Test
	public void run() throws UnsupportedEncodingException {
		MockInputAdapter inputAdapter = new MockInputAdapter();
		InputStream input = new ByteArrayInputStream(
				"{\"version\":\"1\",\"namespace\":\"space\",\"name\":\"name\"}".getBytes("ISO-8859-1"));
		inputAdapter.setInputStream(input);
		GetUiMetaStoreDataCommand command = new GetUiMetaStoreDataCommand(inputAdapter,outputAdapter);
		command.execute();
	}
}
