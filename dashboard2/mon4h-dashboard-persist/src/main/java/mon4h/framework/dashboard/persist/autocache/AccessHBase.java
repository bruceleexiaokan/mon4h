package mon4h.framework.dashboard.persist.autocache;

import java.util.TreeMap;

public class AccessHBase {

    public static AccessHBase accesshbase = new AccessHBase();

    private AccessHBase() {
    }

    public static AccessHBase getAccessHBase() {
        return accesshbase;
    }

    public TreeMap<byte[], byte[]> getData( String namespace, int mid, int start, int end ) {
        return HBaseImpl.getHBaseController().findStreamSpans(namespace,mid,start,end);
    }

}
