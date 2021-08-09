package com.wevois.surveyapp;

public class Revisited {
    String lat;
    String lng;
    String reason;
    String houseType;
    String date;
    String id;
    String revisitedBy;
    String name;

    public Revisited(String lat, String lng, String reason, String houseType, String date, String id, String revisitedBy, String name) {
        this.lat = lat;
        this.lng = lng;
        this.reason = reason;
        this.houseType = houseType;
        this.date = date;
        this.id = id;
        this.revisitedBy = revisitedBy;
        this.name = name;
    }

    public String getLat() {
        return lat;
    }

    public String getLng() {
        return lng;
    }

    public String getReason() {
        return reason;
    }

    public String getHouseType() {
        return houseType;
    }

    public String getDate() {
        return date;
    }

    public String getId() {
        return id;
    }

    public String getRevisitedBy() {
        return revisitedBy;
    }

    public String getName() {
        return name;
    }
}
