/*
 * VirtualShop
 * Copyright (C) 2015-2017 Daniel D. Scalzi
 * See LICENSE.txt for license information.
 */
package com.dscalzi.virtualshop.connection;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.dscalzi.virtualshop.managers.MessageManager;
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
	
	@Override
	public boolean initialize(){
		if(initialized) return true;
		initialized = true;
		MessageManager.getInstance().logInfo("Using SQLite, attempting to establish connection..");
		try {
			Class.forName("org.sqlite.JDBC");
			config.setDriverClassName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e){
			MessageManager.getInstance().logError("SQLite Driver not found. Shutting down!", true);
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
			MessageManager.getInstance().logError("Could not establish connection to SQLite.", true);
			MessageManager.getInstance().logError("Check your settings or submit a ticket, shutting down..", true);
			return false;
		}
		
		MessageManager.getInstance().logInfo("Successfully connected to SQLite.");
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
			MessageManager.getInstance().logError("Failed to check if table \"" + table + "\" exists: " + e.getMessage(), true);
			return false;
		}
	}

}
