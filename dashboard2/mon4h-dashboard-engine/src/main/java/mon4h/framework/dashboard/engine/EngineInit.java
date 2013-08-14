package mon4h.framework.dashboard.engine;

import java.io.IOException;

import mon4h.framework.dashboard.command.InterfaceConst;
import mon4h.framework.dashboard.common.config.ConfigConstant;
import mon4h.framework.dashboard.common.io.InitListener;
import mon4h.framework.dashboard.common.util.ConfigUtil;
import mon4h.framework.dashboard.persist.id.LocalCacheIDS;


public class EngineInit implements InitListener{

	@Override
	public void init() {
		try {
            ConfigUtil.addResource(ConfigConstant.CONFIG_KEY_DB,"dashboard-db-config.xml");
            ConfigUtil.addResource(ConfigConstant.CONFIG_KEY_TASK,"dashboard-engine-task.xml");
            ConfigUtil.addResource(ConfigConstant.CONFIG_KEY_CACHE,"dashboard-engine-cache.xml");

            LocalCacheIDS.getInstance().scheduleCacheTask();

			setSupportedCommandInfo();
			Engine.getInstance().start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void setSupportedCommandInfo(){
		InterfaceConst.putSupportedCommandVersion(InterfaceConst.Commands.GET_DATA_POINTS, 1);
        InterfaceConst.putSupportedCommandVersion(InterfaceConst.Commands.GET_GROUPED_DATA_POINTS, 1);
		InterfaceConst.putSupportedCommandVersion(InterfaceConst.Commands.GET_GROUPED_DATA_POINTS, 2);
		InterfaceConst.putSupportedCommandVersion(InterfaceConst.Commands.GET_METRICS_TAGS, 1);
		InterfaceConst.putSupportedCommandVersion(InterfaceConst.Commands.PUT_DATA_POINTS, 1);
		InterfaceConst.putSupportedCommandVersion(InterfaceConst.Commands.SYSTEM_STATUS, 1);
		InterfaceConst.putSupportedCommandVersion(InterfaceConst.Commands.GET_RAW_DATA, 1);
		InterfaceConst.putSupportedCommandVersion(InterfaceConst.Commands.GET_META, 1);
		InterfaceConst.putSupportedCommandVersion(InterfaceConst.Commands.GET_STORE, 1);
		InterfaceConst.putSupportedCommandVersion(InterfaceConst.Commands.PUT_STORE, 1);
	}

}
