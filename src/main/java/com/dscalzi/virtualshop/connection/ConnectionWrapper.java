/*
 * VirtualShop
 * Copyright (C) 2015-2017 Daniel D. Scalzi
 * See LICENSE.txt for license information.
 */
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
		if(ds != null){
			try {
				Class.forName("lib.com.zaxxer.hikari.pool.HikariPool$1");
			} catch (ClassNotFoundException e) {
				try {
					ConnectionWrapper.class.getClassLoader().loadClass("lib.com.zaxxer.hikari.pool.HikariPool$1");
					Class.forName("lib.com.zaxxer.hikari.pool.HikariPool$1");
				} catch (ClassNotFoundException e1) {
					e1.printStackTrace();
				}
				
			}
			ds.close();
		}
	}
	
	public abstract boolean initialize();
	
	public abstract boolean checkTable(String table);
}
