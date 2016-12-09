package com.dscalzi.virtualshop.connection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.dscalzi.virtualshop.managers.ChatManager;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool.PoolInitializationException;

public class MySQLWrapper extends ConnectionWrapper{

	private final String host;
	private final int port;
	private final String schema;
	private final String user;
	private final String pass;
	
	private boolean initialized;
	
	public MySQLWrapper(String host, int port, String schema, String user, String pass){
		super();
		this.host = host;
		this.port = port;
		this.schema = schema;
		this.user = user;
		this.pass = pass;
		this.initialized = false;
	}
	
	public boolean initialize(){
		if(this.initialized) return true;
		initialized = true;
		ChatManager.getInstance().logInfo("Using MySQL, attempting to establish connection..");
		try {
			Class.forName("com.mysql.jdbc.Driver");
			config.setDriverClassName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e){
			ChatManager.getInstance().logError("MySQL Driver not found. Shutting down..", true);
			return false;
		}
    	
		config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + schema);
		config.setUsername(user);
		config.setPassword(pass);
		config.setIdleTimeout(1500);
		config.setMaximumPoolSize(5);
		
		try{
			ds = new HikariDataSource(config);
		} catch (PoolInitializationException e){
			ChatManager.getInstance().logError("Could not establish connection to MySQL Server.", true);
			ChatManager.getInstance().logError("Check your settings or use SQLite, shutting down..", true);
			return false;
		}
		
		ChatManager.getInstance().logInfo("Successfully connected to MySQL server.");
		return true;
	}

	@Override
	public boolean checkTable(String table) {
		String sql = "SELECT * FROM " + table;
		try(Connection connection = ds.getConnection();
			PreparedStatement statement = connection.prepareStatement(sql))
		{ 
		    try(ResultSet result = statement.executeQuery()){
		    	if(result == null) return false;
		    	else return true;
		    }
		} catch (SQLException e) {
			if (e.getMessage().contains("exist")) {
				return false;
			} else {
				ChatManager.getInstance().logError("Error in SQL query: " + e.getMessage(), false);
			}
		}
    	return false;
	}
	
}
