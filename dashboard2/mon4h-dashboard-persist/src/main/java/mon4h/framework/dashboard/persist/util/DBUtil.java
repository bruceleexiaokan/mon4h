package mon4h.framework.dashboard.persist.util;


import mon4h.framework.dashboard.common.config.ConfigConstant;
import mon4h.framework.dashboard.common.config.Configure;
import mon4h.framework.dashboard.common.util.ConfigUtil;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * User: huang_jie
 * Date: 7/5/13
 * Time: 1:22 PM
 */
public class DBUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(DBUtil.class);
    private DataSource dataSource;

    private static class ConnectionPoolHolder {
        private static DBUtil instance = new DBUtil();
    }

    private static DBUtil get() {
        return ConnectionPoolHolder.instance;
    }

    public static Connection getConnection() throws SQLException {
        return get().dataSource.getConnection();
    }

    public static void close(Connection conn) {
        close(conn, null, null);
    }

    public static void close(Connection conn, Statement statement, ResultSet resultSet) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                LOGGER.warn("Close db connection error: ", e);
            }
        }

        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                LOGGER.warn("Close db statement error: ", e);
            }
        }
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                LOGGER.warn("Close db resultSet error: ", e);
            }
        }
    }

    private DBUtil() {
        PoolProperties prop;
        try {
            prop = loadConfigProp();
            dataSource = new DataSource();
            dataSource.setPoolProperties(prop);
        } catch (Exception e) {
            throw new RuntimeException("Cannot initial connection pool, ", e);
        }
    }

    private PoolProperties loadConfigProp() throws Exception {
        Configure configure = ConfigUtil.getConfigure(ConfigConstant.CONFIG_KEY_DB);

        PoolProperties prop = new PoolProperties();
        prop.setUrl(configure.getString("db-config/jdbc-connection-pool/url"));
        prop.setDriverClassName(configure.getString("db-config/jdbc-connection-pool/driverClassName"));
        prop.setUsername(configure.getString("db-config/jdbc-connection-pool/username"));
        prop.setPassword(configure.getString("db-config/jdbc-connection-pool/password"));
        prop.setJmxEnabled(configure.getBoolean("db-config/jdbc-connection-pool/jmxEnabled", true));
        prop.setTestWhileIdle(configure.getBoolean("db-config/jdbc-connection-pool/testWhileIdle", false));
        prop.setTestOnBorrow(configure.getBoolean("db-config/jdbc-connection-pool/testOnBorrow", true));
        prop.setValidationQuery(configure.getString("db-config/jdbc-connection-pool/validationQuery", "SELECT 1"));
        prop.setTestOnReturn(configure.getBoolean("db-config/jdbc-connection-pool/testOnReturn", false));
        prop.setValidationInterval(configure.getInt("db-config/jdbc-connection-pool/validationInterval", 30000));
        prop.setTimeBetweenEvictionRunsMillis(configure.getInt("db-config/jdbc-connection-pool/timeBetweenEvictionRunsMillis", 30000));
        prop.setMaxActive(configure.getInt("db-config/jdbc-connection-pool/maxActive", 100));
        prop.setInitialSize(configure.getInt("db-config/jdbc-connection-pool/initialSize", 10));
        prop.setMaxWait(configure.getInt("db-config/jdbc-connection-pool/maxWait", 10000));
        prop.setRemoveAbandonedTimeout(configure.getInt("db-config/jdbc-connection-pool/removeAbandonedTimeout", 60));
        prop.setMinEvictableIdleTimeMillis(configure.getInt("db-config/jdbc-connection-pool/minEvictableIdleTimeMillis", 30000));
        prop.setMinIdle(configure.getInt("db-config/jdbc-connection-pool/minIdle", 10));
        prop.setMaxIdle(configure.getInt("db-config/jdbc-connection-pool/maxIdle", 30));
        prop.setLogAbandoned(configure.getBoolean("db-config/jdbc-connection-pool/logAbandoned", true));
        prop.setRemoveAbandoned(configure.getBoolean("db-config/jdbc-connection-pool/removeAbandoned", true));
        return prop;
    }

}
