package org.blockface.virtualshop.util;

public class Numbers
{
	public static final int ALL = 135291444;
	
    public static Integer parseInteger(String s)
	{
    	
    	if(s.equalsIgnoreCase("all"))
			return Numbers.ALL;
    	
		try
		{
			Integer i = Integer.parseInt(s);
			if(i > 0) return i;
		}
		catch(NumberFormatException ex)
		{
			return -1;
		}

		return -1;
	}

	public static Float parseFloat(String s)
	{
		try
		{
			Float i = Float.parseFloat(s);
			if(i > 0) return i;
		}
		catch(NumberFormatException ex)
		{
			return -1f;
		}

		return -1f;
	}
}
