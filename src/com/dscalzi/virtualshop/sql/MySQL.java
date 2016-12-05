package com.dscalzi.virtualshop.sql;

import java.net.MalformedURLException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Marked for removal after overhaul is complete
 *
 */
@Deprecated
public class MySQL extends DatabaseHandler {
	
	private final String hostname;
	private final int port;
	private final String username;
	private final String password;
	private final String schema;
	
	public MySQL(String hostname, int portnmbr, String schema, String username, String password) {
		this.hostname = hostname;
		this.port = portnmbr;
		this.schema = schema;
		this.username = username;
		this.password = password;
	}
	
	@Override
	protected boolean initialize() {
		try {
			Class.forName("com.mysql.jdbc.Driver"); // Check that server's Java has MySQL support.
			return true;
	    } catch (ClassNotFoundException e) {
	    	super.cm.logError("Class Not Found Exception: " + e.getMessage() + ".", true);
	    	return false;
	    }
	}
	
	@Override
	public Connection open() throws MalformedURLException, InstantiationException, IllegalAccessException {
		if (initialize()) {
			String url = "";
		    try {
				url = "jdbc:mysql://" + this.hostname + ":" + this.port + "/" + this.schema;
				this.connection = DriverManager.getConnection(url, this.username, this.password);
				return this.connection;
		    } catch (SQLException e) {
		    	super.cm.logError(url,true);
		    	super.cm.logError("Could not be resolved because of an SQL Exception: " + e.getMessage() + ".", true);
		    }
		}
		return this.connection;
	}
	
	@Override
	public void close() {
		try {
			if (this.connection != null)
				this.connection.close();
		} catch (Exception e) {
			super.cm.logError("Failed to close database connection: " + e.getMessage(), true);
		}
	}
	
	@Override
	public Connection getConnection()
	throws MalformedURLException, InstantiationException, IllegalAccessException {
		if (this.connection == null)
			return open();
		return this.connection;
	}
	
	@Override
	public boolean checkConnection() {
		if (this.connection == null) {
			try {
				open();
				return true;
			} catch (MalformedURLException ex) {
				super.cm.logError("MalformedURLException: " + ex.getMessage(), true);
			} catch (InstantiationException ex) {
				super.cm.logError("InstantiationExceptioon: " + ex.getMessage(), true);
			} catch (IllegalAccessException ex) {
				super.cm.logError("IllegalAccessException: " + ex.getMessage(), true);
			}
			return false;
		}
		return true;
	}
	
	@Override
	public ResultSet query(String query)
	throws MalformedURLException, InstantiationException, IllegalAccessException {
		//Connection connection = null;
		Statement statement = null;
		ResultSet result = null;
		try {
			//connection = getConnection();
			this.connection = this.getConnection();
		    statement = this.connection.createStatement();
		    
		    switch (this.getStatement(query)) {
			    case SELECT:
				    result = statement.executeQuery(query);
				    return result;
			    
			    default:
			    	statement.executeUpdate(query);
			    	return result;
		    }
		} catch (SQLException ex) {
			super.cm.logError("Error in SQL query: " + ex.getMessage(), false);
		}
		return null;
	}
	
	@Override
	public boolean createTable(String query) {
		Statement statement = null;
		try {
			this.connection = this.getConnection();
			if (query.equals("") || query == null) {
				super.cm.logError("SQL query empty: createTable(" + query + ")", true);
				return false;
			}
		    
			statement = this.connection.createStatement();
		    statement.execute(query);
		    return true;
		} catch (SQLException e) {
			super.cm.logError(e.getMessage(), true);
			return false;
		} catch (Exception e) {
			super.cm.logError(e.getMessage(), true);
			return false;
		}
	}
	
	@Override
	public boolean checkTable(String table) throws MalformedURLException, InstantiationException, IllegalAccessException {
		try {
			//Connection connection = getConnection();
			this.connection = this.getConnection();
		    Statement statement = this.connection.createStatement();
		    
		    ResultSet result = statement.executeQuery("SELECT * FROM " + table);
		    
		    if (result == null)
		    	return false;
		    if (result != null)
		    	return true;
		} catch (SQLException e) {
			if (e.getMessage().contains("exist")) {
				return false;
			} else {
				super.cm.logError("Error in SQL query: " + e.getMessage(), false);
			}
		}
		
		
		if (query("SELECT * FROM " + table) == null) return true;
		return false;
	}
	
	@Override
	public boolean wipeTable(String table) throws MalformedURLException, InstantiationException, IllegalAccessException {
		//Connection connection = null;
		Statement statement = null;
		String query = null;
		try {
			if (!this.checkTable(table)) {
				super.cm.logError("Error wiping table: \"" + table + "\" does not exist.", true);
				return false;
			}
			//connection = getConnection();
			this.connection = this.getConnection();
		    statement = this.connection.createStatement();
		    query = "DELETE FROM " + table + ";";
		    statement.executeUpdate(query);
		    
		    return true;
		} catch (SQLException e) {
			if (!e.toString().contains("not return ResultSet"))
				return false;
		}
		return false;
	}
}