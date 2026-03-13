package com.geely.os.car;

public interface IGlyCar {
    int getIntProperty(int propertyId, int areaId);
    boolean setIntProperty(int propertyId, int areaId, int value);
    void disconnect();
}
