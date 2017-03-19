package com.virtualbienes.app.models;

/**
 * Created by runegame on 17-03-2017.
 */

public class Neighborhood {
    private int id;
    private String referential;
    private String dataName;

    public Neighborhood (int id, String dataName) {
        setId(id);
        setDataName(dataName);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDataName() {
        return dataName;
    }

    public void setDataName(String dataName) {
        this.dataName = dataName;
    }
}
