package mon4h.framework.dashboard.common.task;

/**
 * User: huang_jie
 * Date: 7/9/13
 * Time: 1:32 PM
 */
public class DemoTask implements Runnable {
    public static boolean flag = false;

    @Override
    public void run() {
        flag = true;
    }
}
