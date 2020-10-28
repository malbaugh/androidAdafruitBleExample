package com.example.blearduinoexampleproject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/*
* This is a class for the accelerometer data captured from your BLE connection.
*
* */
public class AccelerometerData {

    private ArrayList<Integer> xAcceleration;
    private ArrayList<Integer> yAcceleration;
    private ArrayList<Integer> zAcceleration;
    private ArrayList<Integer> xGyroscope;
    private ArrayList<Integer> yGyroscope;
    private ArrayList<Integer> zGyroscope;
    private ArrayList<Date> readingTimes;
    private ArrayList<String> readingTimesFormatted;
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

    public AccelerometerData(ArrayList<Integer> xAcc, ArrayList<Integer> yAcc, ArrayList<Integer> zAcc, ArrayList<Integer> xGyro, ArrayList<Integer> yGyro, ArrayList<Integer> zGyro, ArrayList<Date> times) {
        this.xAcceleration = xAcc;
        this.yAcceleration = yAcc;
        this.zAcceleration = zAcc;
        this.xGyroscope = xGyro;
        this.yGyroscope = yGyro;
        this.zGyroscope = zGyro;
        this.readingTimes = times;
        formatReadTimes(times);
    }

    public ArrayList<Integer> getXAcceleration() {
        return this.xAcceleration;
    }
    public void setXAcceleration(ArrayList<Integer> xAcc) {
        this.xAcceleration = xAcc;
    }

    public ArrayList<Integer> getYAcceleration() {
        return this.yAcceleration;
    }
    public void setYAcceleration(ArrayList<Integer> yAcc) {
        this.yAcceleration = yAcc;
    }

    public ArrayList<Integer> getZAcceleration() {
        return this.zAcceleration;
    }
    public void setZAcceleration(ArrayList<Integer> zAcc) {
        this.zAcceleration = zAcc;
    }

    public int getXAccelerationAvg() {
        return calculateAverage(this.xAcceleration);
    }

    public int getYAccelerationAvg() {
        return calculateAverage(this.yAcceleration);
    }

    public int getZAccelerationAvg() {
        return calculateAverage(this.zAcceleration);
    }

    public ArrayList<Integer> getXGyroscope() {
        return this.xGyroscope;
    }
    public void setXGyroscope(ArrayList<Integer> xGyro) {
        this.xGyroscope = xGyro;
    }

    public ArrayList<Integer> getYGyroscope() {
        return this.yGyroscope;
    }
    public void setYGyroscope(ArrayList<Integer> yGyro) {
        this.yGyroscope = yGyro;
    }

    public ArrayList<Integer> getZGyroscope() {
        return this.zGyroscope;
    }
    public void setZGyroscope(ArrayList<Integer> zGyro) {
        this.zGyroscope = zGyro;
    }

    public ArrayList<Date> getReadingTimes() {
        return this.readingTimes;
    }
    public void setReadingTimes(ArrayList<Date> time) {
        this.readingTimes = time;
    }

    private void formatReadTimes(ArrayList<Date> times) {
        readingTimesFormatted = new ArrayList<String>();
        for (int i=0; i < 5; i++) {
            readingTimesFormatted.add(dateFormatter.format(readingTimes.get(i)));
        }
    }

    private int calculateAverage(ArrayList<Integer> data) {
        int sum = 0;
        if(!data.isEmpty()) {
            for (Integer num : data) {
                sum += num;
            }
            return sum / data.size();
        }
        System.out.println(sum);
        return sum;
    }
}
