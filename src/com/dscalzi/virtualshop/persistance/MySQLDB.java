package com.dscalzi.virtualshop.persistance;

import lib.PatPeter.SQLibrary.MySQL;

import java.sql.ResultSet;

import com.dscalzi.virtualshop.Chatty;
import com.dscalzi.virtualshop.managers.ConfigManager;

public class MySQLDB implements com.dscalzi.virtualshop.persistance.Database
{
    private MySQL db;

    @Override
    public void load() throws Exception
    {
        Chatty.logInfo("Using MySQL.");
        db = new MySQL(Chatty.getLogger(), Chatty.getPrefix(), ConfigManager.mySQLHost(), ConfigManager.getPort().toString(), ConfigManager.mySQLdatabase(), ConfigManager.mySQLUserName(), ConfigManager.mySQLPassword());
        db.open();
        if(db.checkConnection())
        {
            Chatty.logInfo("Successfully connected to MySQL Database");
            checkTables();
            return;
        }
        Chatty.logInfo("Could not connect to MySQL Database. Check settings.");
    }

    @Override
    public ResultSet query(String query) {
        try {
        	if(!db.checkConnection())
        		load();
            return db.query(query);
        } catch (Exception e) {
            reconnect();
            return query(query);
        }
    }

    @Override
    public void unload() {
        db.close();
    }


    private void reconnect() {
        try {
            db.open();
        } catch (Exception e) {
            Chatty.logInfo("Your database has gone offline, please switch to SQLite for stability.");
        }
    }


    private void checkTables() throws Exception
	{
		if(!db.checkTable("vshop_stock"))
		{
			String query = "create table vshop_stock(`id` integer primary key auto_increment,`damage` integer,`seller` varchar(80) not null,`item` integer not null, `price` double not null,`amount` integer not null)";
			db.createTable(query);
            Chatty.logInfo("Created vshop_stock table.");
		}
		if(!db.checkTable("vshop_transactions"))
		{
			String query = "create table vshop_transactions(`id` integer primary key auto_increment,`damage` integer not null, `buyer` varchar(20) not null,`seller` varchar(20) not null,`item` integer not null, `cost` double not null,`amount` integer not null)";
			db.createTable(query);
			Chatty.logInfo("Created vshop_transaction table.");
		}
		if(!db.checkTable("vshop_toggles")){
			String query = "create table vshop_toggles(`id` integer primary key auto_increment,`merchant` varchar(80) not null,`buyconfirm` bit not null,`sellconfirm` bit not null)";
			db.createTable(query);
			Chatty.logInfo("Created vshop_toggles table.");
		}
	}


}
