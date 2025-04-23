/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.lab1;

/**
 *
 * @author Александра
 */
public class IntegralPotok implements Runnable {
    private double a;
    private double b;
    private double step;
    private double partResult;
    
    public IntegralPotok(double a, double b, double step){
        this.a = a;
        this.b = b;
        this.step = step;
        this.partResult = 0.0;
    }
    
    @Override
    public void run(){
        System.out.println("Thread started: [" + a + ", " + b + "]");
        int n = (int)((b - a) / step);
        double x1, x2, h;
        double ost = b - (a + step * n);
        
        for (int i = 0; i <= n; i++) {
            x1 = a + i * step;
            h = (i == n ? ost : step);
            x2 = x1 + h;
            partResult += (Math.sin(x1 * x1) + Math.sin(x2 * x2)) * h / 2.0;
        }
        System.out.println("Thread finished: [" + a + ", " + b + "], result: " + partResult);
    }
    
    public double getPartResult(){
        return partResult;
    }
}
