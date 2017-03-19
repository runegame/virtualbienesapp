package com.virtualbienes.app.models;

/**
 * Created by runegame on 15-03-2017.
 */

public class Municipality {
    private int id;
    private String dataName;
    private String point;
    private String iso;
    private int idDepartment;
    private String postalCode;
    private String zonePostal;
    private String department;

    public Municipality(int id, String dataName, String point, String iso, int idDepartment,
                        String postalCode, String zonePostal, String department) {
        setId(id);
        setDataName(dataName);
        setPoint(point);
        setIso(iso);
        setIdDepartment(idDepartment);
        setPostalCode(postalCode);
        setZonePostal(zonePostal);
        setDepartment(department);
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

    public String getIso() {
        return iso;
    }

    public void setIso(String iso) {
        this.iso = iso;
    }

    public int getIdDepartment() {
        return idDepartment;
    }

    public void setIdDepartment(int idDepartment) {
        this.idDepartment = idDepartment;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getZonePostal() {
        return zonePostal;
    }

    public void setZonePostal(String zonePostal) {
        this.zonePostal = zonePostal;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
