package com.mycompany.lab1;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import com.mycompany.lab1.ClientHandler;

import static com.mycompany.lab1.NewJFrame.clients;

public class Server {
    static final int PORT = 8080;
    static final int THREADS_LIMIT = 100;
    private static double totalResult = 0.0;
    private static int receivedResults = 0;
    public static ExecutorService threadPool;

    public static void StartServer() throws IOException {
        DatagramSocket socket = new DatagramSocket(PORT);
        System.out.println("UDP Server started on port " + PORT);
        threadPool = Executors.newFixedThreadPool(THREADS_LIMIT);

        threadPool.submit(() -> listenForClients(socket));

        // Сервер продолжает работать в фоне
    }

    private static void listenForClients(DatagramSocket socket) {
    byte[] buffer = new byte[1024];

    try {
    while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            String message = new String(packet.getData(), 0, packet.getLength());
            InetSocketAddress clientAddr = new InetSocketAddress(packet.getAddress(), packet.getPort());


            // Регистрируем клиента, если его ещё нет
           clients.computeIfAbsent(clientAddr, addr -> {
           System.out.println("[SERVER] New client connected: " + addr);
           return new ClientHandler(socket, packet.getAddress(), packet.getPort());
        });


            ClientHandler handler = clients.get(clientAddr);
            handler.handleMessage(message);
            }
        } catch (IOException e) {
            System.out.println("[SERVER] UDP receive error: " + e.getMessage());
        }
        finally {
        // При штатном завершении, закрываем сервер
            if (!socket.isClosed()) {
                socket.close();
            }
            threadPool.shutdown(); // Завершаем пул потоков корректно
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

        final Object lock = new Object();
        final int[] completed = {0};

        for (ClientHandler client : clients.values()) {
            client.reset();

            int stepsForClient = stepsPerClient + (i < remainder ? 1 : 0);
            double subA = a + currentStep * h;
            double subB = subA + stepsForClient * h;

            try {
                RecIntegral subTask = new RecIntegral(subA, subB, h, 0.0);

                threadPool.submit(() -> {
                    client.sendTask(subTask);

                    while (!client.hasResult()) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }

                    synchronized (lock) {
                        totalResult += client.getLastResult();
                        completed[0]++;
                        lock.notify();
                    }
                });

            } catch (ValueException e) {
                System.out.println("Subtask error: " + e.getMessage());
            }

            currentStep += stepsForClient;
            i++;
        }

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
