/*
 * VirtualShop
 * Copyright (C) 2015-2017 Daniel D. Scalzi
 * See LICENSE.txt for license information.
 */
package com.dscalzi.virtualshop.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.inventory.ItemStack;
import com.dscalzi.virtualshop.VirtualShop;
import com.dscalzi.virtualshop.commands.Buy;
import com.dscalzi.virtualshop.commands.Cancel;
import com.dscalzi.virtualshop.commands.ESell;
import com.dscalzi.virtualshop.commands.Sell;
import com.dscalzi.virtualshop.connection.ConnectionWrapper;
import com.dscalzi.virtualshop.connection.MySQLWrapper;
import com.dscalzi.virtualshop.connection.SQLiteWrapper;
import com.dscalzi.virtualshop.commands.Reprice;
import com.dscalzi.virtualshop.objects.Confirmable;
import com.dscalzi.virtualshop.objects.Offer;
import com.dscalzi.virtualshop.objects.Transaction;
import javafx.util.Pair;

public final class DatabaseManager {

	private static final String DEFAULTNAME;
	private static final Map<Class<? extends Confirmable>, String> togglesKey;
	private static boolean initialized;
	private static DatabaseManager instance;
	static {
		DEFAULTNAME = "sync_required";
		togglesKey = new HashMap<Class<? extends Confirmable>, String>();
		togglesKey.put(Buy.class, "buyconfirm");
		togglesKey.put(Sell.class, "sellconfirm");
		togglesKey.put(ESell.class, "sellconfirm");
		togglesKey.put(Reprice.class, "repriceconfirm");
		togglesKey.put(Cancel.class, "cancelconfirm");
	}
	
	public enum ConnectionType{
		SQLite, MYSQL, VOID;
	}
	
	private ConnectionType type;
	
	@SuppressWarnings("unused")
	private VirtualShop plugin;
	private ConnectionWrapper ds;
	private ConfigManager configM;
	private MessageManager cm;
	private UUIDManager uuidm;
	
	private DatabaseManager(VirtualShop plugin){
		this.plugin = plugin;
		this.configM = ConfigManager.getInstance();
		this.cm = MessageManager.getInstance();
		this.uuidm = UUIDManager.getInstance();
		if(!this.setUp())
			this.type = ConnectionType.VOID;
	}
	
	private boolean setUp(){
		if(configM.usingMySQL()){
			if(!loadMySQL()) return false;
			this.type = ConnectionType.MYSQL;
		} else { 
			if(!loadSQLite()) return false;
			this.type = ConnectionType.SQLite;
		}
		return checkTables(this.type);
	}
	
	private boolean loadSQLite() {
		final String name = "VirtualShop";
		final String location = "plugins/VirtualShop/";
		
        this.ds = new SQLiteWrapper(name, location);
        return this.ds.initialize();
    }

    private boolean loadMySQL() {
    	this.ds = new MySQLWrapper(configM.mySQLHost(), configM.mySQLport(), 
    			configM.mySQLdatabase(), configM.mySQLUserName(), 
    			configM.mySQLPassword());
    	return this.ds.initialize();
    }
	
	public static void initialize(VirtualShop plugin){
		if(!initialized){
			instance = new DatabaseManager(plugin);
			initialized = true;
		}
	}
	
	public static boolean reload(){
		if(!initialized) return false;
		getInstance().setUp();
		return true;
	}
    
	public static DatabaseManager getInstance(){
		return DatabaseManager.instance;
	}
	
    public void terminate(){
        try {
        	if(ds != null)
        		this.ds.terminate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
    }
    
    public ConnectionType getConnectionType(){
    	return this.type;
    }
    
    private boolean checkTables(ConnectionType type){
    	int checksum = 0, desired = 0;
    	String autoIncrement = type == ConnectionType.SQLite ? "" : " auto_increment"; 
		if(!ds.checkTable("vshop_stock")){
			++desired;
			String query = "create table vshop_stock(`id` integer primary key" + autoIncrement + ",`damage` integer,`seller` varchar(80) not null,`item` integer not null, `price` double not null,`amount` integer not null, `uuid` varchar(80) not null)";
			if(createTable(query)){
				cm.logInfo("Successfully created table vshop_stock.");
				++checksum;
			} else
				cm.logError("Unable to create table vshop_stock.", true);
		}
		if(!ds.checkTable("vshop_estock")){
			++desired;
			String query = "create table vshop_estock(`id` integer primary key" + autoIncrement + ", `merchant` varchar(80) not null, `item` integer not null, `data` smallint not null, `price` double not null, `edata` varchar(255) not null, `uuid` varchar(80) not null)";
			if(createTable(query)){
				cm.logInfo("Successfully created table vshop_estock.");
				++checksum;
			} else
				cm.logError("Unable to create table vshop_stock.", true);
		}
		if(!ds.checkTable("vshop_transactions")){
			++desired;
			String query = "create table vshop_transactions(`id` integer primary key" + autoIncrement + ",`damage` integer not null, `buyer` varchar(20) not null,`seller` varchar(20) not null,`item` integer not null, `cost` double not null,`amount` integer not null, `buyer_uuid` varchar(80) not null, `seller_uuid` varchar(80) not null)";
			if(createTable(query)){
				cm.logInfo("Successfully created table vshop_transaction.");
				++checksum;
			} else
				cm.logError("Unable to create table vshop_transaction.", true);
		}
		if(!ds.checkTable("vshop_toggles")){
			++desired;
			String query = "create table vshop_toggles(`id` integer primary key" + autoIncrement + ",`merchant` varchar(80) not null,`buyconfirm` bit not null,`sellconfirm` bit not null, `cancelconfirm` bit not null, `repriceconfirm` bit not null, `uuid` varchar(80) not null)";
			if(createTable(query)){
				cm.logInfo("Successfully created table vshop_toggles.");
				++checksum;
			} else
				cm.logError("Unable to create table vshop_toggles.", true);
		}
		
		if(checksum != desired){
			cm.logError("Failed to create one or database tables, shutting down..", true);
			return false;
		}
		return true;
	}
    
	private boolean createTable(String query) {
		try(Connection connection = ds.getDataSource().getConnection();
			PreparedStatement stmt = connection.prepareStatement(query)){
			stmt.executeUpdate();
		    return true;
		} catch (SQLException e) {
			cm.logError(e.getMessage(), true);
			return false;
		}
	}
    
    /* Buy & Sell Confirmation Accessors */
    
    public int syncNameToUUID(){
    	Map<UUID, String> uuidNamePairs = new HashMap<UUID, String>();
    	
    	try(Connection connection = ds.getDataSource().getConnection();
    		Statement stmt = connection.createStatement()){
    		
	        try(ResultSet result = stmt.executeQuery("select * from vshop_toggles")){
				while(result.next())
					uuidNamePairs.put(UUID.fromString(result.getString("uuid")), result.getString("merchant"));
	        }
			
	    	//Remove all entries that have the same name. Update entries with new name.
	    	Iterator<Entry<UUID, String>> it = uuidNamePairs.entrySet().iterator();
	    	while(it.hasNext()){
	    		Entry<UUID, String> entry = it.next();
	    		Optional<String> newName = uuidm.getNewPlayerName(entry.getKey(), entry.getValue());
	    		if(newName.isPresent())
	    			entry.setValue(newName.get());
	    		else
	    			it.remove();
	    	}
	    	//Update data
	    	for(Map.Entry<UUID, String> entry : uuidNamePairs.entrySet()){
	    		//Update toggles table
	    		stmt.executeUpdate("update vshop_toggles set merchant='" + entry.getValue() + "' where uuid='" + entry.getKey().toString() + "'");
	    		//Update stock table
	    		stmt.executeUpdate("update vshop_stock set seller='" + entry.getValue() + "' where uuid='" + entry.getKey().toString() + "'");
	    		//Update transaction table
	    		stmt.executeUpdate("update vshop_transactions set buyer='" + entry.getValue() + "' where buyer_uuid='" + entry.getKey().toString() + "'");
	    		stmt.executeUpdate("update vshop_transactions set seller='" + entry.getValue() + "' where seller_uuid='" + entry.getKey().toString() + "'");
	    	}
	    	return uuidNamePairs.size();
    	} catch (SQLException e){
    		cm.logError(e.getMessage(), true);
    		return -1;
    	}
    }
    
    public Pair<Boolean, Integer> syncNameToUUID(UUID uuid){
    	String loggedName = "";
    	
    	try(Connection connection = ds.getDataSource().getConnection();
    		Statement stmt = connection.createStatement()){
    		try(ResultSet result = stmt.executeQuery("select * from vshop_toggles where uuid='" + uuid.toString() + "'")){
				if(!result.next()) return new Pair<Boolean, Integer>(false, 404);
				else loggedName = result.getString("merchant");
    		}
			
	    	Optional<String> newName = uuidm.getNewPlayerName(uuid, loggedName);
	    	if(newName.isPresent()){
	    		//Update toggles table
	    		stmt.executeUpdate("update vshop_toggles set merchant='" + newName.get() + "' where uuid='" + uuid.toString() + "'");
				//Update stock table
	    		stmt.executeUpdate("update vshop_stock set seller='" + newName.get() + "' where uuid='" + uuid.toString() + "'");
				//Update transaction table'
	    		stmt.executeUpdate("update vshop_transactions set buyer='" + newName.get() + "' where buyer_uuid='" + uuid.toString() + "'");
	    		stmt.executeUpdate("update vshop_transactions set seller='" + newName.get() + "' where seller_uuid='" + uuid.toString() + "'");
				return new Pair<Boolean, Integer>(true, 0);
	    	}
			return new Pair<Boolean, Integer>(false, 1);
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    		return new Pair<Boolean, Integer>(false, -1);
    	}
    }
    
    @Deprecated
    public Pair<Integer, Integer> updateDatabase(){
    	try(Connection connection = ds.getDataSource().getConnection();
    		Statement stmt = connection.createStatement())
    	{
	    	//Add columns
	    	stmt.executeUpdate("ALTER TABLE vshop_toggles  ADD `uuid` varchar(80)");
	    	stmt.executeUpdate("ALTER TABLE vshop_stock  ADD `uuid` varchar(80)");
	    	stmt.executeUpdate("ALTER TABLE vshop_transactions  ADD `buyer_uuid` varchar(80)");
	    	stmt.executeUpdate("ALTER TABLE vshop_transactions  ADD `seller_uuid` varchar(80)");
	    	
	    	List<String> names = new ArrayList<String>();
	    	try(ResultSet result = stmt.executeQuery("select * from vshop_toggles")){
				while(result.next())
					names.add(result.getString("merchant"));
	    	}
	    	
	    	int s = 0, f = 0;
	    	
	    	for(String n : names){
	    		Optional<UUID> uuidOpt = uuidm.getPlayerUUID(n);
	    		if(!uuidOpt.isPresent()) {
	    			++f;
	    			continue;
	    		}
	    		UUID uuid = uuidOpt.get();
	    		//Update toggles table
	    		stmt.executeUpdate("update vshop_toggles set uuid='" + uuid.toString() + "' where merchant='" + n + "'");
				//Update stock table
	    		stmt.executeUpdate("update vshop_stock set uuid='" + uuid.toString() + "' where seller='" + n + "'");
				//Update transaction table'
	    		stmt.executeUpdate("update vshop_transactions set buyer_uuid='" + uuid.toString() + "' where buyer='" + n + "'");
	    		stmt.executeUpdate("update vshop_transactions set seller_uuid='" + uuid.toString() + "' where seller='" + n + "'");
				++s;
	    	}
	    	
	    	//Purge invalids.. no mercy.
	    	stmt.executeUpdate("delete from vshop_stock where uuid = ''");
	    	stmt.executeUpdate("delete from vshop_toggles where uuid = ''");
	    	stmt.executeUpdate("delete from vshop_transactions where buyer_uuid = ''");
	    	stmt.executeUpdate("delete from vshop_transactions where seller_uuid = ''");
	    	
	    	return new Pair<Integer, Integer>(s,f);
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    		return new Pair<Integer, Integer>(-1,-1);
    	}
    } 
    
    public boolean isPlayerInToggles(UUID merchantUUID){
    	String sql = "select * from vshop_toggles where uuid='" + merchantUUID.toString() + "'";
    	try(Connection connection = ds.getDataSource().getConnection();
    		PreparedStatement stmt = connection.prepareStatement(sql))
    	{
	    	try(ResultSet result = stmt.executeQuery()){
		    	if(!result.next())
		    		return false;
		    	else
		    		if(result.getString("uuid").equalsIgnoreCase(merchantUUID.toString()))
		    			return true;
		    	return false;
	    	}
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    	}
    	return false;
    }
    
    public void addPlayerToToggles(UUID merchantUUID){
    	String merchant = DatabaseManager.DEFAULTNAME;
    	Optional<String> name = uuidm.getPlayerName(merchantUUID);
    	if(name.isPresent()) merchant = name.get();
    	String sql = "insert into vshop_toggles(merchant,buyconfirm,sellconfirm,cancelconfirm,repriceconfirm,uuid) values('" + merchant + "',1,1,1,1,'" + merchantUUID.toString() + "')";
    	executeUpdate(sql);
    }
    
    public void updateToggle(UUID merchantUUID, Class<? extends Confirmable> clazz, boolean value){
    	if(!togglesKey.containsKey(clazz))
    		return;
    	if(!isPlayerInToggles(merchantUUID))
    		addPlayerToToggles(merchantUUID);
    	
    	int dataval = value ? 1 : 0;
    	String sql = "update vshop_toggles set " + togglesKey.get(clazz) + "=" + dataval + " where uuid='" + merchantUUID.toString() + "'";
    	executeUpdate(sql);
    }
    
    public boolean getToggle(UUID merchantUUID, Class<? extends Confirmable> clazz){
    	if(!togglesKey.containsKey(clazz))
    		return false;
    	if(!isPlayerInToggles(merchantUUID))
    		addPlayerToToggles(merchantUUID);
    	
    	String sql = "select * from vshop_toggles where uuid='" + merchantUUID.toString() + "'";
    	try(Connection connection = ds.getDataSource().getConnection();
        	PreparedStatement stmt = connection.prepareStatement(sql);
        	ResultSet result = stmt.executeQuery()){
    		result.next();    		
    		return result.getBoolean(togglesKey.get(clazz));
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    		return true;
    	}
    }
    
    @SuppressWarnings("deprecation")
	public void addOffer(Offer offer){
    	String sql = "insert into vshop_stock(seller,item,amount,price,damage,uuid) values('" +offer.getSeller() +"',"+ offer.getItem().getType().getId() + ","+offer.getItem().getAmount() +","+offer.getPrice()+"," + offer.getItem().getDurability()+",'"+offer.getSellerUUID().toString()+"')";
    	executeUpdate(sql);
    }
    
    @SuppressWarnings("deprecation")
    public void addEOffer(Offer offer, String edata){
		String sql = "insert into vshop_estock(merchant,item,data,price,edata,uuid) values('" +offer.getSeller() +"',"+ offer.getItem().getType().getId() + ","+offer.getItem().getDurability() +","+offer.getPrice()+",'" + edata +"','"+offer.getSellerUUID().toString()+"')";
    	executeUpdate(sql);
    }
    
    public List<Offer> getAllOffers(){
    	String sql = "select * from vshop_stock order by price asc";
    	try(Connection connection = ds.getDataSource().getConnection();
        	PreparedStatement stmt = connection.prepareStatement(sql);
        	ResultSet result = stmt.executeQuery()){
    		return Offer.listOffers(result);
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    		return null;
    	}
    }

    @SuppressWarnings("deprecation")
	public List<Offer> getItemOffers(ItemStack item){
    	String sql = "select * from vshop_stock where item=" + item.getTypeId()+ " and damage=" + item.getDurability() + " order by price asc";
    	try(Connection connection = ds.getDataSource().getConnection();
        	PreparedStatement stmt = connection.prepareStatement(sql);
        	ResultSet result = stmt.executeQuery()){
    		return Offer.listOffers(result);
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    		return null;
    	}
    }
    
    @SuppressWarnings("deprecation")
	public List<Offer> getEnchantedOffers(ItemStack item){
    	String sql = "select * from vshop_estock where item=" + item.getTypeId() + " and data=" + item.getDurability() + " order by price asc";
    	try(Connection connection = ds.getDataSource().getConnection();
        	PreparedStatement stmt = connection.prepareStatement(sql);
        	ResultSet result = stmt.executeQuery()){
    		return Offer.listEnchantedOffers(result);
    	} catch (SQLException e){
    		cm.logError(e.getMessage(), true);
    		return null;
    	}
    }
    
    @SuppressWarnings("deprecation")
	public List<Offer> getSpecificEnchantedOffer(ItemStack item, String edata, double price){
    	String sql = "select * from vshop_estock where item=" + item.getTypeId() + " and data=" + item.getDurability() + " and edata='" + edata + "' and price=" + price;
    	try(Connection connection = ds.getDataSource().getConnection();
        	PreparedStatement stmt = connection.prepareStatement(sql);
        	ResultSet result = stmt.executeQuery()){
    		return Offer.listEnchantedOffers(result);
    	} catch (SQLException e){
    		cm.logError(e.getMessage(), true);
    		return null;
    	}
    }

    @SuppressWarnings("deprecation")
	public List<Offer> getSellerOffers(UUID merchantUUID, ItemStack item){
    	String sql = "select * from vshop_stock where uuid = '" + merchantUUID.toString() + "' and item =" + item.getTypeId() + " and damage=" + item.getDurability();
    	try(Connection connection = ds.getDataSource().getConnection();
        	PreparedStatement stmt = connection.prepareStatement(sql);
        	ResultSet result = stmt.executeQuery()){
    		return Offer.listOffers(result);
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    		return null;
    	}
    }

    @SuppressWarnings("deprecation")
	public void removeSellerOffers(UUID merchantUUID, ItemStack item){
    	String sql = "delete from vshop_stock where uuid = '" + merchantUUID.toString() + "' and item =" + item.getTypeId() + " and damage = " + item.getDurability();
    	executeUpdate(sql);
    }

    public void deleteItem(int id){
    	String sql = "delete from vshop_stock where id="+id;
    	executeUpdate(sql);
	}

    public void updateQuantity(int id, int quantity){
    	String sql = "update vshop_stock set amount="+quantity+" where id=" + id;
    	executeUpdate(sql);
	}

    public void updatePrice(int id, double price){
    	String sql = "update vshop_stock set price="+price+" where id=" + id;
    	executeUpdate(sql);
    }
    @SuppressWarnings("deprecation")
	public void updatePrice(UUID merchantUUID, double price, ItemStack item){
    	String sql = "update vshop_stock set price="+price+" where uuid='" + merchantUUID + "' and item =" + item.getTypeId() + " and damage = " + item.getDurability();
    	executeUpdate(sql);
    }
    
    @SuppressWarnings("deprecation")
	public void logTransaction(Transaction transaction){
    	String sql = "insert into vshop_transactions(seller,buyer,item,amount,cost,damage,buyer_uuid,seller_uuid) values('" +transaction.getSeller() +"','"+ transaction.getBuyer() + "'," + transaction.getItem().getTypeId() + ","+ transaction.getItem().getAmount() +","+transaction.getCost()+","+transaction.getItem().getDurability()+",'"+transaction.getBuyerUUID().toString()+"','"+transaction.getSellerUUID().toString()+"')";
    	executeUpdate(sql);
    }

    public List<Offer> getBestPrices(){
    	String sql = "select f.* from (select item,min(price) as minprice from vshop_stock group by item) as x inner join vshop_stock as f on f.item = x.item and f.price = x.minprice";
    	try(Connection connection = ds.getDataSource().getConnection();
        	PreparedStatement stmt = connection.prepareStatement(sql);
        	ResultSet result = stmt.executeQuery()){
    		return Offer.listOffers(result);
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    		return null;
    	}
    }

    public List<Offer> searchBySeller(UUID merchantUUID){
    	String sql = "select * from vshop_stock where uuid like '%" + merchantUUID.toString() +  "%'";
    	try(Connection connection = ds.getDataSource().getConnection();
        	PreparedStatement stmt = connection.prepareStatement(sql);
        	ResultSet result = stmt.executeQuery()){
    		return Offer.listOffers(result);
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    		return null;
    	}
    }

    public List<Transaction> getTransactions(){
    	String sql = "select * from vshop_transactions order by id desc";
    	try(Connection connection = ds.getDataSource().getConnection();
        	PreparedStatement stmt = connection.prepareStatement(sql);
        	ResultSet result = stmt.executeQuery()){
    		return Transaction.listTransactions(result);
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    		return null;
    	}
    }

	public List<Transaction> getTransactions(UUID targetUUID){
		String sql = "select * from vshop_transactions where seller_uuid like '%" + targetUUID.toString() +"%' OR buyer_uuid like '%" + targetUUID.toString() +"%' order by id";
		try(Connection connection = ds.getDataSource().getConnection();
	    	PreparedStatement stmt = connection.prepareStatement(sql);
	    	ResultSet result = stmt.executeQuery()){
			return Transaction.listTransactions(result);
		} catch(SQLException e){
			cm.logError(e.getMessage(), true);
			return null;
		}
	}

    @SuppressWarnings("deprecation")
	public List<Offer> getPrices(ItemStack item){
    	String sql = "select * from vshop_stock where item=" + item.getTypeId() + " AND damage=" + item.getDurability() + " order by price asc limit 0,10";
    	try(Connection connection = ds.getDataSource().getConnection();
    		PreparedStatement stmt = connection.prepareStatement(sql);
    		ResultSet result = stmt.executeQuery()){
    		return Offer.listOffers(result);
    	} catch (SQLException e) {
    		cm.logError(e.getMessage(), true);
    		return null;
		}
    }
    
    private void executeUpdate(String sql){
    	try(Connection connection = ds.getDataSource().getConnection();
    		PreparedStatement stmt = connection.prepareStatement(sql))
    	{
    		stmt.executeUpdate();
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    	}
    }
	
}
