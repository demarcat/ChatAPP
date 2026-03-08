import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {

    private static final int PORT = 8080;
    private static final AtomicInteger threadId = new AtomicInteger(1);

    // username → writer
    private static final ConcurrentHashMap<String, PrintWriter> userMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        log("[INFO] ChatApp Server v1.0");
        log("[INFO] Listening on port " + PORT + "...\n");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                int tid = threadId.getAndIncrement();
                Thread t = new Thread(new ClientHandler(socket, tid));
                t.setName("thread-" + tid);
                t.start();
            }
        } catch (IOException e) {
            log("[ERROR] Server crashed: " + e.getMessage());
        }
    }

    // ── Logging ──────────────────────────────────────────────────────────
    static void log(String msg) {
        System.out.println(msg);
    }

    static void logConn(String user, boolean join, String threadName) {
        String sym = join ? "[+]" : "[-]";
        String verb = join ? "connected   (" + threadName + ")" : "disconnected";
        System.out.printf("%-4s %-12s %s%n", sym, user, verb);
    }

    static void logGroup(String user, String msg) {
        System.out.printf("[GROUP] %-10s→ %s%n", user + " ", msg);
    }

    static void logDM(String from, String to, String msg) {
        System.out.printf("  [DM]  %-10s→ @%s %s%n", from + " ", to, msg);
    }

    // ── ClientHandler ─────────────────────────────────────────────────────
    static class ClientHandler implements Runnable {
        private final Socket socket;
        private final int tid;
        private String userName;
        private BufferedReader in;
        private PrintWriter out;

        ClientHandler(Socket socket, int tid) {
            this.socket = socket;
            this.tid = tid;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // First message is the username
                userName = in.readLine();
                if (userName == null || userName.isBlank())
                    return;
                userName = userName.trim();

                // Reject duplicate usernames
                if (userMap.containsKey(userName)) {
                    out.println("INFO:Username already taken. Please reconnect with a different name.");
                    return;
                }

                userMap.put(userName, out);
                logConn(userName, true, "thread-" + tid);
                broadcast("INFO:" + userName + " joined the room");
                broadcastUserList();

                String line;
                while ((line = in.readLine()) != null) {
                    handleMessage(line);
                }

            } catch (Exception ignored) {
                // Connection reset — handled in finally
            } finally {
                cleanup();
            }
        }

        private void handleMessage(String msg) {
            if (msg.startsWith("GROUP:")) {
                String text = msg.substring(6);
                logGroup(userName, text);
                broadcastExcept(userName, "FROM:" + userName + ":" + text);

            } else if (msg.startsWith("PRIVATE:")) {
                // PRIVATE:targetUser:message
                String[] parts = msg.split(":", 3);
                if (parts.length == 3) {
                    String target = parts[1];
                    String text = parts[2];
                    logDM(userName, target, text);
                    sendTo(target, "PRIVATE_FROM:" + userName + ":" + text);
                    if (userMap.get(target) == null) {
                        // Notify sender the target is offline
                        out.println("INFO:" + target + " is not online.");
                    }
                }
            }
        }

        private void cleanup() {
            if (userName != null) {
                userMap.remove(userName);
                logConn(userName, false, "thread-" + tid);
                broadcast("INFO:" + userName + " left the room");
                broadcastUserList();
            }
            try {
                socket.close();
            } catch (Exception ignored) {
            }
        }
    }

    // ── Broadcast helpers ─────────────────────────────────────────────────
    private static void broadcast(String msg) {
        userMap.values().forEach(w -> w.println(msg));
    }

    private static void broadcastExcept(String excludeUser, String msg) {
        userMap.forEach((name, w) -> {
            if (!name.equals(excludeUser))
                w.println(msg);
        });
    }

    private static void sendTo(String target, String msg) {
        PrintWriter w = userMap.get(target);
        if (w != null)
            w.println(msg);
    }

    private static void broadcastUserList() {
        String list = String.join(",", userMap.keySet());
        broadcast("USERS:" + list);
    }

    // ── Timestamp util (for future extensions) ────────────────────────────
    static String now() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date());
    }
}
