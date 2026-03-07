import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import javax.swing.*;

public class Client {
    private String userName;
    private PrintWriter out;
    private BufferedReader in;

    private JFrame mainFrame = new JFrame("LinkUp");
    
    private JTextArea groupArea = new JTextArea(20, 40);
    private JTextField inputField = new JTextField(30);
    private JButton sendBtn = new JButton("Send");

    private JList<String> userList = new JList<>();
    private HashMap<String, PrivateChatWindow> privateWindows = new HashMap<>();

    public Client(String serverHost, int port) {
        try {
            Socket socket = new Socket(serverHost, port);
            Image icon= Toolkit.getDefaultToolkit().getImage("icon.png");
            mainFrame.setIconImage(icon);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Ask user for username
            userName = JOptionPane.showInputDialog("Enter your username:");
            if (userName == null || userName.trim().isEmpty()) userName = "User";
            out.println(userName);

            buildUI();
            startReaderThread();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Connection error: " + e.getMessage());
        }
    }

    private void buildUI() {
        groupArea.setEditable(false); 

        mainFrame.setLayout(new BorderLayout());
        mainFrame.add(new JScrollPane(groupArea), BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.add(inputField);
        bottom.add(sendBtn);
        mainFrame.add(bottom, BorderLayout.SOUTH);

        // User list panel
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());
        rightPanel.add(new JLabel("Users Online:"), BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(userList), BorderLayout.CENTER);
        mainFrame.add(rightPanel, BorderLayout.EAST);

        // Send button action
        sendBtn.addActionListener(e -> sendGroupMessage());
        inputField.addActionListener(e -> sendGroupMessage());

        // Clicking a user opens private chat window
        userList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selected = userList.getSelectedValue();
                    if (!selected.equals(userName)) openPrivateWindow(selected);
                }
            }
        });

        mainFrame.pack();
        mainFrame.setVisible(true);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private void sendGroupMessage() {
        String msg = inputField.getText().trim();
        if (!msg.isEmpty()) {
            out.println("GROUP:" + msg);
            inputField.setText("");
        }
    }

    private void startReaderThread() {
        new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    processServerMessage(msg);
                }
            } catch (Exception e) {
                groupArea.append("Connection closed.\n");
            }
        }).start();
    }

    private void processServerMessage(String msg) {
        if (msg.startsWith("FROM:")) {
            String[] p = msg.split(":", 3);
            groupArea.append(p[1] + ": " + p[2] + "\n");
        }
        else if (msg.startsWith("PRIVATE_FROM:")) {
            String[] p = msg.split(":", 3);
            PrivateChatWindow win = openPrivateWindow(p[1]);
            win.area.append(p[1] + ": " + p[2] + "\n");
        }
        else if (msg.startsWith("INFO:")) {
            groupArea.append("[System] " + msg.substring(5) + "\n");
        }
        else if (msg.startsWith("USERS:")) {
            String[] users = msg.substring(6).split(",");
            userList.setListData(users);
        }
    }

    private PrivateChatWindow openPrivateWindow(String target) {
        if (!privateWindows.containsKey(target)) {
            privateWindows.put(target, new PrivateChatWindow(target));
        }
        return privateWindows.get(target);
    }

    class PrivateChatWindow {
    JFrame frame;
    JTextArea area;
    JTextField input;
    String targetUser;

        PrivateChatWindow(String targetUser) {
            this.targetUser = targetUser;

            frame = new JFrame("Private Chat with " + targetUser);
            area = new JTextArea(20, 40);
            area.setEditable(false);
            input = new JTextField(30);
            JButton send = new JButton("Send");

            send.addActionListener(e -> sendPrivateMsg(targetUser));

            JPanel bottom = new JPanel();
            bottom.add(input);
            bottom.add(send);

            frame.add(new JScrollPane(area), BorderLayout.CENTER);
            frame.add(bottom, BorderLayout.SOUTH);

            frame.pack();
            frame.setVisible(true);

            // ⭐ FIX: Remove from HashMap when window is closed
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    privateWindows.remove(targetUser);
                }
            });
        }

        void sendPrivateMsg(String targetUser) {
            String msg = input.getText().trim();
            if (!msg.isEmpty()) {
                out.println("PRIVATE:" + targetUser + ":" + msg);
                area.append("Me: " + msg + "\n");
                input.setText("");
            }
        }

    }

    public static void main(String[] args) {
        new Client("localhost", 5000);
    }
}
