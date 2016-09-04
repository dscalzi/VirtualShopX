package com.dscalzi.virtualshop.managers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.dscalzi.virtualshop.VirtualShop;
import com.dscalzi.virtualshop.objects.Confirmable;
import com.dscalzi.virtualshop.objects.VsDataCache;

import javafx.util.Pair;

public class ConfirmationManager implements Serializable{

	private static final long serialVersionUID = 6301284628063131606L;
	
	private static transient boolean initialized;
	private static transient ConfirmationManager instance;
	
	private transient VirtualShop plugin;
	private transient ConfigManager configM;
	private Map<Pair<UUID, Class<? extends Confirmable>>, VsDataCache> confirmations;
	
	private ConfirmationManager(Plugin plugin){
		this.plugin = (VirtualShop)plugin;
		this.configM = ConfigManager.getInstance();
		this.assignVars();
	}
	
	private void assignVars(){
		this.confirmations = new HashMap<Pair<UUID, Class<? extends Confirmable>>, VsDataCache>();
	}
	
	public static void initialize(Plugin plugin){
		if(!initialized){
			Optional<ConfirmationManager> e = ConfirmationManager.deserialize(plugin);
			if(e.isPresent()){
				instance = e.get();
				instance.assignPlugin(plugin);
				instance.configM = ConfigManager.getInstance();
				instance.setUp();
			} else
				instance = new ConfirmationManager(plugin);
			initialized = true;
		}
	}
	
	public static boolean refresh(){
		if(!initialized) return false;
		getInstance().assignVars();
		return true;
	}
	
	public static ConfirmationManager getInstance(){
		return ConfirmationManager.instance;
	}
	
	private void assignPlugin(Plugin p){
		this.plugin = (VirtualShop) p;
	}
	
	/* Functions */
	
	public void register(Class<? extends Confirmable> command, Player player, VsDataCache data){
		if(data == null) throw new IllegalArgumentException();
		Pair<UUID, Class<? extends Confirmable>> key = new Pair<UUID, Class<? extends Confirmable>>(player.getUniqueId(), command);
		if(confirmations.containsKey(key)){
			confirmations.remove(key);
		}
		confirmations.put(key, data);
	}
	
	public boolean unregister(Class<? extends Confirmable> command, Player player){
		Pair<UUID, Class<? extends Confirmable>> key = new Pair<UUID, Class<? extends Confirmable>>(player.getUniqueId(), command);
		if(confirmations.containsKey(key)){
			confirmations.remove(key);
			return true;
		}
		return false;
	}
	
	public VsDataCache retrieve(Class<? extends Confirmable> command, Player player){
		Pair<UUID, Class<? extends Confirmable>> key = new Pair<UUID, Class<? extends Confirmable>>(player.getUniqueId(), command);
		return confirmations.get(key);
	}
	
	public boolean contains(Class<? extends Confirmable> command, Player player){
		Pair<UUID, Class<? extends Confirmable>> key = new Pair<UUID, Class<? extends Confirmable>>(player.getUniqueId(), command);
		if(confirmations.containsKey(key)) return true;
		return false;
	}
	
	private void clean(){
		if(confirmations.size() < 1) return;
		
		long systemTime = System.currentTimeMillis();
		
		Iterator<Entry<Pair<UUID, Class<? extends Confirmable>>, VsDataCache>> it = confirmations.entrySet().iterator();
		while(it.hasNext()){
			Entry<Pair<UUID, Class<? extends Confirmable>>, VsDataCache> entry = it.next();
			if(systemTime - entry.getValue().getTransactionTime() > configM.getConfirmationTimeout(entry.getKey().getValue())){
				it.remove();
			}
		}
	}
	
	public void serialize(){
		this.clean();
		if(confirmations.size() < 1) return; //No need to serialize nothing.
		for(Map.Entry<Pair<UUID, Class<? extends Confirmable>>, VsDataCache> entry : confirmations.entrySet()){
			entry.getValue().serialize();
		}
		try {
			FileOutputStream fOut = new FileOutputStream(plugin.getDataFolder() + "/confirmations.ser");
			ObjectOutputStream oOut = new ObjectOutputStream(fOut);
			oOut.writeObject(this);
			oOut.close();
			fOut.close();
		} catch (IOException e) {
			e.printStackTrace();
			plugin.getLogger().severe("Serialization Error, discarding existing confirmations.");
		}
	}
	
	private static Optional<ConfirmationManager> deserialize(Plugin plugin){
		
		Optional<ConfirmationManager> e = Optional.empty();
		
		 try {
	         FileInputStream fIn = new FileInputStream(plugin.getDataFolder() + "/confirmations.ser");
	         ObjectInputStream oIn = new ObjectInputStream(fIn);
	         e = Optional.of((ConfirmationManager) oIn.readObject());
	         oIn.close();
	         fIn.close();
	         File f = new File(plugin.getDataFolder() + "/confirmations.ser");
	         f.delete();
	      } catch(IOException i) {
	         return e;
	      } catch(ClassNotFoundException c) {
	         plugin.getLogger().severe("ConfirmationManager class not found during deserialization!");
	         return e;
	      }
		 
		 return e;
	}
	
	/**
	 * Deserialize the VsDataCaches
	 */
	private void setUp(){
		for(Map.Entry<Pair<UUID, Class<? extends Confirmable>>, VsDataCache> entry : confirmations.entrySet()){
			entry.getValue().deserialize();
		}
	}
}
