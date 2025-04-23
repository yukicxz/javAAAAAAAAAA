/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.lab1;

import java.io.Serializable;

/**
 *
 * @author btwyu
 */
public class RecIntegral implements Serializable {
    private static final long serialVersionUID = 1L; // Рекомендуется добавить версию
    private double lowerLimit;
    private double upperLimit;
    private double step;
    private double result;
    
    public RecIntegral(double lowerLimit, double upperLimit, double step, double result) throws ValueException{
        if(!ValidateValue(lowerLimit) || !ValidateValue(upperLimit) || !ValidateValue(step)){
            throw new ValueException("Параметры должны быть числами в диапазоне от 0.000001 до 1000000");
        }
        if(step <=0 || lowerLimit>=upperLimit || step > upperLimit){
             throw new ValueException("Неверные параметры");
        }
        this.lowerLimit = lowerLimit;
        this.upperLimit = upperLimit;
        this.step = step;
        this.result = result;
    }
    private boolean ValidateValue(double value){
        return (value>=0.000001 && value <= 1000000);
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
        long startTime = System.nanoTime(); // начало отсчёта времени
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
        long endTime = System.nanoTime(); // конец отсчёта времени
        long duration = endTime - startTime;
        System.out.printf("Total time (SoloThread): %.3f ms%n", duration / 1_000_000.0);
        //sum += (Math.sin(x2 * x2) + Math.sin(b * b)) * ost / 2.0;
        return sum;
    }
    public double integrMultithreaded() throws InterruptedException {
    

    int threadCount = 8;
    double range = (upperLimit - lowerLimit) / threadCount;
    Thread[] threads = new Thread[threadCount];
    IntegralPotok[] tasks = new IntegralPotok[threadCount];
    long startTime = System.nanoTime(); // начало отсчёта времени

    for (int i = 0; i < threadCount; i++) {
        double a = lowerLimit + i * range;
        double b = (i == threadCount - 1) ? upperLimit : a + range;
        tasks[i] = new IntegralPotok(a, b, step);
        threads[i] = new Thread(tasks[i]);
        threads[i].start();
    }

    for (int i = 0; i < threadCount; i++) {
        threads[i].join();
    }

    double total = 0.0;
    for (IntegralPotok task : tasks) {
        total += task.getPartResult();
    }

    long endTime = System.nanoTime(); // конец отсчёта времени
    long duration = endTime - startTime;
    System.out.printf("Total time (MultiThread): %.3f ms%n", duration / 1_000_000.0);

    this.result = total;
    return total;
}
}