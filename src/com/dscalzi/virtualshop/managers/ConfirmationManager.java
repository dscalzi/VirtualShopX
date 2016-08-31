package com.dscalzi.virtualshop.managers;

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
	private Map<Pair<Player, Class<? extends Confirmable>>, VsDataCache> confirmations;
	
	private ConfirmationManager(Plugin plugin){
		this.plugin = (VirtualShop)plugin;
		this.assignVars();
	}
	
	private void assignVars(){
		this.confirmations = new HashMap<Pair<Player, Class<? extends Confirmable>>, VsDataCache>();
	}
	
	public static void initialize(Plugin plugin){
		if(!initialized){
			Optional<ConfirmationManager> e = ConfirmationManager.deserialize(plugin);
			instance = e.isPresent() ? e.get() : new ConfirmationManager(plugin);
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
	
	/* Functions */
	
	public void register(Class<? extends Confirmable> command, Player player, VsDataCache data){
		if(data == null) throw new IllegalArgumentException();
		Pair<Player, Class<? extends Confirmable>> key = new Pair<Player, Class<? extends Confirmable>>(player, command);
		if(confirmations.containsKey(key)){
			confirmations.remove(key);
		}
		confirmations.put(key, data);
	}
	
	public boolean unregister(Class<? extends Confirmable> command, Player player){
		Pair<Player, Class<? extends Confirmable>> key = new Pair<Player, Class<? extends Confirmable>>(player, command);
		if(confirmations.containsKey(key)){
			confirmations.remove(key);
			return true;
		}
		return false;
	}
	
	private void clean(){
		if(confirmations.size() < 1) return;
		
		long systemTime = System.currentTimeMillis();
		
		Iterator<Entry<Pair<Player, Class<? extends Confirmable>>, VsDataCache>> it = confirmations.entrySet().iterator();
		while(it.hasNext()){
			Entry<Pair<Player, Class<? extends Confirmable>>, VsDataCache> entry = it.next();
			if(systemTime - entry.getValue().getTransactionTime() > 15000){
				it.remove();
			}
		}
	}
	
	public void serialize(){
		this.clean();
		if(confirmations.size() < 1) return; //No need to serialize nothing.
		try {
			FileOutputStream fOut = new FileOutputStream(plugin.getDataFolder() + "confirmations.ser");
			ObjectOutputStream oOut = new ObjectOutputStream(fOut);
			oOut.writeObject(this);
			oOut.close();
			fOut.close();
		} catch (IOException e) {
			plugin.getLogger().severe("Serialization Error, discarding existing confirmations.");
		}
	}
	
	private static Optional<ConfirmationManager> deserialize(Plugin plugin){
		
		Optional<ConfirmationManager> e = Optional.empty();
		
		 try {
	         FileInputStream fIn = new FileInputStream(plugin.getDataFolder() + "confirmations.ser");
	         ObjectInputStream oIn = new ObjectInputStream(fIn);
	         e = Optional.of((ConfirmationManager) oIn.readObject());
	         oIn.close();
	         fIn.close();
	      } catch(IOException i) {
	         return e;
	      } catch(ClassNotFoundException c) {
	         plugin.getLogger().severe("ConfirmationManager class not found during deserialization!");
	         return e;
	      }
		 
		 return e;
	}
	
}
