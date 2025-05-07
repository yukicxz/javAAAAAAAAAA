package com.mycompany.lab1;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static com.mycompany.lab1.NewJFrame.clients;
public class Server {
    static final int PORT = 8080;
    static final int THREADS_LIMIT=100;
    private static double totalResult = 0.0;
    public static ExecutorService threadPool;
    public static void StartServer() throws IOException{
        ServerSocket s = new ServerSocket(PORT);
        System.out.println("Server started on port " + PORT);
        threadPool = Executors.newFixedThreadPool(THREADS_LIMIT);
        try {
            while (true) {
                Socket socket = s.accept();
                ClientHandler handler = new ClientHandler(socket);
                clients.put(socket, handler);
                threadPool.submit(handler);
                System.out.println("Client connected");
            }
         } catch (IOException e) {
        System.out.println("Server error: " + e.getMessage());
    } finally {
        // При штатном завершении, закрываем сервер
        try {
            if (!s.isClosed()) {
                s.close();
            }

            threadPool.shutdown(); // Завершаем пул потоков корректно

        } catch (IOException e) {
            System.out.println("Error during server shutdown: " + e.getMessage());
        }

        clients.clear(); // Очищаем карту клиентов уже после завершения
    }
        
    }
    public double distributeAndCompute(RecIntegral fullTask) {
    if (clients.isEmpty()) {
        System.out.println("No clients connected");
        return 0.0;
    }

    totalResult = 0.0;

    double a = fullTask.getLowerLimit();
    double b = fullTask.getUpperLimit();
    double h = fullTask.getStep();
    int clientCount = clients.size();

    int totalSteps = (int) Math.ceil((b - a) / h);
    int stepsPerClient = totalSteps / clientCount;
    int remainder = totalSteps % clientCount;

    int currentStep = 0;
    int i = 0;

    // Счётчик завершенных задач
    final Object lock = new Object();
    final int[] completed = {0};

    for (ClientHandler client : clients.values()) {
        client.reset();

        int stepsForClient = stepsPerClient + (i < remainder ? 1 : 0);
        double subA = a + currentStep * h;
        double subB = subA + stepsForClient * h;

        try {
            RecIntegral subTask = new RecIntegral(subA, subB, h, 0.0);

            // Параллельно ждем результат
            threadPool.submit(() -> {
                client.sendTask(subTask);

                // Ждём, пока клиент не пришлёт результат
                while (!client.hasResult()) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                // Добавляем результат в общий итог
                synchronized (lock) {
                    totalResult += client.getLastResult();
                    completed[0]++;
                    lock.notify();  // Оповещаем основной поток
                }
            });

        } catch (ValueException e) {
            System.out.println("Subtask error: " + e.getMessage());
        }

        currentStep += stepsForClient;
        i++;
    }

    // Ожидаем, пока все клиенты завершат выполнение
    synchronized (lock) {
        while (completed[0] < clientCount) {
            try {
                lock.wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Interrupted while waiting for results");
                break;
            }
        }
    }

    return totalResult;
}
    public int getClientsCount() {
    return clients.size();
    }
}