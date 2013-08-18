package mon4h.framework.dashboard.persist.util;


import mon4h.framework.dashboard.DashboardAbstractTest;
import mon4h.framework.dashboard.persist.util.DBUtil;

import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * User: huang_jie
 * Date: 7/5/13
 * Time: 2:38 PM
 */
public class DBUtilTest extends DashboardAbstractTest{
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testGetConnection() throws Exception {
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            con = DBUtil.getConnection();
            st = con.createStatement();
            rs = st.executeQuery("select 2 from dual");
            if (rs.next()) {
                assert 2 == rs.getInt(1);
            }
        } finally {
            DBUtil.close(con);
            DBUtil.close(null, st, rs);
        }
    }
}
