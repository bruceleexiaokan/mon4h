package mon4h.framework.dashboard.command.meta;

import java.util.List;

/**
 * User: huang_jie
 * Date: 7/18/13
 * Time: 11:21 AM
 */
public class GetNamespaceResponse extends AbstractMetaCommandResponse {

    protected GetNamespaceResponse(String key, List<String> values) {
        super(key, values);
    }
}
