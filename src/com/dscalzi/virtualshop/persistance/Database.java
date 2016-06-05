package com.dscalzi.virtualshop.persistance;

import java.sql.ResultSet;

public interface Database
{
    public void load() throws Exception;

    public ResultSet query(String query);

    public void unload();
}
