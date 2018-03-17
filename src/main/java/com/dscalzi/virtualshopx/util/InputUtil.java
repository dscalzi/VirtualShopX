/*
 * VirtualShopX
 * Copyright (C) 2015-2018 Daniel D. Scalzi
 * See LICENSE for license information.
 */
package com.dscalzi.virtualshopx.util;

public class InputUtil {

	public static int parseInt(String s){
    	if(s.equalsIgnoreCase("all")) return Integer.MAX_VALUE;
    	
		try {
			int i = Integer.parseInt(s);
			if(i > 0) return i;
		} catch(NumberFormatException ex){
			return -1;
		}

		return -1;
	}
	
	public static double parseDouble(String s){
		try{
			double i = Double.parseDouble(s);
			if(i > 0) return i;
		} catch(NumberFormatException ex){
			return -1D;
		}

		return -1D;
	}
	
}
