package com.ecarx.xui.adaptapi.car.base;

public interface ICarFunction {
    int getFunctionValue(int propertyId);
    int getFunctionValue(int propertyId, int areaId);
    boolean setFunctionValue(int propertyId, int value);
    boolean setFunctionValue(int propertyId, int areaId, int value);
}
