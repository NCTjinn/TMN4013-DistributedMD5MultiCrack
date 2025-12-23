# Distributed MD5 Password Cracker - Documentation

## Overview
This system transforms a single-machine multithreaded password cracker into a distributed system using Java RMI, allowing workload distribution across multiple servers with multiple threads each.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      CrackerClient                          │
│  • Takes user input                                         │
│  • Partitions search space                                  │
│  • Coordinates servers                                      │
│  • Aggregates results                                       │
└───────────────┬─────────────────────────┬───────────────────┘
                │ RMI                     │ RMI
    ┌───────────▼──────────┐   ┌──────────▼───────────┐
    │   CrackerServer 1    │   │   CrackerServer 2    │
    │  • Range: [0, 48)    │   │  • Range: [48, 95)   │
    │  • Creates threads   │   │  • Creates threads   │
    │  • Logs activity     │   │  • Logs activity     │
    └──────────┬───────────┘   └───────────┬──────────┘
               │                           │
        ┌──────┴──────┐             ┌──────┴──────┐
        │   Thread 1  │             │   Thread 1  │
        │   Thread 2  │             │   Thread 2  │
        │   ...       │             │   ...       │
        └─────────────┘             └─────────────┘
```

## Search Space Partitioning Strategy

### Level 1: Server-Level Partitioning

The system uses **character set partitioning** based on the first character of passwords.

**Character Set**: 95 printable ASCII characters (space to tilde, indices 0-94)

**Mathematical Division**:
```
For N servers:
  baseChunk = 95 / N
  remainder = 95 % N
  
For each server i:
  startIndex = Σ(j=0 to i-1)[baseChunk + (j < remainder ? 1 : 0)]
  chunkSize = baseChunk + (i < remainder ? 1 : 0)
  endIndex = startIndex + chunkSize
```

**Example with 2 Servers**:
- **Server 1**: Characters [0, 48) = 48 characters
  - Includes: space through 'O'
  - First chars: ' ', '!', '"', ..., 'N', 'O'
  
- **Server 2**: Characters [48, 95) = 47 characters
  - Includes: 'P' through '~'
  - First chars: 'P', 'Q', 'R', ..., '}', '~'

**Properties**:
✓ Non-overlapping ranges (endIndex of server i = startIndex of server i+1)
✓ Complete coverage (all 95 characters assigned)
✓ Balanced distribution (max difference of 1 character)

### Level 2: Thread-Level Partitioning

Each server further divides its assigned character range across its threads using the **same algorithm**.

**Example**: Server 1 with 3 threads (range [0, 48)):
```
baseChunk = 48 / 3 = 16
remainder = 48 % 3 = 0

Thread 1: [0, 16)   = 16 characters
Thread 2: [16, 32)  = 16 characters  
Thread 3: [32, 48)  = 16 characters
```

### Complete Example

**Configuration**: 2 servers, 3 threads each, password length 3

**Server 1** searches all passwords starting with characters [0, 48):
- Thread 1: First char in [0, 16), all combos for positions 2-3
- Thread 2: First char in [16, 32), all combos for positions 2-3
- Thread 3: First char in [32, 48), all combos for positions 2-3

**Server 2** searches all passwords starting with characters [48, 95):
- Thread 1: First char in [48, 63), all combos for positions 2-3
- Thread 2: First char in [63, 79), all combos for positions 2-3
- Thread 3: First char in [79, 95), all combos for positions 2-3

**Total Search Space**: 95 × 95 × 95 = 857,375 combinations

## File Descriptions

### 1. `CrackerInterface.java`
Remote interface defining the RMI contract:
- `searchPassword()`: Initiates search with specific parameters
- `stopSearch()`: Signals server to halt all operations
- `ping()`: Health check method

### 2. `SearchResult.java`
Serializable data class containing:
- Whether password was found
- The password (if found)
- Thread that found it
- Server that found it
- Search time

### 3. `CrackerServer.java`
RMI server implementation:
- Accepts search requests via RMI
- Creates and manages worker threads
- Implements brute-force algorithm
- Generates detailed log file (`server_name.log`)
- Handles stop signals from client

**Key Features**:
- Thread-local MessageDigest for thread safety
- Atomic variables for coordination
- Comprehensive logging with timestamps
- Exception handling

### 4. `CrackerClient.java`
CLI client that:
- Takes user input (hash, threads, servers, length)
- Connects to RMI servers
- Partitions and distributes work
- Manages concurrent searches
- Signals servers to stop when password found
- Displays results

## Compilation

```bash
# Compile all files
javac CrackerInterface.java
javac SearchResult.java
javac CrackerServer.java
javac CrackerClient.java
```

## Usage

### Step 1: Start RMI Registry (Optional)
The servers will create their own registries, but you can start one manually:
```bash
rmiregistry 1099 &
rmiregistry 1100 &
```

### Step 2: Start Servers

**Terminal 1 - Server 1**:
```bash
java CrackerServer Server1 1099
```

**Terminal 2 - Server 2** (if using 2 servers):
```bash
java CrackerServer Server2 1100
```

### Step 3: Run Client

**Terminal 3**:
```bash
java CrackerClient
```

Follow the prompts:
```
Enter target MD5 hash: 5f4dcc3b5aa765d61d8327deb882cf99
Enter password length to search: 8
Enter number of threads per server: 5
Enter number of servers to use: 2

Server 1 details:
  Server name: Server1
  Host: localhost
  Port: 1099

Server 2 details:
  Server name: Server2
  Host: localhost
  Port: 1100
```

## Example Test Cases

### Test Case 1: Single Character Password
```
MD5: 5d41402abc4b2a76b9719d911017c592
Password: "hello" (will find if length=5)
Threads: 3
Servers: 1
```

### Test Case 2: "password"
```
MD5: 5f4dcc3b5aa765d61d8327deb882cf99
Password: "password"
Length: 8
Threads: 5
Servers: 2
```

### Test Case 3: Short Password
```
MD5: 900150983cd24fb0d6963f7d28e17f72
Password: "abc"
Length: 3
Threads: 4
Servers: 2
```

## Log Files

Each server generates a log file: `server_1.log`, `server_2.log`, etc.

**Log Contents**:
```
[2024-12-23 10:15:30.123] === Server Initialized: Server1 ===
[2024-12-23 10:15:30.125] Server start time: 2024-12-23 10:15:30.125
[2024-12-23 10:16:45.200] New search request received:
[2024-12-23 10:16:45.201]   Target Hash: 5f4dcc3b5aa765d61d8327deb882cf99
[2024-12-23 10:16:45.202]   Character Range: [0, 48)
[2024-12-23 10:16:45.203]   Number of Threads: 5
[2024-12-23 10:16:45.204]   Password Length: 8
[2024-12-23 10:16:45.205]   Assigned Characters: ' ' to 'O' (48 chars)
[2024-12-23 10:16:45.210] Creating 5 worker threads...
[2024-12-23 10:16:45.211]   Server1-Thread-1 assigned range: [0, 10)
[2024-12-23 10:16:45.212]   Server1-Thread-1 started at 2024-12-23 10:16:45.212
...
[2024-12-23 10:18:22.456] PASSWORD FOUND: 'password' by Server1-Thread-3
[2024-12-23 10:18:22.457] Search completed in 97245 ms
```

## Performance Analysis

### Metrics to Collect

**Speedup**: 
```
S = T₁ / Tₙ
where T₁ = time with 1 thread, Tₙ = time with n threads/servers
```

**Efficiency**:
```
E = S / n = T₁ / (n × Tₙ)
where n = total number of threads across all servers
```

### Experimental Setup

Run the same hash with different configurations:
1. 1 server, 1 thread (baseline)
2. 1 server, 2 threads
3. 1 server, 5 threads
4. 2 servers, 5 threads each

Record the "Total Elapsed Time" for each run.

## Troubleshooting

### "Connection refused"
- Ensure servers are running before starting client
- Check firewall settings
- Verify correct host and port numbers

### "java.rmi.NotBoundException"
- Server name in client must match exactly
- Server must be fully started before client connects

### "Password not found"
- Increase password length parameter
- Verify MD5 hash is correct (32 hex characters)
- Password might be longer than search range

### Performance Issues
- Increase number of threads (up to CPU core count)
- Use multiple servers to distribute load
- Ensure password length matches actual password

## Security Note

This tool is for **educational purposes only**. MD5 is cryptographically broken and should not be used for password storage. Modern systems use bcrypt, scrypt, or Argon2.

## Key Design Benefits

1. **Deterministic Partitioning**: No work duplication
2. **Scalability**: Easy to add more servers
3. **Fault Tolerance**: One server failing doesn't affect others
4. **Load Balancing**: Even distribution across resources
5. **Early Termination**: All workers stop when password found
6. **Comprehensive Logging**: Full audit trail for analysis