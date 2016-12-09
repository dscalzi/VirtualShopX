package com.dscalzi.virtualshop.connection;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.dscalzi.virtualshop.managers.ChatManager;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool.PoolInitializationException;

public class SQLiteWrapper extends ConnectionWrapper{

	private final String name;
	private final String location;
	
	private boolean initialized;
	
	public SQLiteWrapper(String name, String location){
		super();
		this.name = name;
		this.location  =  location;
		this.initialized = false;
	}
	
	public boolean initialize(){
		if(initialized) return true;
		initialized = true;
		ChatManager.getInstance().logInfo("Using SQLite, attempting to establish connection..");
		try {
			Class.forName("org.sqlite.JDBC");
			config.setDriverClassName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e){
			ChatManager.getInstance().logError("SQLite Driver not found. Shutting down!", true);
			return false;
		}
		
		File folder = new File(location);
		if(!folder.exists()) folder.mkdir();
		
		File sqlFile = new File(folder.getAbsolutePath() + File.separator + name + ".db");
		
		config.setJdbcUrl("jdbc:sqlite:" + sqlFile.toPath().toString());
		config.setMaximumPoolSize(1);
		
		try{
			ds = new HikariDataSource(config);
		} catch (PoolInitializationException e){
			ChatManager.getInstance().logError("Could not establish connection to SQLite.", true);
			ChatManager.getInstance().logError("Check your settings or submit a ticket, shutting down..", true);
			return false;
		}
		
		ChatManager.getInstance().logInfo("Successfully connected to SQLite.");
		return true;
	}

	@Override
	public boolean checkTable(String table) {
		try(Connection connection = ds.getConnection()){
			DatabaseMetaData dbm = connection.getMetaData();
			try(ResultSet tables = dbm.getTables(null, null, table, null)){
				if (tables.next())
				  return true;
				else
				  return false;
			}
		} catch (SQLException e) {
			ChatManager.getInstance().logError("Failed to check if table \"" + table + "\" exists: " + e.getMessage(), true);
			return false;
		}
	}

}
