/*
 * VirtualShopX
 * Copyright (C) 2015-2018 Daniel D. Scalzi
 * See LICENSE for license information.
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
    
    // @NotNull integer
	public static final String KEY_ID = "ID";
	public static final String KEY_QUANTITY = "QUANTITY";
	
	// @NotNull double
	public static final String KEY_PRICE = "PRICE";
	public static final String KEY_COST = "COST";
	
	// @NotNull varchar(256)
	public static final String KEY_MATERIAL = "MATERIAL";
	
	// @Nullable varchar(256)
	public static final String KEY_POTION_DATA = "POTION_DATA";
	public static final String KEY_ENCHANTMENT_DATA = "ENCHANTMENT_DATA";
	
	// @NotNull char(36)
	public static final String KEY_UUID = "UUID";
    public static final String KEY_SELLER_UUID = "SELLER_UUID";
	public static final String KEY_BUYER_UUID = "BUYER_UUID";
	
	// @NotNull bigint
	public static final String KEY_TIMESTAMP = "TIMESTAMP";
	
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
		if(!ds.checkTable("vsx_stock")){
			++desired;
			String query = "create table vsx_stock(`" + KEY_ID + "` integer primary key" + autoIncrement + ", `" + KEY_MATERIAL + "` varchar(256) not null, `" + KEY_POTION_DATA + "` varchar(256), `" + KEY_ENCHANTMENT_DATA + "` varchar(256) not null, `" + KEY_QUANTITY + "` integer not null, `" + KEY_PRICE + "` double not null, `" + KEY_UUID + "` char(36) not null)";
			if(createTable(query)){
				cm.logInfo("Successfully created table vsx_stock.");
				++checksum;
			} else
				cm.logError("Unable to create table vsx_stock.", true);
		}
		/*if(!ds.checkTable("vsx_estock")){
			++desired;
			String query = "create table vsx_estock(`" + KEY_ID + "` integer primary key" + autoIncrement + ", `" + KEY_MATERIAL + "` varchar(256) not null, `" + KEY_PRICE + "` double not null, `" + KEY_ITEM_ENCHANTMENT_DATA + "` varchar(255) not null, `" + KEY_UUID + "` char(36) not null)";
			if(createTable(query)){
				cm.logInfo("Successfully created table vsx_estock.");
				++checksum;
			} else
				cm.logError("Unable to create table vsx_estock.", true);
		}*/
		if(!ds.checkTable("vsx_transactions")){
			++desired;
			String query = "create table vsx_transactions(`" + KEY_ID + "` integer primary key" + autoIncrement + ", `" + KEY_MATERIAL + "` varchar(256) not null, `" + KEY_POTION_DATA + "` varchar(256), `" + KEY_ENCHANTMENT_DATA + "` varchar(256), `" + KEY_QUANTITY + "` integer not null, `" + KEY_COST + "` double not null, `" + KEY_TIMESTAMP + "` bigint, `" + KEY_BUYER_UUID + "` char(36) not null, `" + KEY_SELLER_UUID + "` char(36) not null)";
			if(createTable(query)){
				cm.logInfo("Successfully created table vsx_transaction.");
				++checksum;
			} else
				cm.logError("Unable to create table vsx_transaction.", true);
		}
		if(!ds.checkTable("vsx_toggles")){
			++desired;
			String query = "create table vsx_toggles(`" + KEY_ID + "` integer primary key" + autoIncrement + ", `buyconfirm` bit not null, `ebuyconfirm` bit not null,`sellconfirm` bit not null, `esellconfirm` bit not null, `cancelconfirm` bit not null, `ecancelconfirm` bit not null, `repriceconfirm` bit not null, `erepriceconfirm` bit not null, `" + KEY_UUID + "` char(36) not null)";
			if(createTable(query)){
				cm.logInfo("Successfully created table vsx_toggles.");
				++checksum;
			} else
				cm.logError("Unable to create table vsx_toggles.", true);
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
    	String sql = "select * from vsx_toggles where " + KEY_UUID + "='" + vendorUUID.toString() + "'";
    	try(Connection connection = ds.getDataSource().getConnection();
    		PreparedStatement stmt = connection.prepareStatement(sql))
    	{
	    	try(ResultSet result = stmt.executeQuery()){
		    	if(!result.next())
		    		return false;
		    	else
		    		if(result.getString(KEY_UUID).equalsIgnoreCase(vendorUUID.toString()))
		    			return true;
		    	return false;
	    	}
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    	}
    	return false;
    }
    
    public void addPlayerToToggles(UUID vendorUUID){
    	String sql = "insert into vsx_toggles(buyconfirm,ebuyconfirm,sellconfirm,esellconfirm,cancelconfirm,ecancelconfirm,repriceconfirm,erepriceconfirm," + KEY_UUID + ") values(1,1,1,1,1,1,1,1,'" + vendorUUID.toString() + "')";
    	executeUpdate(sql);
    }
    
    public void updateToggle(UUID vendorUUID, Class<? extends Confirmable> clazz, boolean value){
    	if(!togglesKey.containsKey(clazz))
    		return;
    	if(!isPlayerInToggles(vendorUUID))
    		addPlayerToToggles(vendorUUID);
    	
    	int dataval = value ? 1 : 0;
    	String sql = "update vsx_toggles set " + togglesKey.get(clazz) + "=" + dataval + " where " + KEY_UUID + "='" + vendorUUID.toString() + "'";
    	executeUpdate(sql);
    }
    
    public boolean getToggle(UUID vendorUUID, Class<? extends Confirmable> clazz){
    	if(!togglesKey.containsKey(clazz))
    		return false;
    	if(!isPlayerInToggles(vendorUUID))
    		addPlayerToToggles(vendorUUID);
    	
    	String sql = "select * from vsx_toggles where " + KEY_UUID + "='" + vendorUUID.toString() + "'";
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
    
	public void addOffer(Offer offer){
    	String sql = "insert into vsx_stock(" + KEY_MATERIAL + "," + KEY_POTION_DATA + "," + KEY_ENCHANTMENT_DATA + "," + KEY_QUANTITY + "," + KEY_PRICE + "," + KEY_UUID + ") values('" + offer.getItem().getType().name() + "','" + ItemDB.serializePotionData(offer.getItem()) + "','" + ItemDB.serializeEnchantmentData(offer.getItem()) + "'," + offer.getItem().getAmount() + "," + offer.getPrice() + ",'" + offer.getSellerUUID().toString() + "')";
    	executeUpdate(sql);
    }
    
	/*
    public void addEOffer(Offer offer, String edata){
		String sql = "insert into vsx_estock(" + KEY_MATERIAL + "," + KEY_PRICE + "," + KEY_ENCHANTMENT_DATA + "," + KEY_UUID + ") values(" + offer.getItem().getType().getId() + ","+offer.getItem().getDurability() +","+offer.getPrice()+",'" + edata +"','"+offer.getSellerUUID().toString()+"')";
    	executeUpdate(sql);
    }*/
    
    public List<Offer> getAllRegularOffers(){
    	String sql = "select * from vsx_stock where " + KEY_ENCHANTMENT_DATA + "='null' order by " + KEY_PRICE + " asc";
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
    	String sql = "select * from vsx_stock where NOT " + KEY_ENCHANTMENT_DATA + "='null' order by " + KEY_PRICE + " asc";
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
    
	public List<Offer> getItemOffers(ItemStack item){
    	String sql = "select * from vsx_stock where " + KEY_MATERIAL + "='" + item.getType().name() + "' and " + KEY_POTION_DATA + "='" + ItemDB.serializePotionData(item) + "' and NOT " + KEY_ENCHANTMENT_DATA + "='null' order by " + KEY_PRICE + " asc";
    	try(Connection connection = ds.getDataSource().getConnection();
        	PreparedStatement stmt = connection.prepareStatement(sql);
        	ResultSet result = stmt.executeQuery()){
    		return Offer.listOffers(result);
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    		return null;
    	}
    }
    
	public List<Offer> getEnchantedOffers(ItemStack item, boolean withLore){
    	String sql = "select * from vsx_stock where " + KEY_MATERIAL + "='" + item.getType().name() + "' and " + KEY_POTION_DATA + "='" + ItemDB.serializePotionData(item) + "' and " + KEY_ENCHANTMENT_DATA + "='null' order by " + KEY_PRICE + " asc";
    	try(Connection connection = ds.getDataSource().getConnection();
        	PreparedStatement stmt = connection.prepareStatement(sql);
        	ResultSet result = stmt.executeQuery()){
    		return Offer.listEnchantedOffers(result, withLore);
    	} catch (SQLException e){
    		cm.logError(e.getMessage(), true);
    		return null;
    	}
    }
    
	public List<Offer> getOffersWithEnchants(ItemStack item, boolean withLore){
    	String sql = "select * from vsx_stock where " + KEY_MATERIAL + "='" + item.getType().name() + "' and " + KEY_POTION_DATA + "='" + ItemDB.serializePotionData(item) + "' and " + KEY_ENCHANTMENT_DATA + "='" + ItemDB.serializeEnchantmentData(item) + "' order by " + KEY_PRICE + " asc";
    	try(Connection connection = ds.getDataSource().getConnection();
        	PreparedStatement stmt = connection.prepareStatement(sql);
        	ResultSet result = stmt.executeQuery()){
    		return Offer.listEnchantedOffers(result, withLore);
    	} catch (SQLException e){
    		cm.logError(e.getMessage(), true);
    		return null;
    	}
    }
    
	public List<Offer> getOffersWithEnchants(UUID vendorUUID, ItemStack item, boolean withLore){
    	String sql = "select * from vsx_stock where " + KEY_UUID + "= '" + vendorUUID.toString() + "' and " + KEY_MATERIAL + "='" + item.getType().name() + "' and " + KEY_POTION_DATA + "='" + ItemDB.serializePotionData(item) + "' and " + KEY_ENCHANTMENT_DATA + "='" + ItemDB.serializeEnchantmentData(item) + "' order by " + KEY_PRICE + " asc";
    	try(Connection connection = ds.getDataSource().getConnection();
        	PreparedStatement stmt = connection.prepareStatement(sql);
        	ResultSet result = stmt.executeQuery()){
    		return Offer.listEnchantedOffers(result, withLore);
    	} catch (SQLException e){
    		cm.logError(e.getMessage(), true);
    		return null;
    	}
    }
    
	public List<Offer> getSpecificEnchantedOffer(ItemStack item, String edata, double price, boolean withLore){
    	String sql = "select * from vsx_stock where " + KEY_MATERIAL + "='" + item.getType().name() + "' and " + KEY_POTION_DATA + "='" + ItemDB.serializePotionData(item) + "' and " + KEY_ENCHANTMENT_DATA + "='" + edata + "' and " + KEY_PRICE + "=" + price;
    	try(Connection connection = ds.getDataSource().getConnection();
        	PreparedStatement stmt = connection.prepareStatement(sql);
        	ResultSet result = stmt.executeQuery()){
    		return Offer.listEnchantedOffers(result, withLore);
    	} catch (SQLException e){
    		cm.logError(e.getMessage(), true);
    		return null;
    	}
    }

	public List<Offer> getSellerOffers(UUID vendorUUID, ItemStack item){
    	String sql = "select * from vsx_stock where " + KEY_UUID + "= '" + vendorUUID.toString() + "' and " + KEY_MATERIAL + "='" + item.getType().name() + "' and " + KEY_POTION_DATA + "='" + ItemDB.serializePotionData(item) + "' and " + KEY_ENCHANTMENT_DATA + "='null'";
    	try(Connection connection = ds.getDataSource().getConnection();
        	PreparedStatement stmt = connection.prepareStatement(sql);
        	ResultSet result = stmt.executeQuery()){
    		return Offer.listOffers(result);
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    		return null;
    	}
    }
    
	public List<Offer> getEnchantedSellerOffers(UUID vendorUUID, ItemStack item, boolean withLore){
    	String sql = "select * from vsx_stock where " + KEY_UUID + "= '" + vendorUUID.toString() + "' and " + KEY_MATERIAL + "='" + item.getType().name() + "' and " + KEY_POTION_DATA + "='" + ItemDB.serializePotionData(item) + "' and NOT " + KEY_ENCHANTMENT_DATA + "='null' order by " + KEY_PRICE + " asc";
    	try(Connection connection = ds.getDataSource().getConnection();
        	PreparedStatement stmt = connection.prepareStatement(sql);
        	ResultSet result = stmt.executeQuery()){
    		return Offer.listEnchantedOffers(result, withLore);
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    		return null;
    	}
    }

	public void removeSellerOffers(UUID vendorUUID, ItemStack item){
    	String sql = "delete from vsx_stock where " + KEY_UUID + "= '" + vendorUUID.toString() + "' and " + KEY_MATERIAL + "='" + item.getType().name() + "' and " + KEY_POTION_DATA + "='" + ItemDB.serializePotionData(item) + "' and " + KEY_ENCHANTMENT_DATA + "='null'";
    	executeUpdate(sql);
    }

    public void deleteItem(int id){
    	String sql = "delete from vsx_stock where " + KEY_ID + "=" + id;
    	executeUpdate(sql);
	}
    
    /*public void deleteEnchantedItem(int id){
    	String sql = "delete from vsx_estock where " + KEY_ID + "=" + id;
    	executeUpdate(sql);
    }*/

    public void updateQuantity(int id, int quantity){
    	String sql = "update vsx_stock set " + KEY_QUANTITY + "=" + quantity + " where " + KEY_ID + "=" + id;
    	executeUpdate(sql);
	}

    public void updatePrice(int id, double price){
    	String sql = "update vsx_stock set " + KEY_PRICE + "="+price+" where " + KEY_ID + "=" + id;
    	executeUpdate(sql);
    }

	public void updatePrice(UUID vendorUUID, double price, ItemStack item){
    	String sql = "update vsx_stock set " + KEY_PRICE + "=" + price + " where " + KEY_UUID + "='" + vendorUUID + "' and " + KEY_MATERIAL + "='" + item.getType().name() + "' and " + KEY_POTION_DATA + "='" + ItemDB.serializePotionData(item) + "' and " + KEY_ENCHANTMENT_DATA + "='null'";
    	executeUpdate(sql);
    }
    
	/*public void updatePriceEnchanted(int id, double price){
    	String sql = "update vsx_estock set " + KEY_PRICE + "="+price+" where " + KEY_ID + "=" + id;
    	executeUpdate(sql);
    }*/
    
	public void logTransaction(Transaction t){
    	String sql = "insert into vsx_transactions(" + KEY_MATERIAL + "," + KEY_POTION_DATA + "," + KEY_ENCHANTMENT_DATA + "," + KEY_QUANTITY + "," + KEY_COST + "," + KEY_TIMESTAMP + "," + KEY_BUYER_UUID + "," + KEY_SELLER_UUID + ") values('" + t.getItem().getType().name() + "'," + ItemDB.serializePotionData(t.getItem()) + ",'" + ItemDB.serializeEnchantmentData(t.getItem()) + "'," + t.getItem().getAmount() + "," + t.getCost() + "," + t.getTimestamp() + ",'" + t.getBuyerUUID().toString() + "','" + t.getSellerUUID().toString() + "')";
    	executeUpdate(sql);
    }

    public List<Offer> getBestPrices(){
    	String sql = "select f.* from (select " + KEY_MATERIAL + ",min(" + KEY_PRICE + ") as min" + KEY_PRICE + " from vsx_stock group by " + KEY_MATERIAL + ") as x inner join vsx_stock as f on f." + KEY_MATERIAL + " = x." + KEY_MATERIAL + " and f." + KEY_PRICE + " = x.min" + KEY_PRICE + "";
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
    	String sql = "select * from vsx_stock where " + KEY_UUID + "='" + vendorUUID.toString() +  "' where " + KEY_ENCHANTMENT_DATA + "='null'";
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
    	String sql = "select * from vsx_estock where " + KEY_UUID + "='" + vendorUUID.toString() +  "' where NOT " + KEY_ENCHANTMENT_DATA + "='null'";
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
    	String sql = "select * from vsx_transactions order by " + KEY_ID + " desc";
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
		String sql = "select * from vsx_transactions where " + KEY_SELLER_UUID + "='" + targetUUID.toString() +"' OR " + KEY_BUYER_UUID + "='" + targetUUID.toString() +"' order by " + KEY_ID + "";
		try(Connection connection = ds.getDataSource().getConnection();
	    	PreparedStatement stmt = connection.prepareStatement(sql);
	    	ResultSet result = stmt.executeQuery()){
			return Transaction.listTransactions(result);
		} catch(SQLException e){
			cm.logError(e.getMessage(), true);
			return null;
		}
	}

	public List<Offer> getPrices(ItemStack item){
    	String sql = "select * from vsx_stock where " + KEY_MATERIAL + "='" + item.getType().name() + "' AND " + KEY_POTION_DATA + "='" + ItemDB.serializePotionData(item) + "' order by " + KEY_PRICE + " asc limit 0,10";
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
