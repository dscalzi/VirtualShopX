package com.dscalzi.virtualshop.connection;

import java.sql.SQLException;
import java.util.MissingResourceException;

import org.apache.commons.dbcp2.BasicDataSource;

public abstract class ConnectionWrapper {

	protected BasicDataSource ds;
	
	public ConnectionWrapper(){
		try {
			this.ds = new BasicDataSource();
		} catch (MissingResourceException e){
			System.out.println("It happened again");
			//Son of a bitch.
		}
	}
	
	public BasicDataSource getDataSource(){
		return ds;
	}
	
	public void terminate() throws SQLException {
		ds.close();
	}
	
	public abstract boolean initialize();
	
	public abstract boolean checkTable(String table);
}
