package com.dscalzi.virtualshop.connection;

import java.sql.SQLException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public abstract class ConnectionWrapper {

	protected HikariDataSource ds;
	protected HikariConfig config;
	
	public ConnectionWrapper(){
		this.config = new HikariConfig();
	}
	
	public HikariDataSource getDataSource(){
		return ds;
	}
	
	public void terminate() throws SQLException {
		if(ds != null)
			ds.close();
	}
	
	public abstract boolean initialize();
	
	public abstract boolean checkTable(String table);
}
