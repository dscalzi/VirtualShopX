package org.blockface.virtualshop.persistance;

import lib.PatPeter.SQLibrary.MySQL;
import org.blockface.virtualshop.Chatty;
import org.blockface.virtualshop.managers.ConfigManager;

import java.sql.ResultSet;

public class MySQLDB implements Database
{
    private MySQL db;

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

    public ResultSet query(String query) {
        try {
            return db.query(query);
        } catch (Exception e) {
            reconnect();
            return query(query);
        }
    }

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
		if(!db.checkTable("stock"))
		{
			String query = "create table stock(`id` integer primary key auto_increment,`damage` integer,`seller` varchar(80) not null,`item` integer not null, `price` double not null,`amount` integer not null)";
			db.createTable(query);
            Chatty.logInfo("Created stock table.");
		}
		if(!db.checkTable("transactions"))
		{
			String query = "create table transactions(`id` integer primary key auto_increment,`damage` integer not null, `buyer` varchar(20) not null,`seller` varchar(20) not null,`item` integer not null, `cost` double not null,`amount` integer not null)";
			db.createTable(query);
			Chatty.logInfo("Created transaction table.");
		}
		if(!db.checkTable("toggles")){
			String query = "create table toggles(`id` integer primary key auto_increment,`merchant` varchar(80) not null,`buyconfirm` bit not null,`sellconfirm` bit not null)";
			db.createTable(query);
			Chatty.logInfo("Created toggles table.");
		}
	}


}
