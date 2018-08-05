package com.dscalzi.virtualshopx.util;

import java.lang.reflect.Type;
import java.util.Map;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class EnchantmentTypeAdapter implements JsonSerializer<Map<Enchantment, Integer>>, JsonDeserializer<Enchantment> {
    
    @Override
    public Enchantment deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        
        String s = json.getAsString();
        String[] pts = s.split(":");
        
        return Enchantment.getByKey(NamespacedKey.minecraft(pts[1]));
    }

    @Override
    public JsonElement serialize(Map<Enchantment, Integer> src, Type typeOfSrc, JsonSerializationContext context) {
        
        JsonObject root = new JsonObject();
        for(Map.Entry<Enchantment, Integer> entry : src.entrySet()) {
            root.addProperty(entry.getKey().getKey().toString(), entry.getValue());
        }
        
        System.out.println("Hey its serializing ");
        
        return root;
    }

}
