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
/**
 *
 * @author Александра
 */
public class ServerHandler implements Runnable{
    static final int PORT = 8080;
    final private Socket socket;
    final private BufferedReader in;
    final private PrintWriter out;
    private volatile boolean running = true;
    public ServerHandler(Socket s) throws IOException {
        socket = s;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
    }
    public void sendResult(double result) {
        out.println("RESULT|" + result);
        out.flush();
    }
    @Override
    public void run(){
        try {
            String line;
            while (running &&(line = in.readLine()) != null) {
                String[] parts = line.split("\\|");
                String type = parts[0];
                switch (type) {
                    case "EXIT":
                        System.err.println("Server closed connection");
                        running = false;
                        break;
                    case "TASK":
                        String data = parts[1];
                        RecIntegral task = deserializeTask(data);
                        try {
                            double result = task.integrMultithreaded();
                            System.out.println(result);
                            sendResult(result);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(ServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        break;

                    default:
                        System.out.println("Unknown message type: " + type);
                }
            }
        } catch (IOException e) {
            System.out.println("Disconnected");
        } finally {
            try {
                in.close();
                out.close();
            } catch (IOException e) {
                System.out.println("Socket close error");
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
        } catch (ValueException e) {
            System.out.println("Error deserialize: " + e.getMessage());
            return null;
        }
    }
}
