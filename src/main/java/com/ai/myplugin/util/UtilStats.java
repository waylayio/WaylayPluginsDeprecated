package com.ai.myplugin.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by User: veselin
 * On Date: 26/03/14
 */
public class UtilStats {

    private static final Log log = LogFactory.getLog(UtilStats.class);
    private double[] samples = null;
    private int samplesLength = 0;
    public int n = 0;
    public double min  = Double.MAX_VALUE;
    public double max  = - Double.MAX_VALUE;
    public double avg = 0;
    public double stdev = 0;

    public UtilStats() {
    }

    public UtilStats(int bufferLength) {
        log.info("init stats calculation on the number of samples " +bufferLength);
        samples = new double[bufferLength];
        samplesLength = bufferLength;
    }

    public synchronized void addSample(double sample){
        log.info("add sample "+sample);
        if(samples == null){
            double prevAvg = n * avg;
            n +=1;
            avg = (prevAvg + sample) / n;
            if(min > sample)
                min = sample;
            if(max < sample)
                max = sample;
            stdev = Math.sqrt(1.0/n * Math.pow(sample - avg, 2));
        } else {
            samples[n%samplesLength] = sample;
            n++;
            calculateStats(sample);
        }
    }

    private void calculateStats(double sample) {
        log.info("calculateStats for " +sample);
        resetCounters();
        double sum =0;
        int length = n > samplesLength ? samplesLength :n;
        for(int i=0; i<length; i ++){
            if(min > samples[i])
                min = samples[i];
            if(max < samples[i])
                max = samples[i];
            sum += samples[i];
        }
        avg = sum/length;
        stdev = Math.sqrt(1.0/length * Math.pow(sample - avg, 2));
    }

    private void resetCounters() {
        min  = Double.MAX_VALUE;
        max  = - Double.MAX_VALUE;
        avg = 0;
        stdev = 0;
    }

    @Override
    public String toString() {
        return "SimpleStats{" +
                "N=" + n +
                ", min=" + min +
                ", max=" + max +
                ", avg=" + avg +
                ", stdev=" + stdev +
                ", buffer=" + samplesLength +
                '}';
    }

    public double getValueForOperator(String operator) {
        if(operator.startsWith("avg"))
            return avg;
        if(operator.startsWith("min"))
            return min;
        if(operator.startsWith("max"))
            return max;
        if(operator.startsWith("std"))
            return stdev;
        throw new RuntimeException("operator " + operator + " not found");
    }
}