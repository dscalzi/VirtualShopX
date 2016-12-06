package com.dscalzi.virtualshop.connection;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.dscalzi.virtualshop.managers.ChatManager;

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
			ds.setDriverClassName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e){
			ChatManager.getInstance().logError("SQLite Driver not found. Shutting down!", true);
			return false;
		}
		
		File folder = new File(location);
		if(!folder.exists()) folder.mkdir();
		
		File sqlFile = new File(folder.getAbsolutePath() + File.separator + name + ".db");
		
		ds.setUrl("jdbc:sqlite:" + sqlFile.toPath().toString());
		ds.setMaxIdle(0);
		
		boolean connectionSuccessful;
		
		//Test connection
		try(Connection connection = ds.getConnection();
			PreparedStatement stmt = connection.prepareStatement("SELECT 1"))
		{
			try(ResultSet result = stmt.executeQuery()){
				connectionSuccessful = true;
				ChatManager.getInstance().logInfo("Successfully connected to SQLite.");
			}
		} catch (SQLException e) {
			connectionSuccessful = false;
			ChatManager.getInstance().logError("Could not establish connection to SQLite: " + e.getMessage(), true);
			ChatManager.getInstance().logError("Check your settings or submit a ticket, shutting down..", true);
		}
		
		return connectionSuccessful;
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
