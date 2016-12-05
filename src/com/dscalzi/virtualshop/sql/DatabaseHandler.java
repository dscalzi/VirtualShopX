package com.dscalzi.virtualshop.sql;

import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.ResultSet;

import com.dscalzi.virtualshop.managers.ChatManager;

/**
 * Marked for removal after overhaul is complete
 *
 */
public abstract class DatabaseHandler {
	protected final ChatManager cm;
	protected Connection connection;
	protected enum Statements {
		SELECT, INSERT, UPDATE, DELETE, DO, REPLACE, LOAD, HANDLER, CALL, // Data manipulation statements
		CREATE, ALTER, DROP, TRUNCATE, RENAME  // Data definition statements
	}
	
	/*
	 *  MySQL, SQLLite
	 */
	public DatabaseHandler(){
		this.cm = ChatManager.getInstance();
		this.connection = null;
	}
	
	/**
	 * <b>initialize</b><br>
	 * <br>
	 * &nbsp;&nbsp;Used to check whether the class for the SQL engine is installed.
	 * <br>
	 * <br>
	 */
	abstract boolean initialize();
	
	/**
	 * <b>open</b><br>
	 * <br>
	 * &nbsp;&nbsp;Opens a connection with the database.
	 * <br>
	 * <br>
	 * @return the success of the method.
	 * @throws MalformedURLException - cannot access database because of a syntax error in the jdbc:// protocol.
	 * @throws InstantiationException - cannot instantiate an interface or abstract class.
	 * @throws IllegalAccessException - cannot access classes, fields, methods, or constructors that are private.
	 */
	abstract Connection open()
		throws MalformedURLException, InstantiationException, IllegalAccessException;
	
	/**
	 * <b>close</b><br>
	 * <br>
	 * &nbsp;&nbsp;Closes a connection with the database.
	 * <br>
	 * <br>
	 */
	abstract void close();
	
	/**
	 * <b>getConnection</b><br>
	 * <br>
	 * &nbsp;&nbsp;Gets the connection variable 
	 * <br>
	 * <br>
	 * @return the <a href="http://download.oracle.com/javase/6/docs/api/java/sql/Connection.html">Connection</a> variable.
	 * @throws MalformedURLException - cannot access database because of a syntax error in the jdbc:// protocol.
	 * @throws InstantiationException - cannot instantiate an interface or abstract class.
	 * @throws IllegalAccessException - cannot access classes, fields, methods, or constructors that are private.
	 */
	abstract Connection getConnection()
		throws MalformedURLException, InstantiationException, IllegalAccessException;
	
	/**
	 * <b>checkConnection</b><br>
	 * <br>
	 * Checks the connection between Java and the database engine.
	 * <br>
	 * <br>
	 * @return the status of the connection, true for up, false for down.
	 */
	abstract boolean checkConnection();
	
	/**
	 * <b>query</b><br>
	 * &nbsp;&nbsp;Sends a query to the SQL database.
	 * <br>
	 * <br>
	 * @param query - the SQL query to send to the database.
	 * @return the table of results from the query.
	 * @throws MalformedURLException - cannot access database because of a syntax error in the jdbc:// protocol.
	 * @throws InstantiationException - cannot instantiate an interface or abstract class.
	 * @throws IllegalAccessException - cannot access classes, fields, methods, or constructors that are private.
	 */
	abstract ResultSet query(String query)
		throws MalformedURLException, InstantiationException, IllegalAccessException;
	
	/**
	 * <b>getStatement</b><br>
	 * 
	 * <br>
	 * <br>
	 */
	protected Statements getStatement(String query) {
		String trimmedQuery = query.trim();
		if (trimmedQuery.substring(0,6).equalsIgnoreCase("SELECT"))
			return Statements.SELECT;
		else if (trimmedQuery.substring(0,6).equalsIgnoreCase("INSERT"))
			return Statements.INSERT;
		else if (trimmedQuery.substring(0,6).equalsIgnoreCase("UPDATE"))
			return Statements.UPDATE;
		else if (trimmedQuery.substring(0,6).equalsIgnoreCase("DELETE"))
			return Statements.DELETE;
		else if (trimmedQuery.substring(0,6).equalsIgnoreCase("CREATE"))
			return Statements.CREATE;
		else if (trimmedQuery.substring(0,5).equalsIgnoreCase("ALTER"))
			return Statements.ALTER;
		else if (trimmedQuery.substring(0,4).equalsIgnoreCase("DROP"))
			return Statements.DROP;
		else if (trimmedQuery.substring(0,8).equalsIgnoreCase("TRUNCATE"))
			return Statements.TRUNCATE;
		else if (trimmedQuery.substring(0,6).equalsIgnoreCase("RENAME"))
			return Statements.RENAME;
		else if (trimmedQuery.substring(0,2).equalsIgnoreCase("DO"))
			return Statements.DO;
		else if (trimmedQuery.substring(0,7).equalsIgnoreCase("REPLACE"))
			return Statements.REPLACE;
		else if (trimmedQuery.substring(0,4).equalsIgnoreCase("LOAD"))
			return Statements.LOAD;
		else if (trimmedQuery.substring(0,7).equalsIgnoreCase("HANDLER"))
			return Statements.HANDLER;
		else if (trimmedQuery.substring(0,4).equalsIgnoreCase("CALL"))
			return Statements.CALL;
		else
			return Statements.SELECT;
	}
	
	/**
	 * <b>createTable</b><br>
	 * <br>
	 * &nbsp;&nbsp;Creates a table in the database based on a specified query.
	 * <br>
	 * <br>
	 * @param query - the SQL query for creating a table.
	 * @return the success of the method.
	 */
	abstract boolean createTable(String query);
	
	/**
	 * <b>checkTable</b><br>
	 * <br>
	 * &nbsp;&nbsp;Checks a table in a database based on the table's name.
	 * <br>
	 * <br>
	 * @param table - name of the table to check.
	 * @return success of the method.
	 * @throws MalformedURLException - cannot access database because of a syntax error in the jdbc:// protocol.
	 * @throws InstantiationException - cannot instantiate an interface or abstract class.
	 * @throws IllegalAccessException - cannot access classes, fields, methods, or constructors that are private.
	 */
	abstract boolean checkTable(String table)
		throws MalformedURLException, InstantiationException, IllegalAccessException;
	
	/**
	 * <b>wipeTable</b><br>
	 * <br>
	 * &nbsp;&nbsp;Wipes a table given its name.
	 * <br>
	 * <br>
	 * @param table - name of the table to wipe.
	 * @return success of the method.
	 * @throws MalformedURLException - cannot access database because of a syntax error in the jdbc:// protocol.
	 * @throws InstantiationException - cannot instantiate an interface or abstract class.
	 * @throws IllegalAccessException - cannot access classes, fields, methods, or constructors that are private.
	 */
	abstract boolean wipeTable(String table)
		throws MalformedURLException, InstantiationException, IllegalAccessException;
}