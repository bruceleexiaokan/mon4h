package mon4h.common.os;

public class OS {

	private final static String os = System.getProperty("os.name")
			.toLowerCase();

	public static boolean isWindows() {
		return (os.indexOf("win") >= 0);
	}

	public static boolean isMac() {
		return (os.indexOf("mac") >= 0);
	}

	public static boolean isUnix() {
		return (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0 || os
				.indexOf("aix") > 0);
	}

	public static boolean isSolaris() {
		return (os.indexOf("sunos") >= 0);
	}

}
