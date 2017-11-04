/*
 * VirtualShopX
 * Copyright (C) 2015-2017 Daniel D. Scalzi
 * See LICENSE.txt for license information.
 */
package com.dscalzi.virtualshopx.managers;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.inventory.ItemStack;

import com.dscalzi.virtualshopx.VirtualShopX;
import com.dscalzi.virtualshopx.command.Buy;
import com.dscalzi.virtualshopx.command.Cancel;
import com.dscalzi.virtualshopx.command.Reprice;
import com.dscalzi.virtualshopx.command.Sell;
import com.dscalzi.virtualshopx.command.enchanted.EBuy;
import com.dscalzi.virtualshopx.command.enchanted.ECancel;
import com.dscalzi.virtualshopx.command.enchanted.EReprice;
import com.dscalzi.virtualshopx.command.enchanted.ESell;
import com.dscalzi.virtualshopx.connection.ConnectionWrapper;
import com.dscalzi.virtualshopx.connection.MySQLWrapper;
import com.dscalzi.virtualshopx.connection.SQLiteWrapper;
import com.dscalzi.virtualshopx.objects.Confirmable;
import com.dscalzi.virtualshopx.objects.Offer;
import com.dscalzi.virtualshopx.objects.Transaction;
import com.dscalzi.virtualshopx.util.ItemDB;

public final class DatabaseManager {

	/* Constants */
	public static final String ID = "id";
	public static final String UUIDKEY = "uuid";
	public static final String VENDOR_UUID = "vendor_uuid";
	public static final String BUYER_UUID = "buyer_uuid";
	public static final String ITEM_ID = "item";
	public static final String ITEM_DATA = "damage";
	public static final String ITEM_EDATA = "edata";
	public static final String QUANTITY = "quantity";
	public static final String PRICE = "price";
	public static final String COST = "cost";
	public static final String TIMESTAMP = "timestamp";
	
	private static final Map<Class<? extends Confirmable>, String> togglesKey;
	private static boolean initialized;
	private static DatabaseManager instance;
	static {
		togglesKey = new HashMap<Class<? extends Confirmable>, String>();
		togglesKey.put(Buy.class, "buyconfirm");
		togglesKey.put(EBuy.class, "ebuyconfirm");
		togglesKey.put(Sell.class, "sellconfirm");
		togglesKey.put(ESell.class, "esellconfirm");
		togglesKey.put(Reprice.class, "repriceconfirm");
		togglesKey.put(EReprice.class, "erepriceconfirm");
		togglesKey.put(Cancel.class, "cancelconfirm");
		togglesKey.put(ECancel.class, "ecancelconfirm");
	}
	
	public enum ConnectionType{
		SQLite, MYSQL, VOID;
	}
	
	private ConnectionType type;
	
	private VirtualShopX plugin;
	private ConnectionWrapper ds;
	private ConfigManager configM;
	private MessageManager cm;
	
	private DatabaseManager(VirtualShopX plugin){
		this.plugin = plugin;
		this.configM = ConfigManager.getInstance();
		this.cm = MessageManager.getInstance();
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
        this.ds = new SQLiteWrapper(new File(plugin.getDataFolder(), plugin.getName() + ".db"));
        return this.ds.initialize();
    }

    private boolean loadMySQL() {
    	this.ds = new MySQLWrapper(configM.mySQLHost(), configM.mySQLport(), 
    			configM.mySQLdatabase(), configM.mySQLUserName(), 
    			configM.mySQLPassword());
    	return this.ds.initialize();
    }
	
	public static void initialize(VirtualShopX plugin){
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
			String query = "create table vshop_stock(`" + ID + "` integer primary key" + autoIncrement + ", `" + ITEM_ID + "` integer not null, `" + ITEM_DATA + "` smallint, `" + QUANTITY + "` integer not null, `" + PRICE + "` double not null, `" + UUIDKEY + "` char(36) not null)";
			if(createTable(query)){
				cm.logInfo("Successfully created table vshop_stock.");
				++checksum;
			} else
				cm.logError("Unable to create table vshop_stock.", true);
		}
		if(!ds.checkTable("vshop_estock")){
			++desired;
			String query = "create table vshop_estock(`" + ID + "` integer primary key" + autoIncrement + ", `" + ITEM_ID + "` integer not null, `" + ITEM_DATA + "` smallint not null, `" + PRICE + "` double not null, `" + ITEM_EDATA + "` varchar(255) not null, `" + UUIDKEY + "` char(36) not null)";
			if(createTable(query)){
				cm.logInfo("Successfully created table vshop_estock.");
				++checksum;
			} else
				cm.logError("Unable to create table vshop_stock.", true);
		}
		if(!ds.checkTable("vshop_transactions")){
			++desired;
			String query = "create table vshop_transactions(`" + ID + "` integer primary key" + autoIncrement + ", `" + ITEM_ID + "` integer not null, `" + ITEM_DATA + "` smallint not null, `" + ITEM_EDATA + "` varchar(255), `" + QUANTITY + "` integer not null, `" + COST + "` double not null, `" + TIMESTAMP + "` bigint, `" + BUYER_UUID + "` char(36) not null, `" + VENDOR_UUID + "` char(36) not null)";
			if(createTable(query)){
				cm.logInfo("Successfully created table vshop_transaction.");
				++checksum;
			} else
				cm.logError("Unable to create table vshop_transaction.", true);
		}
		if(!ds.checkTable("vshop_toggles")){
			++desired;
			String query = "create table vshop_toggles(`" + ID + "` integer primary key" + autoIncrement + ", `buyconfirm` bit not null, `ebuyconfirm` bit not null,`sellconfirm` bit not null, `esellconfirm` bit not null, `cancelconfirm` bit not null, `ecancelconfirm` bit not null, `repriceconfirm` bit not null, `erepriceconfirm` bit not null, `" + UUIDKEY + "` char(36) not null)";
			if(createTable(query)){
				cm.logInfo("Successfully created table vshop_toggles.");
				++checksum;
			} else
				cm.logError("Unable to create table vshop_toggles.", true);
		}
		
		if(checksum != desired){
			cm.logError("Failed to create one or more database tables, shutting down..", true);
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
    
    public boolean isPlayerInToggles(UUID vendorUUID){
    	String sql = "select * from vshop_toggles where " + UUIDKEY + "='" + vendorUUID.toString() + "'";
    	try(Connection connection = ds.getDataSource().getConnection();
    		PreparedStatement stmt = connection.prepareStatement(sql))
    	{
	    	try(ResultSet result = stmt.executeQuery()){
		    	if(!result.next())
		    		return false;
		    	else
		    		if(result.getString(UUIDKEY).equalsIgnoreCase(vendorUUID.toString()))
		    			return true;
		    	return false;
	    	}
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    	}
    	return false;
    }
    
    public void addPlayerToToggles(UUID vendorUUID){
    	String sql = "insert into vshop_toggles(buyconfirm,ebuyconfirm,sellconfirm,esellconfirm,cancelconfirm,ecancelconfirm,repriceconfirm,erepriceconfirm," + UUIDKEY + ") values(1,1,1,1,1,1,1,1,'" + vendorUUID.toString() + "')";
    	executeUpdate(sql);
    }
    
    public void updateToggle(UUID vendorUUID, Class<? extends Confirmable> clazz, boolean value){
    	if(!togglesKey.containsKey(clazz))
    		return;
    	if(!isPlayerInToggles(vendorUUID))
    		addPlayerToToggles(vendorUUID);
    	
    	int dataval = value ? 1 : 0;
    	String sql = "update vshop_toggles set " + togglesKey.get(clazz) + "=" + dataval + " where " + UUIDKEY + "='" + vendorUUID.toString() + "'";
    	executeUpdate(sql);
    }
    
    public boolean getToggle(UUID vendorUUID, Class<? extends Confirmable> clazz){
    	if(!togglesKey.containsKey(clazz))
    		return false;
    	if(!isPlayerInToggles(vendorUUID))
    		addPlayerToToggles(vendorUUID);
    	
    	String sql = "select * from vshop_toggles where " + UUIDKEY + "='" + vendorUUID.toString() + "'";
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
    	String sql = "insert into vshop_stock(" + ITEM_ID + "," + ITEM_DATA + "," + QUANTITY + "," + PRICE + "," + UUIDKEY + ") values(" + offer.getItem().getType().getId() + ","+offer.getItem().getDurability() +","+offer.getItem().getAmount()+"," + offer.getPrice()+",'"+offer.getSellerUUID().toString()+"')";
    	executeUpdate(sql);
    }
    
    @SuppressWarnings("deprecation")
    public void addEOffer(Offer offer, String edata){
		String sql = "insert into vshop_estock(" + ITEM_ID + "," + ITEM_DATA + "," + PRICE + "," + ITEM_EDATA + "," + UUIDKEY + ") values(" + offer.getItem().getType().getId() + ","+offer.getItem().getDurability() +","+offer.getPrice()+",'" + edata +"','"+offer.getSellerUUID().toString()+"')";
    	executeUpdate(sql);
    }
    
    public List<Offer> getAllRegularOffers(){
    	String sql = "select * from vshop_stock order by " + PRICE + " asc";
    	try(Connection connection = ds.getDataSource().getConnection();
        	PreparedStatement stmt = connection.prepareStatement(sql);
        	ResultSet result = stmt.executeQuery()){
    		return Offer.listOffers(result);
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    		return null;
    	}
    }
    
    public List<Offer> getAllEnchantedOffers(boolean withLore){
    	String sql = "select * from vshop_estock order by " + PRICE + " asc";
    	try(Connection connection = ds.getDataSource().getConnection();
        	PreparedStatement stmt = connection.prepareStatement(sql);
        	ResultSet result = stmt.executeQuery()){
    		return Offer.listEnchantedOffers(result, withLore);
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    		return null;
    	}
    }

  //TODO next
    
    @SuppressWarnings("deprecation")
	public List<Offer> getItemOffers(ItemStack item){
    	String sql = "select * from vshop_stock where " + ITEM_ID + "=" + item.getTypeId()+ " and " + ITEM_DATA + "=" + item.getDurability() + " order by " + PRICE + " asc";
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
	public List<Offer> getEnchantedOffers(ItemStack item, boolean withLore){
    	String sql = "select * from vshop_estock where " + ITEM_ID + "=" + item.getTypeId() + " and " + ITEM_DATA + "=" + item.getDurability() + " order by " + PRICE + " asc";
    	try(Connection connection = ds.getDataSource().getConnection();
        	PreparedStatement stmt = connection.prepareStatement(sql);
        	ResultSet result = stmt.executeQuery()){
    		return Offer.listEnchantedOffers(result, withLore);
    	} catch (SQLException e){
    		cm.logError(e.getMessage(), true);
    		return null;
    	}
    }
    
    @SuppressWarnings("deprecation")
	public List<Offer> getOffersWithEnchants(ItemStack item, boolean withLore){
    	String sql = "select * from vshop_estock where " + ITEM_ID + "=" + item.getTypeId() + " and " + ITEM_DATA + "=" + item.getDurability() + " and " + ITEM_EDATA + "='" + ItemDB.formatEnchantData(ItemDB.getEnchantments(item)) + "' order by " + PRICE + " asc";
    	try(Connection connection = ds.getDataSource().getConnection();
        	PreparedStatement stmt = connection.prepareStatement(sql);
        	ResultSet result = stmt.executeQuery()){
    		return Offer.listEnchantedOffers(result, withLore);
    	} catch (SQLException e){
    		cm.logError(e.getMessage(), true);
    		return null;
    	}
    }
    
    @SuppressWarnings("deprecation")
	public List<Offer> getOffersWithEnchants(UUID vendorUUID, ItemStack item, boolean withLore){
    	String sql = "select * from vshop_estock where " + UUIDKEY + "= '" + vendorUUID.toString() + "' and " + ITEM_ID + "=" + item.getTypeId() + " and " + ITEM_DATA + "=" + item.getDurability() + " and " + ITEM_EDATA + "='" + ItemDB.formatEnchantData(ItemDB.getEnchantments(item)) + "' order by " + PRICE + " asc";
    	try(Connection connection = ds.getDataSource().getConnection();
        	PreparedStatement stmt = connection.prepareStatement(sql);
        	ResultSet result = stmt.executeQuery()){
    		return Offer.listEnchantedOffers(result, withLore);
    	} catch (SQLException e){
    		cm.logError(e.getMessage(), true);
    		return null;
    	}
    }
    
    @SuppressWarnings("deprecation")
	public List<Offer> getSpecificEnchantedOffer(ItemStack item, String edata, double price, boolean withLore){
    	String sql = "select * from vshop_estock where " + ITEM_ID + "=" + item.getTypeId() + " and " + ITEM_DATA + "=" + item.getDurability() + " and " + ITEM_EDATA + "='" + edata + "' and " + PRICE + "=" + price;
    	try(Connection connection = ds.getDataSource().getConnection();
        	PreparedStatement stmt = connection.prepareStatement(sql);
        	ResultSet result = stmt.executeQuery()){
    		return Offer.listEnchantedOffers(result, withLore);
    	} catch (SQLException e){
    		cm.logError(e.getMessage(), true);
    		return null;
    	}
    }

    @SuppressWarnings("deprecation")
	public List<Offer> getSellerOffers(UUID vendorUUID, ItemStack item){
    	String sql = "select * from vshop_stock where " + UUIDKEY + "= '" + vendorUUID.toString() + "' and " + ITEM_ID + "=" + item.getTypeId() + " and " + ITEM_DATA + "=" + item.getDurability();
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
	public List<Offer> getEnchantedSellerOffers(UUID vendorUUID, ItemStack item, boolean withLore){
    	String sql = "select * from vshop_estock where " + UUIDKEY + "= '" + vendorUUID.toString() + "' and " + ITEM_ID + "=" + item.getTypeId() + " and " + ITEM_DATA + "=" + item.getDurability() + " order by " + PRICE + " asc";
    	try(Connection connection = ds.getDataSource().getConnection();
        	PreparedStatement stmt = connection.prepareStatement(sql);
        	ResultSet result = stmt.executeQuery()){
    		return Offer.listEnchantedOffers(result, withLore);
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    		return null;
    	}
    }

    @SuppressWarnings("deprecation")
	public void removeSellerOffers(UUID vendorUUID, ItemStack item){
    	String sql = "delete from vshop_stock where " + UUIDKEY + "= '" + vendorUUID.toString() + "' and " + ITEM_ID + "=" + item.getTypeId() + " and " + ITEM_DATA + "= " + item.getDurability();
    	executeUpdate(sql);
    }

    public void deleteItem(int id){
    	String sql = "delete from vshop_stock where " + ID + "="+id;
    	executeUpdate(sql);
	}
    
    public void deleteEnchantedItem(int id){
    	String sql = "delete from vshop_estock where " + ID + "="+id;
    	executeUpdate(sql);
    }

    public void updateQuantity(int id, int quantity){
    	String sql = "update vshop_stock set " + QUANTITY + "="+quantity+" where " + ID + "=" + id;
    	executeUpdate(sql);
	}

    public void updatePrice(int id, double price){
    	String sql = "update vshop_stock set " + PRICE + "="+price+" where " + ID + "=" + id;
    	executeUpdate(sql);
    }
    @SuppressWarnings("deprecation")
	public void updatePrice(UUID vendorUUID, double price, ItemStack item){
    	String sql = "update vshop_stock set " + PRICE + "="+price+" where " + UUIDKEY + "='" + vendorUUID + "' and " + ITEM_ID + "=" + item.getTypeId() + " and " + ITEM_DATA + "= " + item.getDurability();
    	executeUpdate(sql);
    }
    
	public void updatePriceEnchanted(int id, double price){
    	String sql = "update vshop_estock set " + PRICE + "="+price+" where " + ID + "=" + id;
    	executeUpdate(sql);
    }
    
    
    //TODO HI
    @SuppressWarnings("deprecation")
	public void logTransaction(Transaction t){
    	String edata = null;
    	if(ItemDB.hasEnchantments(t.getItem())) edata = ItemDB.formatEnchantData(ItemDB.getEnchantments(t.getItem()));
    	String sql = "insert into vshop_transactions(" + ITEM_ID + "," + ITEM_DATA + "," + ITEM_EDATA + "," + QUANTITY + "," + COST + "," + TIMESTAMP + "," + BUYER_UUID + "," + VENDOR_UUID + ") values(" + t.getItem().getTypeId() + ","+ t.getItem().getDurability() +",'"+edata+"',"+t.getItem().getAmount()+","+t.getCost()+"," + t.getTimestamp() + ",'"+t.getBuyerUUID().toString()+"','"+t.getSellerUUID().toString()+"')";
    	executeUpdate(sql);
    }

    public List<Offer> getBestPrices(){
    	String sql = "select f.* from (select " + ITEM_ID + ",min(" + PRICE + ") as min" + PRICE + " from vshop_stock group by " + ITEM_ID + ") as x inner join vshop_stock as f on f." + ITEM_ID + " = x." + ITEM_ID + " and f." + PRICE + " = x.min" + PRICE + "";
    	try(Connection connection = ds.getDataSource().getConnection();
        	PreparedStatement stmt = connection.prepareStatement(sql);
        	ResultSet result = stmt.executeQuery()){
    		return Offer.listOffers(result);
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    		return null;
    	}
    }

    public List<Offer> searchRegularBySeller(UUID vendorUUID){
    	String sql = "select * from vshop_stock where " + UUIDKEY + "='" + vendorUUID.toString() +  "'";
    	try(Connection connection = ds.getDataSource().getConnection();
        	PreparedStatement stmt = connection.prepareStatement(sql);
        	ResultSet result = stmt.executeQuery()){
    		return Offer.listOffers(result);
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    		return null;
    	}
    }
    
    public List<Offer> searchEnchantedBySeller(UUID vendorUUID, boolean withLore){
    	String sql = "select * from vshop_estock where " + UUIDKEY + "='" + vendorUUID.toString() +  "'";
    	try(Connection connection = ds.getDataSource().getConnection();
        	PreparedStatement stmt = connection.prepareStatement(sql);
        	ResultSet result = stmt.executeQuery()){
    		return Offer.listEnchantedOffers(result, withLore);
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    		return null;
    	}
    }

    public List<Transaction> getTransactions(){
    	String sql = "select * from vshop_transactions order by " + ID + " desc";
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
		String sql = "select * from vshop_transactions where " + VENDOR_UUID + "='" + targetUUID.toString() +"' OR " + BUYER_UUID + "='" + targetUUID.toString() +"' order by " + ID + "";
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
    	String sql = "select * from vshop_stock where " + ITEM_ID + "=" + item.getTypeId() + " AND " + ITEM_DATA + "=" + item.getDurability() + " order by " + PRICE + " asc limit 0,10";
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
