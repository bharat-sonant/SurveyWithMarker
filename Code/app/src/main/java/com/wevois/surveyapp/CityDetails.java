package com.wevois.surveyapp;

public class CityDetails {
    String cityName;
    String key;
    String dbPath;
    String storagePath;

    public CityDetails(String cityName, String key, String dbPath, String storagePath) {
        this.cityName = cityName;
        this.key = key;
        this.dbPath = dbPath;
        this.storagePath = storagePath;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDbPath() {
        return dbPath;
    }

    public void setDbPath(String dbPath) {
        this.dbPath = dbPath;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    @Override
    public String toString() {
        return "CityDetails{" +
                "cityName='" + cityName + '\'' +
                ", key='" + key + '\'' +
                ", dbPath='" + dbPath + '\'' +
                ", storagePath='" + storagePath + '\'' +
                '}';
    }
}
