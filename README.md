# 💬 LinkUp Chat Engine:
A real-time chat application inspired by WhatsApp, built using Java Socket Programming and Multithreading.
LinkUp allows users to communicate through group chats and private messages while the server efficiently manages multiple client connections.

## 🚀 Features:
💬 Real-time group messaging <br>
👤 Private messaging between users <br>
🧑‍🤝‍🧑 Live online user list <br>
⚡ Multi-client support using multithreading <br>
🔐 Thread-safe user management with ConcurrentHashMap <br>
📡 Lightweight custom messaging protocol <br>

## 🏗 Architecture:

The server follows a thread-per-client model.
Each connected user is handled by a separate thread.
new Thread(new ClientHandler(socket)).start();
This allows multiple users to chat simultaneously without blocking the server.

High Level Architecture
```bash
Client 1  ──┐
Client 2  ──┤
Client 3  ──┤  →  LinkUp Server  → Message Routing
Client 4  ──┘
```
## 📡 Messaging Protocol
The application uses a simple string-based protocol to route messages.

<h3>Client → Server</h3>
<table border="1">
  <tr>
    <th>Format</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>GROUP:message</td>
    <td>Send message to all users</td>
  </tr>
  <tr>
    <td>PRIVATE:user:message</td>
    <td>Send private message</td>
  </tr>
</table>

<h3>Server → Client</h3>
<table border="1">
  <tr>
    <th>Format</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>INFO:</td>
    <td>System notification</td>
  </tr>
  <tr>
    <td>USERS:</td>
    <td>List of online users</td>
  </tr>
  <tr>
    <td>FROM:user:message</td>
    <td>Group message</td>
  </tr>
  <tr>
    <td>PRIVATE_FROM:user:message</td>
    <td>Private message</td>
  </tr>
</table>

## 🧠 User Management

The server maintains active users using:
ConcurrentHashMap<String, PrintWriter> userMap
This ensures thread-safe communication between multiple connected clients.

## ▶️ How to Run
### Compile
```bash
javac Server.java
javac Client.java
```
### Run Server 
```bash
java Server
```
### Run Client
```bash
java Client
```
You can start multiple clients to simulate a group chat.
## 🛠 Technologies Used

Java
Socket Programming
Multithreading
ConcurrentHashMap


## 👨‍💻 Authors

This project is the result of a collaborative effort by a dedicated team of Computer Science students from **Techno India University**.

- **Devraj Maji** — B.Tech CSE  
- **Debasish Chandra**  — B.Tech CSE
- **Tridip Nayek**  — B.Tech CSE
- **Nirmalya Sen**  — B.Tech CSE

✨ *Built with teamwork, creativity, and a shared passion for technology and innovation.*
