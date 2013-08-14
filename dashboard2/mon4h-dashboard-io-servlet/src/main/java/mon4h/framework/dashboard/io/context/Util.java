package mon4h.framework.dashboard.io.context;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

public class Util {
	public static void responseError(HttpServletResponse response,Exception e){
		
	}
	
	public static byte[] readFromStream(InputStream is) throws IOException{
		byte[] rt = null;
		byte[] buf = new byte[4096];
		int len = is.read(buf);
		while(len>0){
			if(rt == null){
				rt = new byte[len];
				System.arraycopy(buf, 0, rt, 0, len);
			}else{
				byte[] tmp = new byte[len+rt.length];
				System.arraycopy(rt, 0, tmp, 0, rt.length);
				System.arraycopy(buf, 0, tmp, rt.length, len);
				rt = tmp;
			}
			len = is.read(buf);
		}
		return rt;
	}
}
