package com.dscalzi.virtualshop.persistance;

import lib.PatPeter.SQLibrary.*;

import java.sql.ResultSet;

import com.dscalzi.virtualshop.Chatty;

public class SQLiteDB implements Database
{
    private SQLite db;

    public void load() throws Exception
    {
        db = new SQLite(Chatty.getLogger(), Chatty.getPrefix(), "VirtualShop", "plugins/VirtualShop/");
		db.open();
		if(!db.checkConnection())
		{
			Chatty.logInfo("FlatFile creation failed!");
			throw new Exception("FlatFile creation failed.");
		}
		Chatty.logInfo("Using flat files.");
		checkTables();

    }

    private void checkTables()
	{
		if(!this.db.checkTable("stock"))
		{
			String query = "create table stock('id' integer primary key,'damage' integer,'seller' varchar(80) not null,'item' integer not null, 'price' double not null,'amount' integer not null)";
			db.createTable(query);
			Chatty.logInfo("Created stock table.");
		}
		if(!this.db.checkTable("transactions"))
		{
			String query = "create table transactions('id' integer primary key,'damage' integer not null,'buyer' varchar(80) not null,'seller' varchar(80) not null,'item' integer not null, 'cost' double not null,'amount' integer not null)";
			db.createTable(query);
			Chatty.logInfo("Created transaction table.");
		}
		if(!db.checkTable("toggles")){
			String query = "create table toggles('id' integer primary key,'merchant' varchar(80) not null,'buyconfirm' bit not null,'sellconfirm' bit not null)";
			db.createTable(query);
			Chatty.logInfo("Created toggles table.");
		}
	}

    public ResultSet query(String query)
	{
		return db.query(query);
	}

    public void unload() {
        db.close();
    }

}
