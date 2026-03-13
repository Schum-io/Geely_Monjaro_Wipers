package com.geely.os.car;

import android.content.Context;
import android.util.Log;

import com.ecarx.xui.adaptapi.binder.IConnectable;
import com.ecarx.xui.adaptapi.car.Car;
import com.ecarx.xui.adaptapi.car.ICar;
import com.ecarx.xui.adaptapi.car.base.ICarFunction;

public final class GlyCar {

    private static final String TAG = "GlyCar";
    private static final int ZONE_ALL = 0x80000000; // == Integer.MIN_VALUE == -0x80000000

    private GlyCar() {}

    public static IGlyCar create(Context context) {
        return create(context, null);
    }

    public static IGlyCar create(Context context, ConnectionListener listener) {
        ICar car = Car.create(context);
        return new Bridge(car, listener);
    }

    private static final class Bridge implements IGlyCar {

        private final ICar mCar;
        private volatile ICarFunction mCarFunction;

        Bridge(ICar car, ConnectionListener listener) {
            mCar = car;
            if (car instanceof IConnectable) {
                IConnectable connectable = (IConnectable) car;
                connectable.registerConnectWatcher(new IConnectable.IConnectWatcher() {
                    @Override
                    public void onConnected() {
                        mCarFunction = mCar.getICarFunction();
                        Log.d(TAG, "onConnected, carFunction=" + mCarFunction);
                        if (listener != null) listener.onConnected();
                    }

                    @Override
                    public void onDisConnected() {
                        mCarFunction = null;
                        Log.w(TAG, "onDisConnected");
                        if (listener != null) listener.onDisConnected();
                    }
                });
            } else {
                mCarFunction = car.getICarFunction();
                Log.d(TAG, "sync connect, carFunction=" + mCarFunction);
                if (listener != null) listener.onConnected();
            }
        }

        @Override
        public int getIntProperty(int propertyId, int areaId) {
            ICarFunction cf = mCarFunction;
            if (cf == null) {
                Log.w(TAG, "getIntProperty called but carFunction is null");
                return 0;
            }
            return cf.getFunctionValue(propertyId, areaId);
        }

        @Override
        public boolean setIntProperty(int propertyId, int areaId, int value) {
            ICarFunction cf = mCarFunction;
            if (cf == null) {
                Log.w(TAG, "setIntProperty called but carFunction is null");
                return false;
            }
            return cf.setFunctionValue(propertyId, areaId, value);
        }

        @Override
        public void disconnect() {
            if (mCar instanceof IConnectable) {
                ((IConnectable) mCar).disconnect();
            }
        }
    }
}
