/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.lab1;

/**
 *
 * @author btwyu
 */
public class RecIntegral {
    private double lowerLimit;
    private double upperLimit;
    private double step;
    private double result;
    
    public RecIntegral(double lowerLimit, double upperLimit, double step, double result){
        this.lowerLimit = lowerLimit;
        this.upperLimit = upperLimit;
        this.step = step;
        this.result = result;
    }
    public double getLowerLimit(){
        return lowerLimit;
    }
    public double getUpperLimit(){
        return upperLimit;
    }
    public double getStep(){
        return step;
    }
    public double getResult(){
        return result;
    }
    public void setResult(double result){
        this.result = result;
    }
    public double integr(){
        double a = this.lowerLimit;
        double b = this.upperLimit;
        double h = this.step;
        int n = (int)((b-a)/h);
        double x1 = 0.0;
        double x2 = 0.0;
        double sum = 0.0;
        double ost = b - (a+h*(double)n);
        for(int i=0; i<n+1; i++){
            x1 = a + i * h;
            h = (i == n ? ost : h);
            x2 =  x1 + h;
            
            sum += (Math.sin(x1 * x1) + Math.sin(x2 * x2)) * h / 2.0;
        }
        
        //sum += (Math.sin(x2 * x2) + Math.sin(b * b)) * ost / 2.0;
        return sum;
    }
}