/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package client;

import java.io.*;
import java.net.*;

public class Client {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;
    private static final int BUFFER_SIZE = 1024;

    public static void main(String[] args) {
        try {
            DatagramSocket socket = new DatagramSocket();
            InetAddress serverAddress = InetAddress.getByName(SERVER_HOST);
            // Приветственное сообщение
            String hello = "REGISTRED|";
            byte[] buffer = hello.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName("localhost"), 8080);
            socket.send(packet);


            // Поток для приёма и обработки сообщений от сервера
            Thread receiverThread = new Thread(new ServerHandler(socket));
            receiverThread.start();

            BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Type 'exit' to quit.");

            while (receiverThread.isAlive()) {
                String message = consoleInput.readLine();
                if (message == null) continue;

                if (message.equalsIgnoreCase("exit")) {
                    String exitMessage = "EXIT|";
                    buffer = exitMessage.getBytes();
                    packet = new DatagramPacket(buffer, buffer.length, serverAddress, SERVER_PORT);
                    socket.send(packet);
                    break;
                }
            }
            socket.close();

        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}


