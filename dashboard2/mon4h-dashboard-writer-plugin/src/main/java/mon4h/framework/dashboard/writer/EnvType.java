package mon4h.framework.dashboard.writer;

/**
 * User: huang_jie
 * Date: 8/5/13
 * Time: 9:23 AM
 */
public enum EnvType {
    DEV("DEV"), TEST("TEST"), UAT("UAT"), PROD("PROD");
    public final String env;

    private EnvType(String env) {
        this.env = env;
    }

}
