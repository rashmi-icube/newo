package org.icube.owen.helper;

import java.sql.Connection;

import org.apache.tomcat.jdbc.pool.DataSource;

public class CompanyConnection {

	private Connection neoConnection;
	private DataSource sqlDataSource;

	public Connection getNeoConnection() {
		return neoConnection;
	}

	public void setNeoConnection(Connection neoConnection) {
		this.neoConnection = neoConnection;
	}

	public DataSource getDataSource() {
		return sqlDataSource;
	}

	public void setDataSource(DataSource sqlDataSource) {
		this.sqlDataSource = sqlDataSource;
	}

}
