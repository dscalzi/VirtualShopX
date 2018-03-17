/*
 * VirtualShopX
 * Copyright (C) 2015-2018 Daniel D. Scalzi
 * See LICENSE for license information.
 */
package com.dscalzi.virtualshopx.util;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public final class UUIDUtil {
	
	public static UUID formatFromInput(String uuid) throws IllegalArgumentException{
		if(uuid == null) throw new IllegalArgumentException();
		uuid = uuid.trim();
		return uuid.length() == 32 ? fromTrimmed(uuid.replaceAll("-", "")) : UUID.fromString(uuid);
	}
	
	public static UUID fromTrimmed(String trimmedUUID) throws IllegalArgumentException{
		if(trimmedUUID == null) throw new IllegalArgumentException();
		StringBuilder builder = new StringBuilder(trimmedUUID.trim());
		/* Backwards adding to avoid index adjustments */
		try {
			builder.insert(20, "-");
			builder.insert(16, "-");
			builder.insert(12, "-");
			builder.insert(8, "-");
		} catch (StringIndexOutOfBoundsException e){
			throw new IllegalArgumentException();
		}
		
		return UUID.fromString(builder.toString());
	}
	
	public static Optional<String> getNewPlayerName(UUID uuid, String oldName){
		String currentName = Bukkit.getOfflinePlayer(uuid).getName();
		if(currentName == null) return Optional.empty();
		return (currentName.equals(oldName)) ? Optional.empty() : Optional.of(currentName);
	}
	
	public static Optional<String> getPlayerName(UUID uuid){
		String name = Bukkit.getOfflinePlayer(uuid).getName();
		return (name == null) ? Optional.empty() : Optional.of(name);
	}
	
	public static Optional<OfflinePlayer> getOfflinePlayer(UUID uuid){
		OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
		return (p == null) ? Optional.empty() : Optional.of(p);
	}
	
	@SuppressWarnings("deprecation")
	public static Optional<UUID> getPlayerUUID(String name){
		OfflinePlayer p = Bukkit.getOfflinePlayer(name);
		return (p == null) ? Optional.empty() : Optional.of(p.getUniqueId());
	}
	
}
