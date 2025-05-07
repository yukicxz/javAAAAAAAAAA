/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package client;
import java.io.*;
import java.net.*;
/**
 *
 * @author Александра
 */
public class Client {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;
    public static void main(String[] args) {
        try {
            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

            System.out.println("Connected to server");

            Thread receiverThread = new Thread(new ServerHandler(socket));
            receiverThread.start();

            BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in));

            System.out.println("Type 'exit' to quit.");

            while (receiverThread.isAlive()) {
                String message = consoleInput.readLine();
                if (message == null) continue;

                if (message.equalsIgnoreCase("exit")) {
                    out.println("EXIT|");
                    out.flush();
                    break;
                }
            }
            receiverThread.join();
            
        } 
        catch (IOException | InterruptedException e) {
            System.err.println("Client error: " + e.getMessage());
        }

    }
}

