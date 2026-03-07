import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final int PORT = 5000;

    // username → writer
    private static ConcurrentHashMap<String, PrintWriter> userMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("Server started on port " + PORT + "...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new ClientHandler(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private String userName;
        private BufferedReader in;
        private PrintWriter out;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // First message from client is username
            userName = in.readLine();
            if (userName == null) return;

            userMap.put(userName, out);
            broadcast("INFO:" + userName + " joined the chat");
            sendUserList();

            String msg;
            while ((msg = in.readLine()) != null) {
                handleMessage(msg);
            }

            } catch (Exception ignored) 
            {}

            finally {
                userMap.remove(userName);
                broadcast("INFO:" + userName + " left the chat");
                sendUserList();
                try { socket.close(); } catch (Exception e) {}
            }
        }

        private void handleMessage(String msg) {
            if (msg.startsWith("GROUP:")) {
                String text = msg.substring(6);
                broadcast("FROM:" + userName + ":" + text);
            }
            else if (msg.startsWith("PRIVATE:")) {
                // Format: PRIVATE:targetUser:message
                String[] parts = msg.split(":", 3);
                if (parts.length == 3) {
                    sendPrivate(parts[1], "PRIVATE_FROM:" + userName + ":" + parts[2]);
                }
            }
        }
    }

    private static void broadcast(String msg) {
        userMap.values().forEach(w -> w.println(msg));
    }

    private static void sendPrivate(String target, String msg) {
        PrintWriter w = userMap.get(target);
        if (w != null) w.println(msg);
    }

    private static void sendUserList() {
        String users = String.join(",", userMap.keySet());
        broadcast("USERS:" + users);
    }
}
