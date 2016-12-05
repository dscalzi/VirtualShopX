package com.dscalzi.virtualshop.managers;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.dbcp2.BasicDataSource;
import org.bukkit.inventory.ItemStack;
import com.dscalzi.virtualshop.VirtualShop;
import com.dscalzi.virtualshop.commands.Buy;
import com.dscalzi.virtualshop.commands.Cancel;
import com.dscalzi.virtualshop.commands.Sell;
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
		togglesKey.put(Reprice.class, "repriceconfirm");
		togglesKey.put(Cancel.class, "cancelconfirm");
	}
	
	public enum ConnectionType{
		SQLite, MYSQL;
	}
	
	private final ConnectionType type;
	
	private VirtualShop plugin;
	private BasicDataSource ds;
	private ConfigManager configM;
	private ChatManager cm;
	private UUIDManager uuidm;
	
	private DatabaseManager(VirtualShop plugin){
		this.plugin = plugin;
		this.configM = ConfigManager.getInstance();
		this.cm = ChatManager.getInstance();
		this.uuidm = UUIDManager.getInstance();
		
		if(configM.usingMySQL()){
			loadMySQL();
			this.type = ConnectionType.MYSQL;
		} else { 
			loadSQLite();
			this.type = ConnectionType.SQLite;
		}
		checkTables(this.type);
	}
	
	private void loadSQLite() {
		final String name = "VirtualShop";
		final String location = "plugins/VirtualShop/";
		
        this.ds = new BasicDataSource();
		try {
			Class.forName("org.sqlite.JDBC");
			ds.setDriverClassName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e){
			ChatManager.getInstance().logError("SQLite Driver not found. Shutting down!", true);
			plugin.getServer().getPluginManager().disablePlugin(plugin);
		}
		
		File folder = new File(location);
		if(!folder.exists()) folder.mkdir();
		
		File sqlFile = new File(folder.getAbsolutePath() + File.separator + name + ".db");
		
		ds.setUrl("jdbc:sqlite:" + sqlFile.toPath().toString());
		ds.setMaxIdle(0);
    }

    private void loadMySQL() {
    	this.ds = new BasicDataSource();
    	try {
			Class.forName("com.mysql.jdbc.Driver");
			ds.setDriverClassName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e){
			ChatManager.getInstance().logError("MySQL Driver not found. Shutting down!", true);
			plugin.getServer().getPluginManager().disablePlugin(plugin);
		}
    	
		ds.setUrl("jdbc:mysql://" + configM.mySQLHost() + ":" + configM.getPort() + "/" + configM.mySQLdatabase());
		ds.setUsername(configM.mySQLUserName());
		ds.setPassword(configM.mySQLPassword());
		
		ds.setMinIdle(5);
		ds.setMaxIdle(15);
		ds.setMaxOpenPreparedStatements(100);
    }
	
	public static void initialize(VirtualShop plugin){
		if(!initialized){
			instance = new DatabaseManager(plugin);
			initialized = true;
		}
	}
    
	public static DatabaseManager getInstance(){
		return DatabaseManager.instance;
	}
	
    public void terminate(){
        try {
			this.ds.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
    }
    
    private void checkTables(ConnectionType type){
    	String autoIncrement = type == ConnectionType.SQLite ? "" : " auto_increment"; 
		if(!checkTable("vshop_stock", type)){
			String query = "create table vshop_stock(`id` integer primary key" + autoIncrement + ",`damage` integer,`seller` varchar(80) not null,`item` integer not null, `price` double not null,`amount` integer not null, `uuid` varchar(80) not null)";
			createTable(query);
            cm.logInfo("Created vshop_stock table.");
		}
		if(!checkTable("vshop_transactions", type)){
			String query = "create table vshop_transactions(`id` integer primary key" + autoIncrement + ",`damage` integer not null, `buyer` varchar(20) not null,`seller` varchar(20) not null,`item` integer not null, `cost` double not null,`amount` integer not null, `buyer_uuid` varchar(80) not null, `seller_uuid` varchar(80) not null)";
			createTable(query);
			cm.logInfo("Created vshop_transaction table.");
		}
		if(!checkTable("vshop_toggles", type)){
			String query = "create table vshop_toggles(`id` integer primary key" + autoIncrement + ",`merchant` varchar(80) not null,`buyconfirm` bit not null,`sellconfirm` bit not null, `cancelconfirm` bit not null, `repriceconfirm` bit not null, `uuid` varchar(80) not null)";
			createTable(query);
			cm.logInfo("Created vshop_toggles table.");
		}
	}
    
    private boolean checkTable(String table, ConnectionType type){
    	if(type == ConnectionType.MYSQL){
	    	try(Connection connection = ds.getConnection()){
			    PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + table);
			    
			    ResultSet result = statement.executeQuery();
			    
			    if (result == null)
			    	return false;
			    if (result != null)
			    	return true;
			} catch (SQLException e) {
				if (e.getMessage().contains("exist")) {
					return false;
				} else {
					cm.logError("Error in SQL query: " + e.getMessage(), false);
				}
			}
	    	return false;
    	} else if(type == ConnectionType.SQLite){
    		try(Connection connection = ds.getConnection()){
    			DatabaseMetaData dbm = connection.getMetaData();
    			ResultSet tables = dbm.getTables(null, null, table, null);
    			if (tables.next())
    			  return true;
    			else
    			  return false;
    		} catch (SQLException e) {
    			cm.logError("Failed to check if table \"" + table + "\" exists: " + e.getMessage(), true);
    			return false;
    		}
    	}
    	return false;
    }
    
	private boolean createTable(String query) {
		try(Connection connection = ds.getConnection()){
			connection.prepareStatement(query).executeUpdate();
		    return true;
		} catch (SQLException e) {
			cm.logError(e.getMessage(), true);
			return false;
		}
	}
    
    /* Buy & Sell Confirmation Accessors */
    
    public int syncNameToUUID(){
    	Map<UUID, String> uuidNamePairs = new HashMap<UUID, String>();
    	
    	try(Connection connection = ds.getConnection()){
    		
	        ResultSet result = connection.prepareStatement("select * from vshop_toggles").executeQuery();
	        	
			while(result.next())
				uuidNamePairs.put(UUID.fromString(result.getString("uuid")), result.getString("merchant"));
			
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
	    		connection.prepareStatement("update vshop_toggles set merchant='" + entry.getValue() + "' where uuid='" + entry.getKey().toString() + "'").executeQuery();
	    		//Update stock table
	    		connection.prepareStatement("update vshop_stock set seller='" + entry.getValue() + "' where uuid='" + entry.getKey().toString() + "'").executeQuery();
	    		//Update transaction table
	    		connection.prepareStatement("update vshop_transactions set buyer='" + entry.getValue() + "' where buyer_uuid='" + entry.getKey().toString() + "'").executeQuery();
	    		connection.prepareStatement("update vshop_transactions set seller='" + entry.getValue() + "' where seller_uuid='" + entry.getKey().toString() + "'").executeQuery();
	    	}
	    	return uuidNamePairs.size();
    	} catch (SQLException e){
    		cm.logError(e.getMessage(), true);
    		return -1;
    	}
    }
    
    public Pair<Boolean, Integer> syncNameToUUID(UUID uuid){
    	String loggedName = "";
    	
    	try(Connection connection = ds.getConnection()){
	    	ResultSet result = connection.prepareStatement("select * from vshop_toggles where uuid='" + uuid.toString() + "'").executeQuery();
			if(!result.next()){
				return new Pair<Boolean, Integer>(false, 404);
			} else {
				loggedName = result.getString("merchant");
			}
			
	    	Optional<String> newName = uuidm.getNewPlayerName(uuid, loggedName);
	    	if(newName.isPresent()){
	    		//Update toggles table
	    		connection.prepareStatement("update vshop_toggles set merchant='" + newName.get() + "' where uuid='" + uuid.toString() + "'").executeQuery();
				//Update stock table
	    		connection.prepareStatement("update vshop_stock set seller='" + newName.get() + "' where uuid='" + uuid.toString() + "'").executeQuery();
				//Update transaction table'
	    		connection.prepareStatement("update vshop_transactions set buyer='" + newName.get() + "' where buyer_uuid='" + uuid.toString() + "'").executeQuery();
	    		connection.prepareStatement("update vshop_transactions set seller='" + newName.get() + "' where seller_uuid='" + uuid.toString() + "'").executeQuery();
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
    	try(Connection connection = ds.getConnection()){
	    	//Add columns
	    	connection.prepareStatement("ALTER TABLE vshop_toggles  ADD `uuid` varchar(80)").executeUpdate();
	    	connection.prepareStatement("ALTER TABLE vshop_stock  ADD `uuid` varchar(80)").executeUpdate();
	    	connection.prepareStatement("ALTER TABLE vshop_transactions  ADD `buyer_uuid` varchar(80)").executeUpdate();
	    	connection.prepareStatement("ALTER TABLE vshop_transactions  ADD `seller_uuid` varchar(80)").executeUpdate();
	    	
	    	List<String> names = new ArrayList<String>();
	    	ResultSet result = connection.prepareStatement("select * from vshop_toggles").executeQuery();
			while(result.next()){
				names.add(result.getString("merchant"));
			}
	    	
	    	int s = 0;
	    	int f = 0;
	    	
	    	for(String n : names){
	    		Optional<UUID> uuidOpt = uuidm.getPlayerUUID(n);
	    		if(!uuidOpt.isPresent()) {
	    			++f;
	    			continue;
	    		}
	    		UUID uuid = uuidOpt.get();
	    		//Update toggles table
	    		connection.prepareStatement("update vshop_toggles set uuid='" + uuid.toString() + "' where merchant='" + n + "'").executeUpdate();
				//Update stock table
	    		connection.prepareStatement("update vshop_stock set uuid='" + uuid.toString() + "' where seller='" + n + "'").executeUpdate();
				//Update transaction table'
	    		connection.prepareStatement("update vshop_transactions set buyer_uuid='" + uuid.toString() + "' where buyer='" + n + "'").executeUpdate();
	    		connection.prepareStatement("update vshop_transactions set seller_uuid='" + uuid.toString() + "' where seller='" + n + "'").executeUpdate();
				++s;
	    	}
	    	
	    	//Purge invalids.. no mercy.
	    	connection.prepareStatement("delete from vshop_stock where uuid = ''").executeUpdate();
	    	connection.prepareStatement("delete from vshop_toggles where uuid = ''").executeUpdate();
	    	connection.prepareStatement("delete from vshop_transactions where buyer_uuid = ''").executeUpdate();
	    	connection.prepareStatement("delete from vshop_transactions where seller_uuid = ''").executeUpdate();
	    	
	    	return new Pair<Integer, Integer>(s,f);
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    		return new Pair<Integer, Integer>(-1,-1);
    	}
    } 
    
    public boolean isPlayerInToggles(UUID merchantUUID){
    	try(Connection connection = ds.getConnection()){
	    	ResultSet result = connection.prepareStatement("select * from vshop_toggles where uuid='" + merchantUUID.toString() + "'").executeQuery();
	    	if(!result.next()) {
	    		return false;
	    	}
	    	else {
	    		if(result.getString("uuid").equalsIgnoreCase(merchantUUID.toString())) {
	    			return true;
	    		}
	    	}
	    	return false;
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    	}
    	return false;
    }
    
    public void addPlayerToToggles(UUID merchantUUID){
    	String merchant = DatabaseManager.DEFAULTNAME;
    	Optional<String> name = uuidm.getPlayerName(merchantUUID);
    	if(name.isPresent()) merchant = name.get();
    	try(Connection connection = ds.getConnection()){
    		connection.prepareStatement("insert into vshop_toggles(merchant,buyconfirm,sellconfirm,cancelconfirm,repriceconfirm,uuid) values('" + merchant + "',1,1,1,1,'" + merchantUUID.toString() + "')").executeUpdate();
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    	}
    }
    
    public void updateToggle(UUID merchantUUID, Class<? extends Confirmable> clazz, boolean value){
    	if(!togglesKey.containsKey(clazz))
    		return;
    	if(!isPlayerInToggles(merchantUUID))
    		addPlayerToToggles(merchantUUID);
    	
    	int dataval = value ? 1 : 0;
    	try(Connection connection = ds.getConnection()){
    		connection.prepareStatement("update vshop_toggles set " + togglesKey.get(clazz) + "=" + dataval + " where uuid='" + merchantUUID.toString() + "'").executeUpdate();
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    	}
    }
    
    public boolean getToggle(UUID merchantUUID, Class<? extends Confirmable> clazz){
    	if(!togglesKey.containsKey(clazz))
    		return false;
    	if(!isPlayerInToggles(merchantUUID))
    		addPlayerToToggles(merchantUUID);
    	
    	try(Connection connection = ds.getConnection()){
    		ResultSet result = connection.prepareStatement("select * from vshop_toggles where uuid='" + merchantUUID.toString() + "'").executeQuery();
    		result.next();    		
			return result.getBoolean(togglesKey.get(clazz));
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    		return true;
    	}
    }
    
    @SuppressWarnings("deprecation")
	public void addOffer(Offer offer){
    	try(Connection connection = ds.getConnection()){
    		connection.prepareStatement("insert into vshop_stock(seller,item,amount,price,damage,uuid) values('" +offer.getSeller() +"',"+ offer.getItem().getType().getId() + ","+offer.getItem().getAmount() +","+offer.getPrice()+"," + offer.getItem().getDurability()+",'"+offer.getSellerUUID().toString()+"')").executeUpdate();
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    	}
    }
    
    public List<Offer> getAllOffers(){
    	try(Connection connection = ds.getConnection()){
    		return Offer.listOffers(connection.prepareStatement("select * from vshop_stock order by price asc").executeQuery());
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    		return null;
    	}
    }

    @SuppressWarnings("deprecation")
	public List<Offer> getItemOffers(ItemStack item){
    	try(Connection connection = ds.getConnection()){
    		return Offer.listOffers(connection.prepareStatement("select * from vshop_stock where item=" + item.getTypeId()+ " and damage=" + item.getDurability() + " order by price asc").executeQuery());
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    		return null;
    	}
    }

    @SuppressWarnings("deprecation")
	public List<Offer> getSellerOffers(UUID merchantUUID, ItemStack item){
    	try(Connection connection = ds.getConnection()){
    		return Offer.listOffers(connection.prepareStatement("select * from vshop_stock where uuid = '" + merchantUUID.toString() + "' and item =" + item.getTypeId() + " and damage=" + item.getDurability()).executeQuery());
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    		return null;
    	}
    }

    @SuppressWarnings("deprecation")
	public void removeSellerOffers(UUID merchantUUID, ItemStack item){
    	try(Connection connection = ds.getConnection()){
    		connection.prepareStatement("delete from vshop_stock where uuid = '" + merchantUUID.toString() + "' and item =" + item.getTypeId() + " and damage = " + item.getDurability()).executeUpdate();
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    	}
    }

    public void deleteItem(int id){
    	try(Connection connection = ds.getConnection()){
    		connection.prepareStatement("delete from vshop_stock where id="+id).executeUpdate();
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    	}
	}

    public void updateQuantity(int id, int quantity){
    	try(Connection connection = ds.getConnection()){
    		connection.prepareStatement("update vshop_stock set amount="+quantity+" where id=" + id).executeUpdate();
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    	}
	}

    public void updatePrice(int id, double price){
    	try(Connection connection = ds.getConnection()){
    		connection.prepareStatement("update vshop_stock set price="+price+" where id=" + id).executeUpdate();
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    	}
    }
    @SuppressWarnings("deprecation")
	public void updatePrice(UUID merchantUUID, double price, ItemStack item){
    	try(Connection connection = ds.getConnection()){
    		connection.prepareStatement("update vshop_stock set price="+price+" where uuid='" + merchantUUID + "' and item =" + item.getTypeId() + " and damage = " + item.getDurability()).executeUpdate();
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    	}
    }
    
    @SuppressWarnings("deprecation")
	public void logTransaction(Transaction transaction){
    	try(Connection connection = ds.getConnection()){
    		connection.prepareStatement("insert into vshop_transactions(seller,buyer,item,amount,cost,damage,buyer_uuid,seller_uuid) values('" +transaction.getSeller() +"','"+ transaction.getBuyer() + "'," + transaction.getItem().getTypeId() + ","+ transaction.getItem().getAmount() +","+transaction.getCost()+","+transaction.getItem().getDurability()+",'"+transaction.getBuyerUUID().toString()+"','"+transaction.getSellerUUID().toString()+"')").executeUpdate();
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    	}
    }

    public List<Offer> getBestPrices(){
    	try(Connection connection = ds.getConnection()){
    		return Offer.listOffers(connection.prepareStatement("select f.* from (select item,min(price) as minprice from vshop_stock group by item) as x inner join vshop_stock as f on f.item = x.item and f.price = x.minprice").executeQuery());
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    		return null;
    	}
    }

    public List<Offer> searchBySeller(UUID merchantUUID){
    	try(Connection connection = ds.getConnection()){
    		return Offer.listOffers(connection.prepareStatement("select * from vshop_stock where uuid like '%" + merchantUUID.toString() +  "%'").executeQuery());
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    		return null;
    	}
    }

    public List<Transaction> getTransactions(){
    	try(Connection connection = ds.getConnection()){
    		return Transaction.listTransactions(connection.prepareStatement("select * from vshop_transactions order by id desc").executeQuery());
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    		return null;
    	}
    }

	public List<Transaction> getTransactions(UUID targetUUID){
		try(Connection connection = ds.getConnection()){
			return Transaction.listTransactions(connection.prepareStatement("select * from vshop_transactions where seller_uuid like '%" + targetUUID.toString() +"%' OR buyer_uuid like '%" + targetUUID.toString() +"%' order by id").executeQuery());
		} catch(SQLException e){
			cm.logError(e.getMessage(), true);
			return null;
		}
	}

    @SuppressWarnings("deprecation")
	public List<Offer> getPrices(ItemStack item){
    	try(Connection connection = ds.getConnection()){
    		return Offer.listOffers(connection.prepareStatement("select * from vshop_stock where item=" + item.getTypeId() + " AND damage=" + item.getDurability() + " order by price asc limit 0,10").executeQuery());
    	} catch(SQLException e){
    		cm.logError(e.getMessage(), true);
    		return null;
    	}
    }
	
}
