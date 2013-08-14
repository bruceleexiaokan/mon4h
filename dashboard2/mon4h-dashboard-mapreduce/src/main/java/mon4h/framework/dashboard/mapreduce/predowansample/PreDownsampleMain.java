package mon4h.framework.dashboard.mapreduce.predowansample;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PreDownsampleMain extends Configured implements Tool {
	
	private static final Logger log = LoggerFactory.getLogger(PreDownsampleMain.class);
//	private static final ILog log = LogManager.getLogger(PreDownsampleMain.class);

	public static void main(String[] args) throws Exception {

		Configuration config = new Configuration();
		int res = ToolRunner.run(config, new PreDownsampleMain(), args);
		System.exit(res);
	}
    
	@Override
	public int run(String[] arg0) throws Exception {
		try {
    		PreDownsampleInit.initConf();
    		PreDownsampleInit.doMapReduce();
    	} catch ( Exception e ) {
    		e.printStackTrace();
    		log.error(e.getMessage());
    	}
		return 0;
	}
}
