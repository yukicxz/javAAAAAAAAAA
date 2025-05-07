package com.mycompany.lab1;

import java.io.*;
import java.net.*;

public class ClientHandler {
    private final DatagramSocket socket;
    private final InetAddress clientAddress;
    private final int clientPort;
    private boolean hasResult = false;
    private double lastResult = 0.0;

    public ClientHandler(DatagramSocket socket, InetAddress address, int port) {
        this.socket = socket;
        this.clientAddress = address;
        this.clientPort = port;
    }

    public void handleMessage(String message) {
        String[] parts = message.split("\\|");
        String type = parts[0];

        switch (type) {
            case "REGISTRED":
                break;
            case "EXIT":
                System.out.println("[SERVER] Client requested exit");
                // Обработка выхода клиента будет на уровне сервера
                break;
            case "RESULT":
                if (parts.length >= 2) {
                    try {
                        lastResult = Double.parseDouble(parts[1]);
                        hasResult = true;
                        System.out.println("[SERVER] Result received: " + lastResult);
                    } catch (NumberFormatException e) {
                        System.out.println("[SERVER] Invalid result format.");
                    }
                }
                break;
            default:
                System.out.println("[SERVER] Unknown message type: " + type);
        }
    }

    public void sendTask(RecIntegral task) {
        String message = "TASK|" + serializeTask(task);
        sendMessage(message);
    }

    public void sendExit() {
        sendMessage("EXIT|");
    }

    private void sendMessage(String msg) {
        try {
            byte[] buffer = msg.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, clientAddress, clientPort);
            socket.send(packet);
        } catch (IOException e) {
            System.out.println("[SERVER] Failed to send message: " + e.getMessage());
        }
    }

    public boolean hasResult() {
        return hasResult;
    }

    public double getLastResult() {
        return lastResult;
    }

    public void reset() {
        hasResult = false;
        lastResult = 0.0;
    }

    private String serializeTask(RecIntegral task) {
        return task.getLowerLimit() + ";" + task.getUpperLimit() + ";" + task.getStep();
    }
}
