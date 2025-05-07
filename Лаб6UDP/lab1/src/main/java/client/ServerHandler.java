/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package client;

import java.io.*;
import java.net.*;
import com.mycompany.lab1.RecIntegral;
import com.mycompany.lab1.ValueException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerHandler implements Runnable {
    private final DatagramSocket socket;
    private static final int BUFFER_SIZE = 1024;
    private volatile boolean running = true;

    public ServerHandler(DatagramSocket socket) {
        this.socket = socket;
    }

    public void sendResult(double result, InetAddress address, int port) {
        String message = "RESULT|" + result;
        byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
        try {
            socket.send(packet);
        } catch (IOException e) {
            System.out.println("Error sending result: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        byte[] buffer = new byte[BUFFER_SIZE];
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String received = new String(packet.getData(), 0, packet.getLength()).trim();
                String[] parts = received.split("\\|");
                String type = parts[0];

                switch (type) {
                    case "EXIT":
                        System.out.println("Server sent exit command");
                        running = false;
                        break;
                    case "TASK":
                        if (parts.length >= 2) {
                            RecIntegral task = deserializeTask(parts[1]);
                            if (task != null) {
                                double result = task.integrMultithreaded();
                                System.out.println("Computed result: " + result);
                                sendResult(result, packet.getAddress(), packet.getPort());
                            }
                        }
                        break;
                    default:
                        System.out.println("Unknown message type: " + type);
                }
            } catch (IOException | InterruptedException e) {
                System.out.println("Receiver error: " + e.getMessage());
                break;
            }
        }
    }

    public static RecIntegral deserializeTask(String data) {
        try {
            String[] parts = data.split(";");
            double a = Double.parseDouble(parts[0]);
            double b = Double.parseDouble(parts[1]);
            double h = Double.parseDouble(parts[2]);
            return new RecIntegral(a, b, h, 0.0);
        } catch (ValueException | NumberFormatException e) {
            System.out.println("Failed to deserialize task: " + e.getMessage());
            return null;
        }
    }
}

