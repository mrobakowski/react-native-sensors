package com.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.annotation.Nullable;
import android.util.Log;
import com.facebook.react.bridge.*;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public class Rotation extends ReactContextBaseJavaModule implements SensorEventListener {
    private final ReactApplicationContext reactContext;
    private final SensorManager sensorManager;
    private final Sensor magneticSensor;
    private final Sensor gyroscopicSensor;
    private double lastReading = (double) System.currentTimeMillis();
    private int interval;
    private float[] magneticRotationMatrix = new float[]{
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
    };
    private float[] gyroscopicRotationMatrix = new float[]{
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
    };
    private float[] magneticCorrection = new float[]{
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
    };
    private float[] invertedGyroscopic = new float[]{
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
    };
    private boolean newMagneticMatrix = false;

    public Rotation(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.sensorManager = (SensorManager) reactContext.getSystemService(Context.SENSOR_SERVICE);
        assert this.sensorManager != null;
        this.magneticSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        this.gyroscopicSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
    }

    // RN Methods
    @ReactMethod
    public void isAvailable(Promise promise) {
        if (this.magneticSensor == null || this.gyroscopicSensor == null) {
            // No sensor found, throw error
            promise.reject(new RuntimeException("No Rotation Sensor found"));
            return;
        }
        promise.resolve(null);
    }

    @ReactMethod
    public void setUpdateInterval(int newInterval) {
        this.interval = newInterval;
    }

    @ReactMethod
    public void startUpdates() {
        // Milisecond to Mikrosecond conversion
        sensorManager.registerListener(this, gyroscopicSensor, this.interval * 1000);
        sensorManager.registerListener(this, magneticSensor, this.interval * 1000 / 100);
    }

    @ReactMethod
    public void stopUpdates() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public String getName() {
        return "Rotation";
    }

    // SensorEventListener Interface
    private void sendEvent(String eventName, @Nullable WritableArray params) {
        try {
            this.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(eventName, params);
        } catch (RuntimeException e) {
            Log.e("ERROR", "java.lang.RuntimeException: Trying to invoke Javascript before CatalystInstance has been set!");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        double tempMs = (double) System.currentTimeMillis();
        if (tempMs - lastReading >= interval) {
            Sensor mySensor = sensorEvent.sensor;
            lastReading = tempMs;

            if (mySensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR) {
                SensorManager.getRotationMatrixFromVector(gyroscopicRotationMatrix, sensorEvent.values);
                if (newMagneticMatrix) {
                    updateCorrectionMatrix();
                    newMagneticMatrix = false;
                }
                mul(gyroscopicRotationMatrix, gyroscopicRotationMatrix, magneticCorrection);
                WritableArray arr = Arguments.fromArray(gyroscopicRotationMatrix);
                sendEvent("Rotation", arr);
            } else if (mySensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                SensorManager.getRotationMatrixFromVector(magneticRotationMatrix, sensorEvent.values);
                newMagneticMatrix = true;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void mul(float[] out, float[] a, float[] b) {
        float a00 = a[0],
                a01 = a[1],
                a02 = a[2],
                a03 = a[3];
        float a10 = a[4],
                a11 = a[5],
                a12 = a[6],
                a13 = a[7];
        float a20 = a[8],
                a21 = a[9],
                a22 = a[10],
                a23 = a[11];
        float a30 = a[12],
                a31 = a[13],
                a32 = a[14],
                a33 = a[15];

        float b0 = b[0],
                b1 = b[1],
                b2 = b[2],
                b3 = b[3];

        out[0] = b0 * a00 + b1 * a10 + b2 * a20 + b3 * a30;
        out[1] = b0 * a01 + b1 * a11 + b2 * a21 + b3 * a31;
        out[2] = b0 * a02 + b1 * a12 + b2 * a22 + b3 * a32;
        out[3] = b0 * a03 + b1 * a13 + b2 * a23 + b3 * a33;

        b0 = b[4];
        b1 = b[5];
        b2 = b[6];
        b3 = b[7];

        out[4] = b0 * a00 + b1 * a10 + b2 * a20 + b3 * a30;
        out[5] = b0 * a01 + b1 * a11 + b2 * a21 + b3 * a31;
        out[6] = b0 * a02 + b1 * a12 + b2 * a22 + b3 * a32;
        out[7] = b0 * a03 + b1 * a13 + b2 * a23 + b3 * a33;

        b0 = b[8];
        b1 = b[9];
        b2 = b[10];
        b3 = b[11];

        out[8] = b0 * a00 + b1 * a10 + b2 * a20 + b3 * a30;
        out[9] = b0 * a01 + b1 * a11 + b2 * a21 + b3 * a31;
        out[10] = b0 * a02 + b1 * a12 + b2 * a22 + b3 * a32;
        out[11] = b0 * a03 + b1 * a13 + b2 * a23 + b3 * a33;

        b0 = b[12];
        b1 = b[13];
        b2 = b[14];
        b3 = b[15];

        out[12] = b0 * a00 + b1 * a10 + b2 * a20 + b3 * a30;
        out[13] = b0 * a01 + b1 * a11 + b2 * a21 + b3 * a31;
        out[14] = b0 * a02 + b1 * a12 + b2 * a22 + b3 * a32;
        out[15] = b0 * a03 + b1 * a13 + b2 * a23 + b3 * a33;
    }

    private void inv(float[] out, float[] a) {
        float a00 = a[0],
                a01 = a[1],
                a02 = a[2],
                a03 = a[3];
        float a10 = a[4],
                a11 = a[5],
                a12 = a[6],
                a13 = a[7];
        float a20 = a[8],
                a21 = a[9],
                a22 = a[10],
                a23 = a[11];
        float a30 = a[12],
                a31 = a[13],
                a32 = a[14],
                a33 = a[15];
        float b00 = a00 * a11 - a01 * a10;
        float b01 = a00 * a12 - a02 * a10;
        float b02 = a00 * a13 - a03 * a10;
        float b03 = a01 * a12 - a02 * a11;
        float b04 = a01 * a13 - a03 * a11;
        float b05 = a02 * a13 - a03 * a12;
        float b06 = a20 * a31 - a21 * a30;
        float b07 = a20 * a32 - a22 * a30;
        float b08 = a20 * a33 - a23 * a30;
        float b09 = a21 * a32 - a22 * a31;
        float b10 = a21 * a33 - a23 * a31;
        float b11 = a22 * a33 - a23 * a32;

        float det = b00 * b11 - b01 * b10 + b02 * b09 + b03 * b08 - b04 * b07 + b05 * b06;

        det = 1.0f / det;
        out[0] = (a11 * b11 - a12 * b10 + a13 * b09) * det;
        out[1] = (a02 * b10 - a01 * b11 - a03 * b09) * det;
        out[2] = (a31 * b05 - a32 * b04 + a33 * b03) * det;
        out[3] = (a22 * b04 - a21 * b05 - a23 * b03) * det;
        out[4] = (a12 * b08 - a10 * b11 - a13 * b07) * det;
        out[5] = (a00 * b11 - a02 * b08 + a03 * b07) * det;
        out[6] = (a32 * b02 - a30 * b05 - a33 * b01) * det;
        out[7] = (a20 * b05 - a22 * b02 + a23 * b01) * det;
        out[8] = (a10 * b10 - a11 * b08 + a13 * b06) * det;
        out[9] = (a01 * b08 - a00 * b10 - a03 * b06) * det;
        out[10] = (a30 * b04 - a31 * b02 + a33 * b00) * det;
        out[11] = (a21 * b02 - a20 * b04 - a23 * b00) * det;
        out[12] = (a11 * b07 - a10 * b09 - a12 * b06) * det;
        out[13] = (a00 * b09 - a01 * b07 + a02 * b06) * det;
        out[14] = (a31 * b01 - a30 * b03 - a32 * b00) * det;
        out[15] = (a20 * b03 - a21 * b01 + a22 * b00) * det;
    }

    private void updateCorrectionMatrix() {
        // M - magnetic, G - gyroscopic, C - correction
        // G * C = M
        // G^-1 * G * C = G^-1 * M
        //            C = G^-1 * M
        inv(invertedGyroscopic, gyroscopicRotationMatrix);
        mul(magneticCorrection, invertedGyroscopic, magneticRotationMatrix);
    }
}
