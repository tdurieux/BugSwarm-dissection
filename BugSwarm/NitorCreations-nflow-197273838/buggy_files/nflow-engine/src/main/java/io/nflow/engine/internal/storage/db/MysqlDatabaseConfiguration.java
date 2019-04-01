package io.nflow.engine.internal.storage.db;

import static io.nflow.engine.internal.config.Profiles.MYSQL;
import static java.lang.Integer.parseInt;
import static org.apache.commons.lang3.StringUtils.split;
import static org.slf4j.LoggerFactory.getLogger;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceUtils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.internal.config.NFlow;
import io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;

@Profile(MYSQL)
@Configuration
public class MysqlDatabaseConfiguration extends DatabaseConfiguration {
  private static final Logger logger = getLogger(MysqlDatabaseConfiguration.class);

  public MysqlDatabaseConfiguration() {
    super("mysql");
  }

  @Bean
  @Override
  @SuppressFBWarnings(value = { "CLI_CONSTANT_LIST_INDEX", "WEM_WEAK_EXCEPTION_MESSAGING" }, //
      justification = "extracting major and minor version from splitted string, exception message is ok")
  public DatabaseInitializer nflowDatabaseInitializer(@NFlow DataSource nflowDataSource, Environment env) {
    String dbType = "mysql";
    try (Connection c = DataSourceUtils.getConnection(nflowDataSource)) {
      DatabaseMetaData meta = c.getMetaData();
      String databaseProductVersion = meta.getDatabaseProductVersion();
      int majorVersion = meta.getDatabaseMajorVersion();
      int minorVersion = meta.getDatabaseMinorVersion();
      logger.info("MySQL {}.{}, product version {}", majorVersion, minorVersion, databaseProductVersion);
      if (databaseProductVersion.contains("MariaDB")) {
        if (databaseProductVersion.startsWith("5.5.5-")) {
          databaseProductVersion = databaseProductVersion.substring(6);
        }
        String[] versions = split(databaseProductVersion, ".-");
        if (parseInt(versions[0]) <= 5 && parseInt(versions[1]) <= 5) {
          dbType += ".legacy";
        }
      } else if (majorVersion <= 5 && minorVersion <= 5) {
        dbType += ".legacy";
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to obtain mysql version", e);
    }
    return new DatabaseInitializer(dbType, nflowDataSource, env);
  }

  @Bean
  @Override
  public SQLVariants sqlVariants() {
    return new MySQLVariants();
  }

  public static class MySQLVariants implements SQLVariants {
    @Override
    public String currentTimePlusSeconds(int seconds) {
      return "date_add(current_timestamp, interval " + seconds + " second)";
    }

    @Override
    public boolean hasUpdateReturning() {
      return false;
    }

    @Override
    public boolean hasUpdateableCTE() {
      return false;
    }

    @Override
    public String nextActivationUpdate() {
      return "(case "
          + "when ? is null then null "
          + "when external_next_activation is null then ? "
          + "else least(?, external_next_activation) end)";
    }

    @Override
    public String workflowStatus(WorkflowInstanceStatus status) {
      return "'" + status.name() + "'";
    }

    @Override
    public String workflowStatus() {
      return "?";
    }

    @Override
    public String actionType() {
      return "?";
    }

    @Override
    public String castToText() {
      return "";
    }

    @Override
    public String limit(String query, String limit) {
      return query + " limit " + limit;
    }

    @Override
    public int longTextType() {
      return Types.VARCHAR;
    }

    @Override
    public boolean useBatchUpdate() {
      return true;
    }
  }
}
