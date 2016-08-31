package com.dscalzi.virtualshop.managers;

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

import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.dscalzi.virtualshop.VirtualShop;
import com.dscalzi.virtualshop.objects.Offer;
import com.dscalzi.virtualshop.objects.Transaction;
import com.dscalzi.virtualshop.persistance.Database;
import com.dscalzi.virtualshop.persistance.MySQLDB;
import com.dscalzi.virtualshop.persistance.SQLiteDB;

import javafx.util.Pair;

public final class DatabaseManager {

	private static final String DEFAULTNAME = "sync_required";
	private static boolean initialized;
	private static DatabaseManager instance;
	
	@SuppressWarnings("unused")
	private VirtualShop plugin;
	private Database database;
	private ConfigManager configM;
	private ChatManager cm;
	private UUIDManager uuidm;
	
	private DatabaseManager(Plugin plugin){
		this.plugin = (VirtualShop)plugin;
		this.configM = ConfigManager.getInstance();
		this.cm = ChatManager.getInstance();
		this.uuidm = UUIDManager.getInstance();
		
		if(configM.usingMySQL()) loadMySQL();
        else loadSQLite();
	}
	
	private void loadSQLite() {
        this.database = new SQLiteDB();
        try {
            this.database.load();
        } catch (Exception e) {
            cm.logInfo("Fatal error.");
        }
    }

    private void loadMySQL() {
        this.database = new MySQLDB();
        try {
            this.database.load();
        } catch (Exception e) {
            loadSQLite();
        }
    }
	
	public static void initialize(Plugin plugin){
		if(!initialized){
			instance = new DatabaseManager(plugin);
			initialized = true;
		}
	}
    
	public static DatabaseManager getInstance(){
		return DatabaseManager.instance;
	}
	
    public void close() {
        this.database.unload();
    }
    
    /* Buy & Sell Confirmation Accessors */
    
    public int syncNameToUUID(){
    	Map<UUID, String> uuidNamePairs = new HashMap<UUID, String>();
    	String query = "select * from vshop_toggles";
    	ResultSet result = this.database.query(query);
    	try {
			while(result.next()){
				uuidNamePairs.put(UUID.fromString(result.getString("uuid")), result.getString("merchant"));
			}
		} catch (SQLException e) {
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
    		this.database.query("update vshop_toggles set merchant='" + entry.getValue() + "' where uuid='" + entry.getKey().toString() + "'");
    		//Update stock table
    		this.database.query("update vshop_stock set seller='" + entry.getValue() + "' where uuid='" + entry.getKey().toString() + "'");
    		//Update transaction table'
    		this.database.query("update vshop_transactions set buyer='" + entry.getValue() + "' where buyer_uuid='" + entry.getKey().toString() + "'");
    		this.database.query("update vshop_transactions set seller='" + entry.getValue() + "' where seller_uuid='" + entry.getKey().toString() + "'");
    	}
    	return uuidNamePairs.size();
    }
    
    public Pair<Boolean, Integer> syncNameToUUID(UUID uuid){
    	String loggedName = "";
    	String query = "select * from vshop_toggles where uuid='" + uuid.toString() + "'";
    	ResultSet result = this.database.query(query);
    	try {
			if(!result.next()){
				return new Pair<Boolean, Integer>(false, 404);
			} else {
				loggedName = result.getString("merchant");
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return new Pair<Boolean, Integer>(false, -1);
		}
    	Optional<String> newName = uuidm.getNewPlayerName(uuid, loggedName);
    	if(newName.isPresent()){
    		//Update toggles table
    		this.database.query("update vshop_toggles set merchant='" + newName.get() + "' where uuid='" + uuid.toString() + "'");
			//Update stock table
			this.database.query("update vshop_stock set seller='" + newName.get() + "' where uuid='" + uuid.toString() + "'");
			//Update transaction table'
			this.database.query("update vshop_transactions set buyer='" + newName.get() + "' where buyer_uuid='" + uuid.toString() + "'");
			this.database.query("update vshop_transactions set seller='" + newName.get() + "' where seller_uuid='" + uuid.toString() + "'");
			return new Pair<Boolean, Integer>(true, 0);
    	}
		return new Pair<Boolean, Integer>(false, 1);
    }
    
    @Deprecated
    public Pair<Integer, Integer> updateDatabase(){
    	
    	//Add columns
    	this.database.query("ALTER TABLE vshop_toggles  ADD `uuid` varchar(80)");
    	this.database.query("ALTER TABLE vshop_stock  ADD `uuid` varchar(80)");
    	this.database.query("ALTER TABLE vshop_transactions  ADD `buyer_uuid` varchar(80)");
    	this.database.query("ALTER TABLE vshop_transactions  ADD `seller_uuid` varchar(80)");
    	
    	List<String> names = new ArrayList<String>();
    	String query = "select * from vshop_toggles";
    	ResultSet result = this.database.query(query);
    	try {
			while(result.next()){
				names.add(result.getString("merchant"));
			}
		} catch (SQLException e) {
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
    		this.database.query("update vshop_toggles set uuid='" + uuid.toString() + "' where merchant='" + n + "'");
			//Update stock table
			this.database.query("update vshop_stock set uuid='" + uuid.toString() + "' where seller='" + n + "'");
			//Update transaction table'
			this.database.query("update vshop_transactions set buyer_uuid='" + uuid.toString() + "' where buyer='" + n + "'");
			this.database.query("update vshop_transactions set seller_uuid='" + uuid.toString() + "' where seller='" + n + "'");
			++s;
    	}
    	
    	//Purge invalids.. no mercy.
    	this.database.query("delete from vshop_stock where uuid = ''");
    	this.database.query("delete from vshop_toggles where uuid = ''");
    	this.database.query("delete from vshop_transactions where buyer_uuid = ''");
    	this.database.query("delete from vshop_transactions where seller_uuid = ''");
    	
    	return new Pair<Integer, Integer>(s,f);
    }
    
    public boolean isPlayerInToggles(UUID merchantUUID){
    	String query = "select * from vshop_toggles where uuid='" + merchantUUID.toString() + "'";
    	try {
    		ResultSet result = this.database.query(query);
    		if(!result.next()) {
    			return false;
    		}
    		else {
    			if(result.getString("uuid").equalsIgnoreCase(merchantUUID.toString())) {
    				return true;
    			}
    		}
    		return false;
		}catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
    }
    
    public void addPlayerToToggles(UUID merchantUUID){
    	String merchant = DatabaseManager.DEFAULTNAME;
    	Optional<String> name = uuidm.getPlayerName(merchantUUID);
    	if(name.isPresent()) merchant = name.get();
    	String query = "insert into vshop_toggles(merchant,buyconfirm,sellconfirm,updateconfirm,uuid) values('" + merchant + "',1,1,1,'" + merchantUUID.toString() + "')";
    	this.database.query(query);
    }
    
    public void updateSellToggle(UUID merchantUUID, boolean value){
    	if(!isPlayerInToggles(merchantUUID))
    		addPlayerToToggles(merchantUUID);
    	int dataval = 0;
    	if(value)
    		dataval = 1;
    	String query = "update vshop_toggles set sellconfirm=" + dataval + " where uuid='" + merchantUUID.toString() + "'";
		this.database.query(query);
    }
    
    public void updateBuyToggle(UUID merchantUUID, boolean value){
    	if(!isPlayerInToggles(merchantUUID))
    		addPlayerToToggles(merchantUUID);
    	int dataval = 0;
    	if(value)
    		dataval = 1;
    	String query = "update vshop_toggles set buyconfirm=" + dataval + " where uuid='" + merchantUUID.toString() + "'";
		this.database.query(query);
    }
    
    public void updateUpdateToggle(UUID merchantUUID, boolean value){
    	if(!isPlayerInToggles(merchantUUID))
    		addPlayerToToggles(merchantUUID);
    	int dataval = 0;
    	if(value)
    		dataval = 1;
    	String query = "update vshop_toggles set updateconfirm=" + dataval + " where uuid='" + merchantUUID.toString() + "'";
    	this.database.query(query);
    }
    
    public boolean getSellToggle(UUID merchantUUID){
    	if(!isPlayerInToggles(merchantUUID))
    		addPlayerToToggles(merchantUUID);
    	String query = "select * from vshop_toggles where uuid='" + merchantUUID.toString() + "'";
    	try {
    		ResultSet result = this.database.query(query);
    		result.next();    		
			return result.getBoolean("sellconfirm");
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
    }
    
    public boolean getBuyToggle(UUID merchantUUID){
    	if(!isPlayerInToggles(merchantUUID))
    		addPlayerToToggles(merchantUUID);
    	String query = "select * from vshop_toggles where uuid='" + merchantUUID.toString() + "'";
    	try {
    		ResultSet result = this.database.query(query);
    		result.next();
			return result.getBoolean("buyconfirm");
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
    }
    
    public boolean getUpdateToggle(UUID merchantUUID){
    	if(!isPlayerInToggles(merchantUUID))
    		addPlayerToToggles(merchantUUID);
    	String query = "select * from vshop_toggles where uuid='" + merchantUUID.toString() + "'";
    	try{
    		ResultSet result = this.database.query(query);
    		result.next();
    		return result.getBoolean("updateconfirm");
    	} catch (SQLException e){
    		e.printStackTrace();
    		return false;
    	}
    }
    
    public void addOffer(Offer offer){
		@SuppressWarnings("deprecation")
		String query = "insert into vshop_stock(seller,item,amount,price,damage,uuid) values('" +offer.getSeller() +"',"+ offer.getItem().getType().getId() + ","+offer.getItem().getAmount() +","+offer.getPrice()+"," + offer.getItem().getDurability()+",'"+offer.getSellerUUID().toString()+"')";
		this.database.query(query);
    }
    
    public List<Offer> getAllOffers(){
    	String query = "select * from vshop_stock order by price asc";
    	return Offer.listOffers(this.database.query(query));
    }

    public List<Offer> getItemOffers(ItemStack item) {
		@SuppressWarnings("deprecation")
		String query = "select * from vshop_stock where item=" + item.getTypeId()+ " and damage=" + item.getDurability() + " order by price asc";
		return Offer.listOffers(this.database.query(query));
	}

    public List<Offer> getSellerOffers(UUID merchantUUID, ItemStack item) {
		@SuppressWarnings("deprecation")
		String query = "select * from vshop_stock where uuid = '" + merchantUUID.toString() + "' and item =" + item.getTypeId() + " and damage=" + item.getDurability();
		return Offer.listOffers(this.database.query(query));
	}

    public void removeSellerOffers(UUID merchantUUID, ItemStack item) {
		@SuppressWarnings("deprecation")
		String query = "delete from vshop_stock where uuid = '" + merchantUUID.toString() + "' and item =" + item.getTypeId() + " and damage = " + item.getDurability();
		this.database.query(query);
	}

    public void deleteItem(int id) {
		String query = "delete from vshop_stock where id="+id;
		this.database.query(query);
	}

    public void updateQuantity(int id, int quantity) {
		String query = "update vshop_stock set amount="+quantity+" where id=" + id;
		this.database.query(query);
	}

    public void updatePrice(int id, double price) {
    	String query = "update vshop_stock set price="+price+" where id=" + id;
    	this.database.query(query);
    }
    public void updatePrice(UUID merchantUUID, double price){
    	String query = "update vshop_stock set price="+price+" where uuid='" + merchantUUID + "'";
    	this.database.query(query);
    }
    
    public void logTransaction(Transaction transaction) {
		@SuppressWarnings("deprecation")
		String query = "insert into vshop_transactions(seller,buyer,item,amount,cost,damage,buyer_uuid,seller_uuid) values('" +transaction.getSeller() +"','"+ transaction.getBuyer() + "'," + transaction.getItem().getTypeId() + ","+ transaction.getItem().getAmount() +","+transaction.getCost()+","+transaction.getItem().getDurability()+",'"+transaction.getBuyerUUID().toString()+"','"+transaction.getSellerUUID().toString()+"')";
		this.database.query(query);
	}

    public List<Offer> getBestPrices() {
        String query = "select f.* from (select item,min(price) as minprice from vshop_stock group by item) as x inner join vshop_stock as f on f.item = x.item and f.price = x.minprice";
        ResultSet result = this.database.query(query);
		List<Offer> prices = Offer.listOffers(result);
		return prices;
    }

    public List<Offer> searchBySeller(UUID merchantUUID) {
    	String query = "select * from vshop_stock where uuid like '%" + merchantUUID.toString() +  "%'";
    	ResultSet result = this.database.query(query);
		List<Offer> prices = Offer.listOffers(result);
		return prices;
    }

    public List<Transaction> getTransactions() {
		return Transaction.listTransactions(this.database.query("select * from vshop_transactions order by id desc"));
	}

	public List<Transaction> getTransactions(UUID targetUUID) {
		return Transaction.listTransactions(this.database.query("select * from vshop_transactions where seller_uuid like '%" + targetUUID.toString() +"%' OR buyer_uuid like '%" + targetUUID.toString() +"%' order by id"));
	}

    public List<Offer> getPrices(ItemStack item) {
		@SuppressWarnings("deprecation")
		String query = "select * from vshop_stock where item=" + item.getTypeId() + " AND damage=" + item.getDurability() + " order by price asc limit 0,10";
		ResultSet result = this.database.query(query);
		List<Offer> prices = Offer.listOffers(result);
		return prices;
	}
	
}
