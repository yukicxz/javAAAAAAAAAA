package com.mycompany.clientserver;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Date;

public class Server {
    // Список всех клиентов (потокобезопасный)
    private static List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(1234);
        System.out.println("Server started at port 1234...");

        while (true) {
            Socket socket = serverSocket.accept();
            new Thread(() -> authenticateAndHandle(socket)).start();
        }
    }

    private static void authenticateAndHandle(Socket socket) {
        try {
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            String command = dis.readUTF(); // LOGIN или REGISTER
            String login = dis.readUTF();
            String password = dis.readUTF();

            if ("LOGIN".equalsIgnoreCase(command)) {
                if (checkCredentials(login, password)) {
                    dos.writeUTF("OK");
                    dos.flush();
                    ClientHandler clientHandler = new ClientHandler(socket, login);
                    clients.add(clientHandler);
                    new Thread(clientHandler).start();
                    System.out.println("Client connected: " + login);
                } else {
                    dos.writeUTF("FAIL");
                    dos.flush();
                }
            } else if ("REGISTER".equalsIgnoreCase(command)) {
                if (userExists(login)) {
                    dos.writeUTF("EXISTS");
                    dos.flush();
                } else {
                    registerUser(login, password);
                    dos.writeUTF("REGISTERED");
                    dos.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean userExists(String login) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:db/users.db");
             PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM users WHERE username = ?")) {
            stmt.setString(1, login);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void registerUser(String login, String password) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:db/users.db");
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)")) {
            stmt.setString(1, login);
            stmt.setString(2, password);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static boolean checkCredentials(String login, String password) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:db/users.db");
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?")) {
            stmt.setString(1, login);
            stmt.setString(2, password);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Класс, отвечающий за отдельного клиента
    private static class ClientHandler implements Runnable {
        private Socket socket;
        private String username;
        private DataInputStream dis;
        private DataOutputStream dos;
    
    public ClientHandler(Socket socket, String username) throws IOException {
        this.socket = socket;
        this.username = username;
        this.dis = new DataInputStream(socket.getInputStream());
        this.dos = new DataOutputStream(socket.getOutputStream());

    }
    private static final Set<String> loggedMessages = Collections.synchronizedSet(new HashSet<>());
    private String lastSentDate = "";
    
    private String formatDateHeader(String formattedDate) {
        int totalLength = 87;
        String dateCenter = " " + formattedDate + " ";
        int dashes = (totalLength - dateCenter.length()) / 2;

        StringBuilder sb = new StringBuilder();
        sb.append("-".repeat(Math.max(0, dashes)));
        sb.append(dateCenter);
        sb.append("-".repeat(Math.max(0, totalLength - sb.length()))); // добиваем справа
        return sb.toString();
    }

    private void logMessage(String sender, String recipient, String message) {
        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
        String fullMessage = String.format("[%s] %s", timestamp, message);
        String hash = Integer.toString(fullMessage.hashCode());
        if (loggedMessages.contains(hash)) return;
        loggedMessages.add(hash);

        String filename;
        if ("ALL".equalsIgnoreCase(recipient)) {
            filename = "history/ALL/chat_log.txt";
        } else {
            String user1 = sender;
            String user2 = recipient;
            if (user1.compareTo(user2) > 0) {
                String temp = user1;
                user1 = user2;
                user2 = temp;
            }
            filename = String.format("history/private/%s_%s.txt", user1, user2);
        }

        try (PrintWriter logOut = new PrintWriter(new FileWriter(filename, true))) {
            logOut.println(fullMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

        private void sendHistory(String recipient) {
        String filename;
        if (recipient.equalsIgnoreCase("ALL")) {
            filename = "history/ALL/chat_log.txt";
        } else {
            String user1 = username;
            String user2 = recipient;
            if (user1.compareTo(user2) > 0) {
                String temp = user1;
                user1 = user2;
                user2 = temp;
            }
            filename = String.format("history/private/%s_%s.txt", user1, user2);
        }

        File file = new File(filename);
        boolean headerPrinted = false;

        try (BufferedReader reader = file.exists() ? new BufferedReader(new FileReader(file)) : null) {
            dos.writeUTF("HISTORY");

            String lastDate = "";
            String line;

            if (reader != null) {
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("[")) continue;

                    String dateTimeStr = line.substring(1, 20);
                    LocalDate date = LocalDate.parse(dateTimeStr.substring(0, 10));
                    LocalTime time = LocalTime.parse(dateTimeStr.substring(11));
                    String rest = line.substring(22);

                    String formattedDate = date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("ru")));

                    if (!formattedDate.equals(lastDate)) {
                        lastDate = formattedDate;
                        dos.writeUTF(formatDateHeader(formattedDate));
                        headerPrinted = true;
                        this.lastSentDate = formattedDate;
                    }

                    String formatted = String.format("[%s] %s", time.toString(), rest);
                    dos.writeUTF(formatted);
                }
            }

            // Если ничего не было отправлено — добавим текущую дату
            if (!headerPrinted) {
                String today = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("ru")));
                dos.writeUTF(formatDateHeader(today));
                this.lastSentDate = today;
            }

            dos.writeUTF("---END---");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    public void run() {
        sendUserListToAll();
        try {
            while (true) {
                String line = dis.readUTF();

                if (line.startsWith("GET_HISTORY")) {
                    String target = line.substring("GET_HISTORY".length()).trim();
                    sendHistory(target);
                    continue;
                }

                if (line.equals("FILE")) {
                    String recipient = dis.readUTF();
                    String filename = dis.readUTF();
                    long fileSize = dis.readLong();

                    byte[] fileData = new byte[(int) fileSize];
                    dis.readFully(fileData);

                    for (ClientHandler client : clients) {
                        if (client.username.equals(recipient)) {
                            client.dos.writeUTF("FILE");
                            client.dos.writeUTF(username);
                            client.dos.writeUTF(filename);
                            client.dos.writeLong(fileSize);
                            client.dos.write(fileData);
                            client.dos.flush();
                            break;
                        }
                    }
                    continue;
                }

                String recipient = line;
                StringBuilder messageBuilder = new StringBuilder();
                String msgLine;
                while (!(msgLine = dis.readUTF()).equals("---END---")) {
                    messageBuilder.append(msgLine).append("\n");
                }

                String message = messageBuilder.toString().trim();
                if (recipient.equalsIgnoreCase("ALL")) {
                    broadcast(username + ": " + message);
                } else {
                    sendPrivateMessage(recipient, username + ": " + message);
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + username);
        } finally {
            try { socket.close(); } catch (IOException e) {}
            clients.remove(this);
            sendUserListToAll();
        }
    }


    private void broadcast(String messageText) throws IOException {
        LocalDate now = LocalDate.now();
        String currentDateFormatted = now.format(DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("ru")));
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String content = String.format("[%s] %s", time, messageText);

        for (ClientHandler client : clients) {
            if (!currentDateFormatted.equals(client.lastSentDate)) {
                client.lastSentDate = currentDateFormatted;
                client.dos.writeUTF(formatDateHeader(currentDateFormatted));
            }
            client.dos.writeUTF(content);
        }

        logMessage(username, "ALL", messageText);
    }


    private void sendPrivateMessage(String recipient, String messageText) throws IOException {
        LocalDate now = LocalDate.now();
        String currentDateFormatted = now.format(DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("ru")));
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String content = String.format("[%s] %s", time, messageText);

        for (ClientHandler client : clients) {
            if (client.username.equals(recipient) || client.username.equals(this.username)) {
                if (!currentDateFormatted.equals(client.lastSentDate)) {
                    client.lastSentDate = currentDateFormatted;
                    client.dos.writeUTF("PM:" + username + ":" + formatDateHeader(currentDateFormatted));
                }

                String direction = client.username.equals(this.username) ? recipient : username;
                client.dos.writeUTF("PM:" + direction + ":" + content);
            }
        }

        logMessage(username, recipient, messageText);
    }


    private void sendUserListToAll() {
        for (ClientHandler client : clients) {
            try {
                client.dos.writeUTF("USERLIST");

                clients.stream() //создаёт поток из списка клиентов;
                       .map(c -> c.username) //преобразует каждый ClientHandler в имя пользователя (String)
                       .forEach(name -> {
                           try {
                               client.dos.writeUTF(name);
                           } catch (IOException e) {
                               e.printStackTrace();
                           }
                       });

                client.dos.writeUTF("---END---");
            } catch (IOException e) {
                e.printStackTrace();
                }
            }
        }
    }
}
