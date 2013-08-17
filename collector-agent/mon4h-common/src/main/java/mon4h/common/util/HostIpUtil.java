package mon4h.common.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class HostIpUtil {
	private static final String hostName;
	private static final String hostIp;
	
	static {
		String tmpHostName = "Unknown";
		String tmpHostIp = "0.0.0.0";
		try {
			boolean assigned = false;
			Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
			while (allNetInterfaces.hasMoreElements()) {
				NetworkInterface netInterface = (NetworkInterface)allNetInterfaces.nextElement();
				Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
				while(addresses.hasMoreElements()) {
					InetAddress ip = (InetAddress) addresses.nextElement();
					if (ip != null && ip instanceof Inet4Address && !ip.isLoopbackAddress()) {
						tmpHostName = ip.getHostName();
						tmpHostIp = ip.getHostAddress();
						assigned = true;
						break;
					}
				}
			}
			if (!assigned) {
			}
 		} catch (SocketException e) {
 			// do nothing
		}
		hostName = tmpHostName;
		hostIp = tmpHostIp;
	}
	
	public static final String getHostName() {
		return hostName;
	}
	
	public static final String getHostIp() {
		return hostIp;
	}

}
