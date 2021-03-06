package com.virtualbienes.app.models;

/**
 * Created by runegame on 15-03-2017.
 */

public class Department {
    private int id;
    private String dataName;
    private String point;

    public Department(int id,String dataName, String point) {
        setId(id);
        setDataName(dataName);
        setPoint(point);
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

    public String getPoint() {
        return point;
    }

    public void setPoint(String point) {
        this.point = point;
    }

    @Override
    public String toString() {
        return getDataName();
    }
}
