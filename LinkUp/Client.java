import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

public class Client {

    // ── Palette ─────────────────────────────────────────────────────────
    private static final Color BG_DARK = new Color(13, 17, 23);
    private static final Color BG_MID = new Color(16, 24, 36);
    private static final Color BG_PANEL = new Color(22, 30, 46);
    private static final Color BG_INPUT = new Color(18, 26, 40);
    private static final Color DIVIDER = new Color(30, 45, 65);
    private static final Color ACCENT = new Color(0, 212, 170);
    private static final Color BUBBLE_ME = new Color(0, 88, 72);
    private static final Color BUBBLE_OTH = new Color(28, 40, 60);
    private static final Color TXT_PRI = new Color(220, 230, 240);
    private static final Color TXT_SEC = new Color(100, 120, 145);
    private static final Color DOT_GREEN = new Color(0, 200, 90);

    private static final Font FONT_MONO = new Font(Font.MONOSPACED, Font.PLAIN, 13);
    private static final Font FONT_BOLD = new Font(Font.MONOSPACED, Font.BOLD, 13);
    private static final Font FONT_SMALL = new Font(Font.MONOSPACED, Font.PLAIN, 11);
    private static final Font FONT_HEAD = new Font(Font.MONOSPACED, Font.BOLD, 14);

    // ── State ────────────────────────────────────────────────────────────
    private String userName;
    private PrintWriter out;
    private BufferedReader in;

    private JFrame mainFrame;
    private JPanel chatPanel;
    private JScrollPane chatScroll;
    private JTextField inputField;
    private JPanel memberPanel;
    private JLabel onlineLabel;

    private final HashMap<String, PrivateChatWindow> dmWindows = new HashMap<>();

    // ────────────────────────────────────────────────────────────────────
    public Client(String host, int port) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        try {
            Socket socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            userName = JOptionPane.showInputDialog(null,
                    "Enter your username:", "LinkUp — Join", JOptionPane.PLAIN_MESSAGE);
            if (userName == null || userName.trim().isEmpty())
                userName = "Guest";
            userName = userName.trim();
            out.println(userName);

            SwingUtilities.invokeLater(() -> {
                buildUI();
                startReaderThread();
            });

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Could not connect: " + e.getMessage(), "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Build UI ─────────────────────────────────────────────────────────
    private void buildUI() {
        mainFrame = new JFrame("LinkUp — General Room");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(960, 660);
        mainFrame.setMinimumSize(new Dimension(720, 480));
        mainFrame.setLocationRelativeTo(null);
        mainFrame.getContentPane().setBackground(BG_DARK);
        mainFrame.setLayout(new BorderLayout());

        // ── Chat column ──────────────────────────────────────────────
        JPanel chatCol = new JPanel(new BorderLayout());
        chatCol.setBackground(BG_DARK);
        chatCol.add(buildHeader(), BorderLayout.NORTH);
        chatCol.add(buildChatScroll(), BorderLayout.CENTER);
        chatCol.add(buildInputBar(), BorderLayout.SOUTH);

        // ── Members sidebar ──────────────────────────────────────────
        JPanel sidebar = buildSidebar();

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chatCol, sidebar);
        split.setDividerLocation(700);
        split.setDividerSize(1);
        split.setBorder(null);
        split.setBackground(DIVIDER);

        mainFrame.add(split, BorderLayout.CENTER);
        mainFrame.setVisible(true);

        appendSystem("Connected as " + userName + " · General Room");
    }

    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(BG_MID);
        h.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, DIVIDER),
                new EmptyBorder(10, 16, 10, 16)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        // Avatar circle
        left.add(makeAvatar("G", ACCENT, 34));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel room = new JLabel("General Room");
        room.setFont(FONT_BOLD);
        room.setForeground(TXT_PRI);
        onlineLabel = new JLabel("• 0 online");
        onlineLabel.setFont(FONT_SMALL);
        onlineLabel.setForeground(ACCENT);
        titles.add(room);
        titles.add(onlineLabel);

        left.add(titles);
        h.add(left, BorderLayout.WEST);
        return h;
    }

    private JScrollPane buildChatScroll() {
        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBackground(BG_DARK);
        chatPanel.setBorder(new EmptyBorder(2, 8, 2, 8));

        chatScroll = new JScrollPane(chatPanel);
        chatScroll.setBorder(null);
        chatScroll.getViewport().setBackground(BG_DARK);
        chatScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        chatScroll.getVerticalScrollBar().setUnitIncrement(20);
        styleScrollBar(chatScroll.getVerticalScrollBar());
        return chatScroll;
    }

    private JPanel buildInputBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setBackground(BG_MID);
        bar.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, DIVIDER),
                new EmptyBorder(10, 14, 10, 14)));

        inputField = makeStyledField("Message General Room…");
        JButton send = makeAccentButton("Send ›");
        send.addActionListener(e -> sendGroup());
        inputField.addActionListener(e -> sendGroup());

        bar.add(inputField, BorderLayout.CENTER);
        bar.add(send, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(BG_PANEL);
        sidebar.setPreferredSize(new Dimension(210, 0));
        sidebar.setBorder(new MatteBorder(0, 1, 0, 0, DIVIDER));

        JLabel title = new JLabel("  Members");
        title.setFont(new Font(Font.MONOSPACED, Font.BOLD, 11));
        title.setForeground(TXT_SEC);
        title.setBorder(new EmptyBorder(14, 8, 8, 8));
        sidebar.add(title, BorderLayout.NORTH);

        memberPanel = new JPanel();
        memberPanel.setLayout(new BoxLayout(memberPanel, BoxLayout.Y_AXIS));
        memberPanel.setBackground(BG_PANEL);
        memberPanel.setBorder(new EmptyBorder(0, 6, 6, 6));

        JScrollPane sp = new JScrollPane(memberPanel);
        sp.setBorder(null);
        sp.getViewport().setBackground(BG_PANEL);
        styleScrollBar(sp.getVerticalScrollBar());
        sidebar.add(sp, BorderLayout.CENTER);
        return sidebar;
    }

    // ── Network ──────────────────────────────────────────────────────────
    private void startReaderThread() {
        new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    final String msg = line;
                    SwingUtilities.invokeLater(() -> dispatch(msg));
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> appendSystem("⚠ Disconnected from server."));
            }
        }, "reader-thread").start();
    }

    private void dispatch(String msg) {
        if (msg.startsWith("FROM:")) {
            String[] p = msg.split(":", 3);
            if (p.length == 3)
                addBubble(chatPanel, chatScroll, p[1], p[2], false);
        } else if (msg.startsWith("PRIVATE_FROM:")) {
            String[] p = msg.split(":", 3);
            if (p.length == 3) {
                PrivateChatWindow win = ensureDMWindow(p[1]);
                win.addBubble(p[1], p[2], false);
            }
        } else if (msg.startsWith("INFO:")) {
            appendSystem(msg.substring(5));
        } else if (msg.startsWith("USERS:")) {
            refreshMembers(msg.substring(6).split(","));
        }
    }

    private void sendGroup() {
        String txt = inputField.getText().trim();
        if (txt.isEmpty())
            return;
        out.println("GROUP:" + txt);
        addBubble(chatPanel, chatScroll, userName, txt, true);
        inputField.setText("");
    }

    // ── Chat bubbles ─────────────────────────────────────────────────────
    private void addBubble(JPanel panel, JScrollPane scroll,
            String sender, String text, boolean mine) {
        boolean right = mine;
        Color bg = mine ? BUBBLE_ME : BUBBLE_OTH;
        String time = new SimpleDateFormat("HH:mm").format(new Date());

        JPanel row = new JPanel(new FlowLayout(right ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Short.MAX_VALUE));

        // Avatar for others
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        if (!mine) {
            JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
            nameRow.setOpaque(false);
            nameRow.add(makeAvatar(sender.substring(0, 1).toUpperCase(), avatarColor(sender), 20));
            col.add(nameRow);
        }

        // Rounded bubble
        JPanel bubble = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.dispose();
            }
        };
        bubble.setOpaque(false);
        bubble.setLayout(new BorderLayout());
        bubble.setBorder(new EmptyBorder(5, 10, 5, 10));

        JLabel lbl = new JLabel("<html><body style='width:230px;font-family:monospace'>"
                + escHtml(text) + "</body></html>");
        lbl.setFont(FONT_MONO);
        lbl.setForeground(TXT_PRI);
        bubble.add(lbl, BorderLayout.CENTER);

        JPanel bRow = new JPanel(new FlowLayout(right ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
        bRow.setOpaque(false);
        bRow.add(bubble);
        col.add(bRow);

        // Timestamp
        JLabel ts = new JLabel(time);
        ts.setFont(FONT_SMALL);
        ts.setForeground(TXT_SEC);
        JPanel tsRow = new JPanel(new FlowLayout(right ? FlowLayout.RIGHT : FlowLayout.LEFT, 4, 0));
        tsRow.setOpaque(false);
        tsRow.add(ts);
        col.add(tsRow);

        row.add(col);
        panel.add(row);
        panel.add(Box.createVerticalStrut(0));
        panel.revalidate();
        panel.repaint();
        scrollToBottom(scroll);
    }

    private void appendSystem(String text) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_SMALL);
        lbl.setForeground(TXT_SEC);
        row.add(lbl);
        chatPanel.add(row);
        chatPanel.add(Box.createVerticalStrut(1));
        chatPanel.revalidate();
        chatPanel.repaint();
        scrollToBottom(chatScroll);
    }

    private void scrollToBottom(JScrollPane sp) {
        SwingUtilities.invokeLater(() -> {
            JScrollBar vsb = sp.getVerticalScrollBar();
            vsb.setValue(vsb.getMaximum());
        });
    }

    // ── Members sidebar ──────────────────────────────────────────────────
    private void refreshMembers(String[] users) {
        memberPanel.removeAll();
        int cnt = 0;
        for (String u : users) {
            if (u.isBlank())
                continue;
            cnt++;
            String name = u.trim();
            JPanel entry = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
            entry.setOpaque(false);
            entry.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            entry.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // Online dot
            JPanel dot = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(DOT_GREEN);
                    g2.fillOval(1, 3, 8, 8);
                    g2.dispose();
                }
            };
            dot.setPreferredSize(new Dimension(10, 14));
            dot.setOpaque(false);

            JLabel lbl = new JLabel(name);
            lbl.setFont(FONT_MONO);
            lbl.setForeground(name.equals(userName) ? ACCENT : TXT_PRI);

            entry.add(dot);
            entry.add(lbl);

            if (!name.equals(userName)) {
                entry.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        ensureDMWindow(name).frame.toFront();
                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        entry.setBackground(DIVIDER);
                        entry.setOpaque(true);
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        entry.setOpaque(false);
                        entry.repaint();
                    }
                });
                JLabel dmHint = new JLabel("DM");
                dmHint.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
                dmHint.setForeground(TXT_SEC);
                entry.add(dmHint);
            }

            memberPanel.add(entry);
        }
        int finalCnt = cnt;
        onlineLabel.setText("• " + finalCnt + " online");
        memberPanel.revalidate();
        memberPanel.repaint();
    }

    // ── DM windows ───────────────────────────────────────────────────────
    private PrivateChatWindow ensureDMWindow(String target) {
        return dmWindows.computeIfAbsent(target, PrivateChatWindow::new);
    }

    class PrivateChatWindow {
        JFrame frame;
        JPanel chatPanel;
        JScrollPane chatScroll;
        JTextField input;
        final String target;

        PrivateChatWindow(String target) {
            this.target = target;

            frame = new JFrame("DM — @" + target);
            frame.setSize(520, 560);
            frame.setLocationRelativeTo(mainFrame);
            frame.getContentPane().setBackground(BG_DARK);
            frame.setLayout(new BorderLayout());

            // Header
            JPanel hdr = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
            hdr.setBackground(BG_MID);
            hdr.setBorder(new MatteBorder(0, 0, 1, 0, DIVIDER));
            JLabel t = new JLabel("@ " + target);
            t.setFont(FONT_HEAD);
            t.setForeground(TXT_PRI);
            hdr.add(makeAvatar(target.substring(0, 1).toUpperCase(), avatarColor(target), 28));
            hdr.add(t);
            frame.add(hdr, BorderLayout.NORTH);

            // Chat
            chatPanel = new JPanel();
            chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
            chatPanel.setBackground(BG_DARK);
            chatPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
            chatScroll = new JScrollPane(chatPanel);
            chatScroll.setBorder(null);
            chatScroll.getViewport().setBackground(BG_DARK);
            chatScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            styleScrollBar(chatScroll.getVerticalScrollBar());
            frame.add(chatScroll, BorderLayout.CENTER);

            // Input
            input = makeStyledField("Message @" + target + "…");
            JButton send = makeAccentButton("Send ›");
            send.addActionListener(e -> sendDM());
            input.addActionListener(e -> sendDM());

            JPanel bar = new JPanel(new BorderLayout(10, 0));
            bar.setBackground(BG_MID);
            bar.setBorder(new CompoundBorder(
                    new MatteBorder(1, 0, 0, 0, DIVIDER),
                    new EmptyBorder(10, 12, 10, 12)));
            bar.add(input, BorderLayout.CENTER);
            bar.add(send, BorderLayout.EAST);
            frame.add(bar, BorderLayout.SOUTH);

            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    dmWindows.remove(target);
                }
            });
            frame.setVisible(true);
        }

        void addBubble(String sender, String text, boolean mine) {
            SwingUtilities.invokeLater(() -> Client.this.addBubble(chatPanel, chatScroll, sender, text, mine));
        }

        void sendDM() {
            String txt = input.getText().trim();
            if (txt.isEmpty())
                return;
            out.println("PRIVATE:" + target + ":" + txt);
            addBubble(userName, txt, true);
            input.setText("");
        }
    }

    // ── Widget helpers ────────────────────────────────────────────────────
    private JLabel makeAvatar(String letter, Color bg, int size) {
        return new JLabel(letter) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillOval(0, 0, getWidth(), getHeight());
                Font f = new Font(Font.MONOSPACED, Font.BOLD, getWidth() / 2);
                g2.setFont(f);
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(letter,
                        (getWidth() - fm.stringWidth(letter)) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(size, size);
            }

            @Override
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }

            @Override
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }
        };
    }

    private JTextField makeStyledField(String hint) {
        JTextField f = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(TXT_SEC);
                    g2.setFont(FONT_MONO);
                    Insets ins = getInsets();
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(hint, ins.left,
                            (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                    g2.dispose();
                }
            }
        };
        f.setBackground(BG_INPUT);
        f.setForeground(TXT_PRI);
        f.setCaretColor(ACCENT);
        f.setFont(FONT_MONO);
        f.setBorder(new CompoundBorder(
                new LineBorder(DIVIDER, 1, true),
                new EmptyBorder(8, 12, 8, 12)));
        return f;
    }

    private JButton makeAccentButton(String label) {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? ACCENT.darker()
                        : getModel().isRollover() ? ACCENT.brighter() : ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(BG_DARK);
                g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(label,
                        (getWidth() - fm.stringWidth(label)) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(86, 38));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void styleScrollBar(JScrollBar sb) {
        sb.setBackground(BG_DARK);
        sb.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = new Color(45, 65, 90);
                trackColor = BG_DARK;
            }

            @Override
            protected JButton createIncreaseButton(int o) {
                return zeroBtn();
            }

            @Override
            protected JButton createDecreaseButton(int o) {
                return zeroBtn();
            }

            private JButton zeroBtn() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
        });
    }

    private Color avatarColor(String name) {
        Color[] palette = {
                new Color(41, 128, 185), new Color(192, 57, 43),
                new Color(142, 68, 173), new Color(39, 174, 96),
                new Color(230, 126, 34), new Color(22, 160, 133)
        };
        return palette[Math.abs(name.hashCode()) % palette.length];
    }

    private String escHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ── Entry point ───────────────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Client("localhost", 8080));
    }
}
