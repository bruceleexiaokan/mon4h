package org.ctrip.hbase.conf;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;

import java.io.File;

/**
 * Split Runner
 *
 * @author: yafengli
 * @date: 13-2-25
 */
public class HBaseConfig {
    static Logger log = Logger.getLogger(HBaseConfig.class);
    static Object lock = new Object();
    private static HBaseConfig HBaseConfigInstance = null;
    private Configuration SpliterConfiguration = null;
    private String SpliterConf = null;

    private HBaseConfig() {
        String spliterHome = System.getenv("SPLITER_HOME");
        if (spliterHome == null) {
            spliterHome = ".";
        }

        if (!spliterHome.endsWith("/")) {
            spliterHome = spliterHome + File.separator;
        }

        SpliterConf = System.getenv("SPLITER_CONF_DIR");
        if (SpliterConf == null) {
            SpliterConf = spliterHome + "conf" + File.separator;
        }

        log.info("Spliter configuration is using " + SpliterConf);

        SpliterConfiguration = new Configuration();
        SpliterConfiguration.addResource(new Path(SpliterConf + "/hbase-site.xml"));
    }

    public static HBaseConfig Instance() {
        if (HBaseConfigInstance == null) {
            synchronized(lock){
                if (HBaseConfigInstance == null)  {
                    HBaseConfigInstance = new HBaseConfig();
                }
            }
        }

        return HBaseConfigInstance;
    }

    public Configuration getConfiguration() {
        return SpliterConfiguration;
    }
}
