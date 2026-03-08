# LinkUp ChatApp — Improved

## Changes Made

### Bug Fixes
- **Thread Safety**: All Swing UI updates now wrapped in `SwingUtilities.invokeLater()` — prevents race conditions and UI crashes
- **Port Consistency**: Both Server and Client now use port `8080` (was mismatched in original)
- **Null Checks**: Added `isBlank()` guards on username and message parsing
- **Duplicate Username**: Server now rejects duplicate usernames with a helpful message
- **Offline DM**: Server notifies sender if target user is offline
- **Message Parsing**: Verified all protocol split operations use `:` with limit 3 correctly

### UI Overhaul (Client)
- Dark theme: `#0D1117` backgrounds with teal `#00D4AA` accent
- Chat bubble layout: right-aligned (you) vs left-aligned (others)
- Per-user color avatars generated from username hash
- Timestamps on every message
- Inline placeholder text on input fields
- Custom-painted rounded Send button with hover/press states
- Members sidebar with green online dot + click-to-DM
- Online user count in header
- Styled scrollbars
- System messages centered in a subtle gray

### Server Logging
- `[INFO]` startup banner
- `[+]` / `[-]` connection events with thread name
- `[GROUP]` broadcast log
- `[DM]` private message log with sender/recipient

## How to Run

```bash
# Compile
javac Server.java Client.java

# Start server (one terminal)
java Server

# Start clients (separate terminals)
java Client
```
