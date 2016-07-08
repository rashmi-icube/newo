package org.icube.owen.helper;

import java.sql.Connection;

public class CompanyConnection {

	private Connection sqlConnection;
	private Connection neoConnection;

	public Connection getSqlConnection() {
		return sqlConnection;
	}

	public void setSqlConnection(Connection sqlConnection) {
		this.sqlConnection = sqlConnection;
	}

	public Connection getNeoConnection() {
		return neoConnection;
	}

	public void setNeoConnection(Connection neoConnection) {
		this.neoConnection = neoConnection;
	}

}
