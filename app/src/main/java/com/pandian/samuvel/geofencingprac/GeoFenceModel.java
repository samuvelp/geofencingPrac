package com.pandian.samuvel.geofencingprac;

/**
 * Created by HP PC on 08-09-2017.
 */

public class GeoFenceModel {
    double mLatitude;
    double mLongitude;
    float mRadius;

    public GeoFenceModel() {
    }

    public GeoFenceModel(double mLatitude, double mLongitude, float mRadius) {
        this.mLatitude = mLatitude;
        this.mLongitude = mLongitude;
        this.mRadius = mRadius;
    }

    public double getmLatitude() {
        return mLatitude;
    }

    public void setmLatitude(double mLatitude) {
        this.mLatitude = mLatitude;
    }

    public double getmLongitude() {
        return mLongitude;
    }

    public void setmLongitude(double mLongitude) {
        this.mLongitude = mLongitude;
    }

    public float getmRadius() {
        return mRadius;
    }

    public void setmRadius(float mRadius) {
        this.mRadius = mRadius;
    }
}
