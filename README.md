# Distributed MD5 Password Cracker

## Project Overview

This project transforms a single-machine multithreaded MD5 password cracker into a **Distributed System using Java RMI**. The system distributes workload across multiple RMI servers (simulated using VirtualBox VMs on a single physical machine), with each server further parallelizing work across multiple threads.

### Key Features

- **Client-Server RMI Architecture** - Remote Method Invocation for distributed computing  
- **Static Search-Space Partitioning** - Deterministic, non-overlapping work distribution  
- **Multi-Level Parallelism** - Parallelization at both server and thread levels  
- **Early Termination** - All workers stop when password is found  
- **Comprehensive Logging** - Detailed logs for each server with timestamps  
- **Performance Metrics** - Built-in timing for speedup and efficiency analysis  
- **Thread-Safe Design** - Thread-local MessageDigest instances  
- **Scalable Architecture** - Supports 1-2 servers, 1-10 threads per server  

---

## Hardware and Software Requirements

### Physical Machine
- **Operating System**: Windows/Linux/macOS (host OS)
- **RAM**: Minimum 8GB (16GB recommended)
- **CPU**: Multi-core processor (4+ cores recommended)
- **Disk Space**: 10GB free space for VirtualBox VMs

### VirtualBox Setup
- **VirtualBox**: Version 6.0 or higher
- **Virtual Machines**: 2 VMs (to simulate 2 servers)
  - Each VM: 2GB RAM, 2 CPU cores
  - Network: Bridged Adapter or Host-Only Adapter
  - OS: Ubuntu/Debian Linux (or any Linux distribution)

### Software Requirements
- **Java Development Kit (JDK)**: Version 8 or higher
- **Java RMI**: Included in JDK (no additional installation needed)
- Text editor or IDE (optional): VS Code, IntelliJ IDEA, Eclipse

### Network Configuration
- Ensure VMs can communicate with each other via network
- Note down IP addresses of each VM
- Firewall should allow RMI ports (default: 1099, 1100)

---

## Compilation & Execution

### Step 1: Compile All Java Files

Copy all Java files to your project directory, then compile:

```bash
javac CrackerInterface.java
javac SearchResult.java
javac CrackerServer.java
javac CrackerClient.java
```

Or compile all at once:

```bash
javac *.java
```

---

### Step 2: Start RMI Servers

#### For Single Server Setup (1 VM or Host Machine)

**Terminal 1 - Start Server 1:**
```bash
java CrackerServer Server1 1099
```

#### For Two Server Setup (2 VMs or Host + VM)

**VM1 Terminal - Start Server 1:**
```bash
java CrackerServer Server1 1099
```

**VM2 Terminal - Start Server 2:**
```bash
java CrackerServer Server2 1100
```

**Note**: Servers will automatically create RMI registry on the specified port. You should see:
```
RMI registry created on port 1099
Server 'Server1' is ready and bound to registry
Waiting for client requests...
```

---

### Step 3: Run Client

On the host machine or any VM with network access to the servers:

```bash
java CrackerClient
```

Then follow the interactive prompts:

```
=== Distributed MD5 Password Cracker ===

Enter target MD5 hash (32 hex characters): 5f4dcc3b5aa765d61d8327deb882cf99
Enter password length to search (1-10): 8
Enter number of threads per server (1-10): 5
Enter number of servers to use (1 or 2): 2

Server 1 details:
  Server name (e.g., Server1): Server1
  Host (e.g., localhost): 192.168.1.100
  Port (e.g., 1099): 1099

Server 2 details:
  Server name (e.g., Server1): Server2
  Host (e.g., localhost): 192.168.1.101
  Port (e.g., 1099): 1100
```

**Important**: 
- Replace `192.168.1.100` and `192.168.1.101` with actual IP addresses of your VMs
- For local testing on same machine, use `localhost` for both servers with different ports

---

## Example Outputs

### Example 1: Password Found (2 Servers, 5 Threads Each)

**Client Output:**
```
=== Distributed MD5 Password Cracker ===

Enter target MD5 hash (32 hex characters): fcd4ea256e602c349123aebbc6ff5662
Enter password length to search (1-10): 5
Enter number of threads per server (1-10): 10
Enter number of servers to use (1 or 2): 2

Server 1 details:
  Server name (e.g., Server1): Server1
  Host (e.g., localhost): 192.168.0.26
  Port (e.g., 1099): 1099

Server 2 details:
  Server name (e.g., Server1): Server2
  Host (e.g., localhost): 192.168.0.29
  Port (e.g., 1099): 1100

=== Starting Distributed Search ===
Target Hash: fcd4ea256e602c349123aebbc6ff5662
Password Length: 5
Threads per Server: 10
Number of Servers: 2
Start Time: 2025-12-25 10:43:24

✓ Connected to: Server1 at 192.168.0.26:1099
✓ Connected to: Server2 at 192.168.0.29:1100

Search Space Partitioning:
  Server 1 (Server1): Characters [0, 48) - 48 characters
  Server 2 (Server2): Characters [48, 95) - 47 characters

→ Starting search on Server1...
→ Starting search on Server2...
★ PASSWORD FOUND by Server1!
→ Server2 completed (not found)

============================================================
SEARCH COMPLETED
============================================================
Status: PASSWORD FOUND
Password: 'K^LbU'
Found by Thread: Server1-Thread-9
Found on Server: Server1
Server Search Time: 270197 ms
Total Elapsed Time: 270.256 seconds
End Time: 2025-12-25 10:47:54
============================================================
```

---

### Example 2: Password Not Found

**Client Output:**
```
=== Starting Distributed Search ===
Target Hash: 1234567890abcdef1234567890abcdef
Password Length: 5
Threads per Server: 3
Number of Servers: 2
Start Time: 2025-12-23 11:20:15

Search Space Partitioning:
  Server 1 (Server1): Characters [0, 48) - 48 characters
  Server 2 (Server2): Characters [48, 95) - 47 characters

→ Starting search on Server1...
→ Starting search on Server2...
→ Server1 completed (not found)
→ Server2 completed (not found)

============================================================
SEARCH COMPLETED
============================================================
Status: PASSWORD NOT FOUND
The password was not found in the search space.
Total Elapsed Time: 125.643 seconds
End Time: 2025-12-23 11:22:21
============================================================
```

---

### Example 3: Server Log Output

**Contents of `server_1.log`:**
```
[2025-12-23 10:15:28.123] === Server Initialized: Server1 ===
[2025-12-23 10:15:28.125] Server start time: 2025-12-23 10:15:28.125
[2025-12-23 10:15:30.200] New search request received:
[2025-12-23 10:15:30.201]   Target Hash: 5f4dcc3b5aa765d61d8327deb882cf99
[2025-12-23 10:15:30.202]   Character Range: [0, 48)
[2025-12-23 10:15:30.203]   Number of Threads: 5
[2025-12-23 10:15:30.204]   Password Length: 8
[2025-12-23 10:15:30.205]   Assigned Characters: ' ' to 'O' (48 chars)
[2025-12-23 10:15:30.210] Creating 5 worker threads...
[2025-12-23 10:15:30.211]   Server1-Thread-1 assigned range: [0, 10) = ' ' to ')' (10 chars)
[2025-12-23 10:15:30.212]   Server1-Thread-1 started at 2025-12-23 10:15:30.212
[2025-12-23 10:15:30.213]   Server1-Thread-2 assigned range: [10, 20) = '*' to '3' (10 chars)
[2025-12-23 10:15:30.214]   Server1-Thread-2 started at 2025-12-23 10:15:30.214
[2025-12-23 10:15:30.215]   Server1-Thread-3 assigned range: [20, 29) = '4' to '<' (9 chars)
[2025-12-23 10:15:30.216]   Server1-Thread-3 started at 2025-12-23 10:15:30.216
[2025-12-23 10:15:30.217]   Server1-Thread-4 assigned range: [29, 38) = '=' to 'F' (9 chars)
[2025-12-23 10:15:30.218]   Server1-Thread-4 started at 2025-12-23 10:15:30.218
[2025-12-23 10:15:30.219]   Server1-Thread-5 assigned range: [38, 48) = 'G' to 'O' (10 chars)
[2025-12-23 10:15:30.220]   Server1-Thread-5 started at 2025-12-23 10:15:30.220
[2025-12-23 10:16:16.108]   Server1-Thread-1 stopped at 2025-12-23 10:16:16.108
[2025-12-23 10:16:16.109]   Server1-Thread-2 stopped at 2025-12-23 10:16:16.109
[2025-12-23 10:16:16.110]   Server1-Thread-3 stopped at 2025-12-23 10:16:16.110
[2025-12-23 10:16:16.111]   Server1-Thread-4 stopped at 2025-12-23 10:16:16.111
[2025-12-23 10:16:16.112]   Server1-Thread-5 stopped at 2025-12-23 10:16:16.112
[2025-12-23 10:16:16.115] PASSWORD FOUND: 'password' by Server1-Thread-3
[2025-12-23 10:16:16.116] Search completed in 45906 ms
```

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      CrackerClient                          │
│  • Takes user input                                         │
│  • Partitions search space                                  │
│  • Coordinates servers via RMI                              │
│  • Aggregates results                                       │
└───────────────┬─────────────────────────┬───────────────────┘
                │ RMI                     │ RMI
    ┌───────────▼──────────┐   ┌──────────▼───────────┐
    │   CrackerServer 1    │   │   CrackerServer 2    │
    │ (VM1 / localhost)    │   │ (VM2 / localhost)    │
    │  • Range: [0, 48)    │   │  • Range: [48, 95)   │
    │  • Creates threads   │   │  • Creates threads   │
    │  • Logs to file      │   │  • Logs to file      │
    └──────────┬───────────┘   └───────────┬──────────┘
               │                           │
        ┌──────┴──────┐             ┌──────┴──────┐
        │   Thread 1  │             │   Thread 1  │
        │   Thread 2  │             │   Thread 2  │
        │   Thread 3  │             │   Thread 3  │
        │   ...       │             │   ...       │
        └─────────────┘             └─────────────┘
```

---

## Search Space Partitioning Explained

### Overview
The system uses **hierarchical static partitioning** to divide work across servers and threads without overlap.

### Character Set
- **Total**: 95 printable ASCII characters (space to tilde)
- **Indexed**: 0 to 94
- **Examples**: ' ' (0), '!' (1), 'A' (33), 'a' (65), '~' (94)

### Level 1: Server-Level Partitioning

**Algorithm:**
```
totalChars = 95
baseChunk = totalChars / numServers
remainder = totalChars % numServers

For each server i:
    if i < remainder:
        chunkSize = baseChunk + 1
    else:
        chunkSize = baseChunk
    
    startIndex = previous_endIndex
    endIndex = startIndex + chunkSize
```

**Example: 2 Servers**
- **Server 1**: Characters [0, 48) = 48 characters (' ' through 'O')
- **Server 2**: Characters [48, 95) = 47 characters ('P' through '~')

### Level 2: Thread-Level Partitioning

Each server applies the same algorithm to divide its assigned character range across threads.

**Example: Server 1 with 5 Threads (range [0, 48))**
```
48 / 5 = 9 remainder 3

Thread 1: [0, 10)   = 10 characters (gets +1 from remainder)
Thread 2: [10, 20)  = 10 characters (gets +1 from remainder)
Thread 3: [20, 29)  = 9 characters  (gets +1 from remainder)
Thread 4: [29, 38)  = 9 characters
Thread 5: [38, 48)  = 10 characters
```

### Complete Example Visualization

**Configuration**: 2 servers, 3 threads each, password length 4

```
Total Search Space: 95^4 = 81,450,625 combinations

┌────────────────────────────────────────────────────────┐
│              SERVER 1 (Characters 0-47)                │
│                    48 characters                       │
├─────────────────┬─────────────────┬────────────────────┤
│   Thread 1      │   Thread 2      │    Thread 3        │
│  Chars [0-16)   │  Chars [16-32)  │   Chars [32-48)    │
│  16 characters  │  16 characters  │   16 characters    │
│  16×95³ combos  │  16×95³ combos  │   16×95³ combos    │
└─────────────────┴─────────────────┴────────────────────┘

┌────────────────────────────────────────────────────────┐
│              SERVER 2 (Characters 48-94)               │
│                    47 characters                       │
├──────────────────┬──────────────────┬──────────────────┤
│   Thread 1       │   Thread 2       │    Thread 3      │
│  Chars [48-63)   │  Chars [63-79)   │   Chars [79-95)  │
│  15 characters   │  16 characters   │   16 characters  │
│  15×95³ combos   │  16×95³ combos   │   16×95³ combos  │
└──────────────────┴──────────────────┴──────────────────┘
```

### Mathematical Properties

- **Non-overlapping**: Each thread searches unique character combinations  
- **Complete Coverage**: All 95^L combinations are searched exactly once  
- **Balanced Distribution**: Max difference of 1 character between workers  
- **Deterministic**: Same configuration always produces same partitioning  

### What Each Thread Searches

For password length **L**, a thread with character range **[start, end)** searches:
- **First character**: One from its assigned range
- **Remaining L-1 characters**: Any of the 95 characters
- **Total combinations**: `(end - start) × 95^(L-1)`

**Example**: Thread with range [0, 16), password length 4
```
Combinations = 16 × 95³ = 16 × 857,375 = 13,718,000
```

### Analysis Points

- **Speedup**: Should increase with more threads but plateau due to overhead
- **Efficiency**: Typically decreases with more threads (< 1.0 is normal)
- **Comparison**: 1 server with 10 threads vs. 2 servers with 5 threads each
- **Bottlenecks**: CPU, memory bandwidth, RMI network overhead

---

## Troubleshooting

### Problem: "Connection refused" or "ConnectException"

**Causes:**
- Servers not running
- Incorrect IP address or port
- Firewall blocking connections

**Solutions:**
```bash
# Verify server is running
# You should see "Server 'ServerX' is ready" message

# Check IP address (on server VM)
ip addr show  # Linux
ipconfig      # Windows

# Test network connectivity from client
ping 192.168.1.100

# Check if port is listening (on server VM)
netstat -an | grep 1099

# Temporarily disable firewall for testing
sudo ufw disable  # Ubuntu/Debian
```

### Problem: "java.rmi.NotBoundException: Server1"

**Cause:** Server name mismatch

**Solution:**
- Ensure server name in client exactly matches server startup name
- Names are case-sensitive: `Server1` ≠ `server1`

### Problem: "Password not found in search space"

**Causes:**
- Password is longer than specified search length
- Password contains non-ASCII characters
- Incorrect MD5 hash

**Solutions:**
- Increase password length parameter
- Verify MD5 hash is correct using online MD5 calculator
- Test with known simple passwords first (e.g., "abc", "test")

### Problem: Server freezes or becomes unresponsive

**Causes:**
- Too many threads for available CPU
- Memory exhaustion

**Solutions:**
```bash
# Monitor resources
top        # Linux
htop       # Linux (better)

# Reduce number of threads per server
# Recommended: threads ≤ CPU cores

# Increase VM memory allocation
# VirtualBox → Settings → System → Base Memory
```

### Problem: Very slow performance on VirtualBox

**Causes:**
- Insufficient VM resources
- VT-x/AMD-V not enabled

**Solutions:**
- Enable hardware virtualization in BIOS/UEFI
- Allocate more CPU cores to VMs
- Use Host-Only or Bridged networking (not NAT)
- Disable unnecessary services in VMs

### Problem: Logs not being generated

**Cause:** Permission issues or wrong directory

**Solution:**
```bash
# Check current directory
pwd

# Verify write permissions
ls -la

# Logs are created in the directory where server is run
# Look for: server_1.log, server_2.log, etc.
```

---

## VirtualBox Setup Guide

### Creating VMs

1. **Create VM1 (Server 1)**:
   - Name: Server1-VM
   - Type: Linux, Ubuntu (64-bit)
   - Memory: 2048 MB
   - CPU: 2 cores
   - Network: Bridged Adapter

2. **Create VM2 (Server 2)**:
   - Name: Server2-VM
   - Type: Linux, Ubuntu (64-bit)
   - Memory: 2048 MB
   - CPU: 2 cores
   - Network: Bridged Adapter

3. **Install Java on both VMs**:
```bash
sudo apt update
sudo apt install default-jdk
java -version  # Verify installation
```

4. **Compile all Java Files**
```bash
javac *.java
```

5. **Find VM IP addresses:**
```bash
# On each VM
ip addr show | grep inet
```

### Running Tests

#### Two Server Setup (2 VMs)

**VM1 Terminal - Start Server 1:**
```bash
java -Djava.rmi.server.hostname=192.168.0.26 -Djava.net.preferIPv4Stack=true CrackerServer Server1 1099
```

**VM2 Terminal - Start Server 2:**
```bash
java -Djava.rmi.server.hostname=192.168.0.29 -Djava.net.preferIPv4Stack=true CrackerServer Server2 1100
```
Replace `192.168.0.26` and `192.168.0.29` with actual IP addresses of your VMs

#### Client
```bash
java CrackerClient
```

```
Server 1 details:
  Server name (e.g., Server1): Server1
  Host (e.g., localhost): 192.168.0.26
  Port (e.g., 1099): 1099

Server 2 details:
  Server name (e.g., Server1): Server2
  Host (e.g., localhost): 192.168.0.29
  Port (e.g., 1099): 1100
```
Replace `192.168.0.26` and `192.168.0.29` with actual IP addresses of your VMs

### Test Hash Values

2-char `yR`

```bash
36b42ccc5a415eae329e72d13978ef18
```

3-char `*Rt`

```bash
83f73c1cd3155845c82b86c55e6c2ddf
```

4-char `,IO2`

```bash
6fe3664a4c30d84e0aee3b950d1f425f
```

5-char `K^LbU`

```bash
fcd4ea256e602c349123aebbc6ff5662
```

6-char `CS_Bv@`

```bash
26f8d7b35de91cc3248465cc6040d655
```

---

## Design Benefits

1. **Deterministic Partitioning**: No race conditions or duplicate work
2. **Scalability**: Easy to add more servers or threads
3. **Fault Isolation**: One server failing doesn't affect others  
4. **Load Balancing**: Even distribution of work across all resources
5. **Early Termination**: Global stop signal when password is found
6. **Comprehensive Logging**: Full audit trail for debugging and analysis
7. **Thread Safety**: Thread-local MessageDigest instances prevent contention
8. **Clean Architecture**: Clear separation between client and server logic

---

## Security & Educational Note

⚠️ **This tool is for educational purposes only.**

- MD5 is cryptographically broken and should **never** be used for password hashing
- Modern systems use **bcrypt**, **scrypt**, or **Argon2**
- This project demonstrates distributed computing concepts, not security best practices
- Do not use this tool for unauthorized password cracking

---

## References & Further Reading

- **Java RMI Tutorial**: https://docs.oracle.com/javase/tutorial/rmi/
- **MD5 Algorithm**: https://en.wikipedia.org/wiki/MD5
- **Parallel Computing**: Introduction to Parallel Computing (Grama et al.)
- **Distributed Systems**: Distributed Systems: Principles and Paradigms (Tanenbaum)