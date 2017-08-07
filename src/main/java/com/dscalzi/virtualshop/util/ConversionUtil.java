package com.dscalzi.virtualshop.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

public class ConversionUtil {
	
	public static void main(String[] args) {
		try {
			convert();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//fixSplash();
		//generateEveryPotion();
		System.out.println("Done");
		//int metadata = 64 >> 32 | (0 << 5) | (0 << 6) | (0 << 13) | (1 << 14);
		//int target = 16384;//16416;
		//System.out.println("target  " + target + "   got  " + metadata);
	}
	
	public static void generateEveryPotion() {
		List<Material> types = new ArrayList<Material>(Arrays.asList(Material.POTION, Material.SPLASH_POTION));
		for(Material m : types) {
			for(PotionType t : PotionType.values()) {
				try {
					ItemStack i = new ItemStack(m);
					PotionMeta meta = (PotionMeta)i.getItemMeta();
					meta.setBasePotionData(new PotionData(t, false, false));
					i.setItemMeta(meta);
					System.out.println("-------- " + m.name() + " " + t.name());
					System.out.println("Normal: " + metaCalculation(i, false));
					if(t.isExtendable()) {
					meta.setBasePotionData(new PotionData(t, true, false));
						i.setItemMeta(meta);
						System.out.println("Extended: " + metaCalculation(i, false));
					}
					if(t.isUpgradeable()) {
						System.out.println("-------- " + m.name() + " " + t.name() + " (II)");
						meta.setBasePotionData(new PotionData(t, false, true));
						i.setItemMeta(meta);
						System.out.println("Normal: " + metaCalculation(i, false));
					}
				} catch (Exception e) {
					//Do nothing
				}
			}
		}
	}
	
	public static ItemStack getPotionFromMeta(int metaId) {
		List<Material> types = new ArrayList<Material>(Arrays.asList(Material.POTION, Material.SPLASH_POTION));
		for(Material m : types) {
			for(PotionType t : PotionType.values()) {
				try {
					ItemStack i = new ItemStack(m);
					PotionMeta meta = (PotionMeta)i.getItemMeta();
					meta.setBasePotionData(new PotionData(t, false, false));
					i.setItemMeta(meta);
					if(metaCalculation(i, false) == metaId) {
						return i;
					}
					if(t.isExtendable()) {
						meta.setBasePotionData(new PotionData(t, true, false));
						i.setItemMeta(meta);
						if(metaCalculation(i, false) == metaId) {
							return i;
						}
					}
					if(t.isUpgradeable()) {
						meta.setBasePotionData(new PotionData(t, false, true));
						i.setItemMeta(meta);
						if(metaCalculation(i, false) == metaId) {
							return i;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}
	
	public static PotionType mapEssentialsToSpigot(String essPotionType) {
		switch(essPotionType) {
		case "REGENERATION":
			return PotionType.REGEN;
		case "SWIFTNESS":
			return PotionType.SPEED;
		case "HEALING":
			return PotionType.INSTANT_HEAL;
		case "STRENGTH":
			return PotionType.STRENGTH;
		case "LEAPING":
			return PotionType.JUMP;
		case "POISON":
			return PotionType.POISON;
		case "FIRE_RESISTANCE":
			return PotionType.FIRE_RESISTANCE;
		case "INVISIBILITY":
			return PotionType.INVISIBILITY;
		case "SLOWNESS":
			return PotionType.SLOWNESS;
		case "WATER_BREATHING":
			return PotionType.WATER_BREATHING;
		case "NIGHT_VISION":
			return PotionType.NIGHT_VISION;
		case "LUCK":
			return PotionType.LUCK;
		case "WEAKNESS":
			return PotionType.WEAKNESS;
		case "HARMING":
			return PotionType.INSTANT_DAMAGE;
		default:
			return PotionType.WATER;
		}
	}
	
	public static int metaCalculation(ItemStack i, boolean dualBit) {
		Material type = i.getType();
		if(type != Material.POTION && type != Material.SPLASH_POTION) {
			return -1;
		}
		PotionMeta meta = (PotionMeta)i.getItemMeta();
		PotionData data = meta.getBasePotionData();
		
		int tier = data.isUpgraded() ? 1 : 0;
		int extended = data.isExtended() ? 1 : 0;
		int drinkable = type == Material.POTION ? 1 : 0;
		int splash = type == Material.SPLASH_POTION ? 1 : 0;
		int effect = -1;
		boolean noEffect = false;
		boolean dualBitPoss = false;
		switch(data.getType()) {
		case WATER:
			effect = 0;
			noEffect = true;
			break;
		case AWKWARD:
			effect = 16;
			noEffect = true;
			break;
		case THICK:
			effect = 32;
			noEffect = true;
			break;
		case MUNDANE:
			effect = 64;
			noEffect = true;
			break;
		case UNCRAFTABLE:
			effect = 373;
			noEffect = true;
			break;
		case REGEN:
			effect = 1;
			dualBitPoss = true;
			break;
		case SPEED:
			effect = 2;
			dualBitPoss = true;
			break;
		case FIRE_RESISTANCE:
			effect = 3;
			break;
		case POISON:
			effect = 4;
			dualBitPoss = true;
			break;
		case INSTANT_HEAL:
			effect = 5;
			break;
		case NIGHT_VISION:
			effect = 6;
			break;
		case WEAKNESS:
			effect = 8;
			break;
		case STRENGTH:
			effect = 9;
			dualBitPoss = true;
			break;
		case SLOWNESS:
			effect = 10;
			break;
		case JUMP:
			effect = 11;
			break;
		case INSTANT_DAMAGE:
			effect = 12;
			break;
		case WATER_BREATHING:
			effect = 13;
			break;
		case INVISIBILITY:
			effect = 14;
			break;
		case LUCK:
			effect = -1;
			break;
		}
		if(effect == -1) {
			return -1;
		}
		
		if(noEffect) {
			if(type == Material.SPLASH_POTION) {
				return effect >> 32 | (0 << 5) | (0 << 6) | (0 << 13) | (1 << 14);
			} else {
				return effect;
			}
		}
		
		if(dualBitPoss && tier == 1 && dualBit) {
			return 96 + (effect | (0 << 5) | (extended << 6) | (drinkable << 13) | (splash << 14));
		}
		
		int metadata = effect | (tier << 5) | (extended << 6) | (drinkable << 13) | (splash << 14);
		
		return metadata;
	}
	
	public static boolean isPotionable(ItemStack i) {
		return i.getType() == Material.POTION || i.getType() == Material.SPLASH_POTION || i.getType() == Material.LINGERING_POTION || i.getType() == Material.TIPPED_ARROW;
	}
	
	public static ItemStack resolveEssentialsPotion(String identifierDIRTY, Material type) {
		try {
			String identifier = identifierDIRTY.replaceAll("[\\{\\}]", "").split(":")[1].replace("\"", "").toUpperCase();
			boolean isStrong = identifier.startsWith("STRONG");
			boolean isExtended = identifier.startsWith("LONG");
			if(isStrong || isExtended) {
				identifier = identifier.substring(identifier.indexOf("_") + 1);
			}
			ItemStack item = new ItemStack(type);
			PotionMeta m = (PotionMeta)item.getItemMeta();
			m.setBasePotionData(new PotionData(mapEssentialsToSpigot(identifier), isExtended, isStrong));
			item.setItemMeta(m);
			return item;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static boolean dualBitException(int metaId, JsonObject potionMeta) {
		
		switch(metaId) {
		case 8289:
		case 16481:
			potionMeta.addProperty("effect", PotionType.REGEN.name());
			potionMeta.addProperty("extended", true);
			potionMeta.addProperty("upgraded", true);
			return true;
		case 8290:
		case 16482:
			potionMeta.addProperty("effect", PotionType.SPEED.name());
			potionMeta.addProperty("extended", true);
			potionMeta.addProperty("upgraded", true);
			return true;
		case 8292:
		case 16484:
			potionMeta.addProperty("effect", PotionType.POISON.name());
			potionMeta.addProperty("extended", true);
			potionMeta.addProperty("upgraded", true);
			return true;
		case 8297:
		case 16489:
			potionMeta.addProperty("effect", PotionType.STRENGTH.name());
			potionMeta.addProperty("extended", true);
			potionMeta.addProperty("upgraded", true);
			return true;
		}
		
		return false;
	}

	public static void fixSplash() {
		File file = new File("src/main/resources/items.csv");
		//File file = new File("src/main/resources/items.csv");
		System.out.println(file.getAbsolutePath());
		//File target = new File("src/main/resources/items.json");
		File target = new File("src/main/resources/itemsNew.csv");
		
		String n = "";
		int lastLine = -50;
		
		try(FileReader reader = new FileReader(file);
			BufferedReader rx = new BufferedReader(reader)){
			
			for (int i = 0; rx.ready(); i++){
				try {
					String line = rx.readLine().trim();
					if (line.startsWith("#")) {
						n += line + "\n";
						continue;
					}
						
					String[] parts = line.split(",");
					if (parts.length < 3) {
						System.out.println("Issue on line " + 1);
						continue;
					}
					
					if(parts[0].toLowerCase().contains("spl") && parts[1].equals("373")) {
						parts[1] = "438";
						n += String.join(",", parts) + "\n";
						lastLine = i;
					} else if(i-lastLine == 1 && parts[0].toLowerCase().startsWith("sp") && parts[1].equals("373")) {
						parts[1] = "438";
						n += String.join(",", parts) + "\n";
						System.out.println(parts[0]);
					} else {
						n += line + "\n";
					}
					
				} catch (Exception ex){
					ex.printStackTrace();
					System.out.println("Error parsing items.csv on line " + i);
				}
			}
			
			Files.write(target.toPath(), n.getBytes());
			
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
	}
	
	/**
	 * TODO
	 * Mundane potions are improperly converted.
	 */
	public static void convert() throws FileNotFoundException, IOException {
		File file = new File("plugins/VirtualShop/items.csv");
		//File file = new File("src/main/resources/items.csv");
		System.out.println(file.getAbsolutePath());
		//File target = new File("src/main/resources/items.json");
		File target = new File("plugins/VirtualShop/items.json");
		
		try(FileReader reader = new FileReader(file);
			BufferedReader rx = new BufferedReader(reader)){
			
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			JsonObject obj = new JsonObject();
			obj.addProperty("version", "AventiumSoftworks");
			obj.addProperty("notice", "If you change this file, it will not be automatically updated after the next release.");
			JsonArray itemsArray = new JsonArray();

			JsonObject container = new JsonObject();
			JsonArray aliases = new JsonArray();
			
			boolean first = true;
			
			for (int i = 0; rx.ready(); i++){
				try {
					String line = rx.readLine().trim().toLowerCase();
					if (line.startsWith("#")) {
						continue;
					}
						
					String[] parts = line.split(",");
					if (parts.length < 3) {
						System.out.println("Issue on line " + 1);
						continue;
					}
					
					@SuppressWarnings("deprecation")
					ItemStack iStack = new ItemStack(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
					
					if(container.has("material") && container.get("material").getAsString().equals(iStack.getType().name()) &&
					   container.has("legacyData") && container.get("legacyData").getAsString().equals(parts[2]) &&
					   (!isPotionable(iStack) || parts.length < 4)){
						aliases.add(parts[0]);
						continue;
					}
					if(parts.length >= 4 && parts[3].startsWith("*")) {
						aliases.add(parts[0]);
						continue;
					} 
					if(!first) {
						container.add("aliases", aliases);
						itemsArray.add(container);
						aliases = new JsonArray();
						container = new JsonObject();
					}
					first = false;
					container.addProperty("material", iStack.getType().name());
					container.addProperty("legacyID", parts[1]);
					container.addProperty("legacyData", parts[2]);
					aliases.add(parts[0]);
					if(isPotionable(iStack)) {
						JsonObject potionMeta = new JsonObject();
						ItemStack potion = null;
						if(iStack.getType() == Material.POTION || iStack.getType() == Material.SPLASH_POTION) {
							potion = getPotionFromMeta(Short.parseShort(parts[2]));
						}
						if(potion == null) {
							if(parts.length >= 4) {
								potion = resolveEssentialsPotion(parts[3], iStack.getType());
							}
						}
						if(dualBitException(Integer.parseInt(parts[2]), potionMeta)){
							
						} else if(potion == null) {
							potionMeta.addProperty("value", "NON_EXISTANT");
						} else {
							PotionData d = ((PotionMeta)potion.getItemMeta()).getBasePotionData();
							potionMeta.addProperty("effect", d.getType().name());
							potionMeta.addProperty("extended", d.isExtended());
							potionMeta.addProperty("upgraded", d.isUpgraded());
						}
						container.add("potionMeta", potionMeta);
					}
				} catch (Exception ex){
					ex.printStackTrace();
					System.out.println("Error parsing items.csv on line " + i);
				}
			}
			try(JsonWriter writer = gson.newJsonWriter(new FileWriter(target))){
				obj.add("items", itemsArray);
				gson.toJson(obj, writer);
			}
		}
	}
	
}
