/*
 * VirtualShopX
 * Copyright (C) 2015-2018 Daniel D. Scalzi
 * See LICENSE for license information.
 */
package com.dscalzi.virtualshopx.util;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import com.dscalzi.virtualshopx.VirtualShopX;
import com.dscalzi.virtualshopx.managers.MessageManager;

public enum Localization {

	US(Locale.US),
	EU(Locale.ITALY);
	
	private Locale locale;
	
	Localization(Locale locale){
		this.locale = locale;
	}
	
	public String formatAmt(Integer amt){
		return NumberFormat.getIntegerInstance(locale).format(amt);
	}
	
	public String formatPrice(Double price){
		String symbol = VirtualShopX.getEconSymbol();
		int symbolLength = NumberFormat.getCurrencyInstance(locale).format(new Double(0.0)).replaceFirst("0", "loc").split("loc")[0].length();
		return symbol + NumberFormat.getCurrencyInstance(locale).format(price).substring(symbolLength);
	}
	
	public Number parse(String d){
		try {
			return NumberFormat.getNumberInstance(locale).parse(d);
		} catch (ParseException e) {
			MessageManager.getInstance().logError(e.getMessage(), true);
			e.printStackTrace();
			return null;
		}
	}
	
	public Locale getLocale(){
		return locale;
	}
}
