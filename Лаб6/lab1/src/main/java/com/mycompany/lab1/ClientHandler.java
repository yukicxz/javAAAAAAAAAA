package com.mycompany.lab1;

import java.io.*;
import java.net.*;
import static com.mycompany.lab1.NewJFrame.clients;
import java.util.logging.Level;
import java.util.logging.Logger;
public class ClientHandler implements Runnable {
    final private Socket socket;
    final private BufferedReader in;
    final private PrintWriter out;
    private boolean isBusy = false;
    private double lastResult = 0.0;
    private boolean hasResult = false;
    public ClientHandler(Socket s) throws IOException {
        socket = s;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
    }
    public void sendTask(RecIntegral task) {
        out.println("TASK|" + serializeTask(task));
        out.flush();
    }
    public void sendExit() {
        out.println("EXIT|");
        out.flush();
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
    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                String[] parts = line.split("\\|");
                String type = parts[0];
                switch (type) {
                    case "EXIT":
                        System.out.println("[SERVER] Client requested exit");
                        Thread.sleep(1000);
                        socket.close();
                        break;
                    case "RESULT":
                        String data = parts[1];
                        lastResult = Double.parseDouble(data);
                        System.out.println(lastResult);
                        hasResult = true;
                        System.out.println("[SERVER] Result is received");
                        break;
                    default:
                        System.out.println("[SERVER] Unknown message type: " + type);
                }
            }
        } catch (IOException e) {
            System.out.println("[SERVER] Client disconnected");
        } catch (InterruptedException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                clients.remove(socket); 
                if (!socket.isClosed()) socket.close();
                in.close();
                out.close();
            } catch (IOException e) {
                System.out.println("[SERVER] Socket close error");
            }
        }
    }

    private String serializeTask(RecIntegral task) {
        return task.getLowerLimit() + ";" + task.getUpperLimit() + ";" + task.getStep();
    }
    
}
