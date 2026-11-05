package io.student.rangiffler.data.db;

import io.student.rangiffler.config.Config;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * The two schemas the tests touch. Each constant owns its connection and transaction manager, so a DAO names the
 * database it works with and never mixes them by accident.
 */
public enum Databases {
  AUTH(Config.authDbUrl()),
  API(Config.apiDbUrl());

  private final DataSource dataSource;
  private final PlatformTransactionManager transactionManager;

  Databases(String url) {
    this.dataSource = new DriverManagerDataSource(url, Config.dbUser(), Config.dbPassword());
    this.transactionManager = new DataSourceTransactionManager(dataSource);
  }

  public DataSource dataSource() {
    return dataSource;
  }

  public PlatformTransactionManager transactionManager() {
    return transactionManager;
  }
}
