# Quick Start Guide

## One-Server Setup (Simplest)

### Terminal 1 - Start Server
```bash
java CrackerServer Server1 1099
```

### Terminal 2 - Run Client
```bash
java CrackerClient
```
Then enter:
```
Enter target MD5 hash: 5f4dcc3b5aa765d61d8327deb882cf99
Enter password length to search: 8
Enter number of threads per server: 5
Enter number of servers to use: 1

Server 1 details:
  Server name: Server1
  Host: localhost
  Port: 1099
```

---

## Two-Server Setup (Distributed)

### Terminal 1 - Start Server 1
```bash
java CrackerServer Server1 1099
```

### Terminal 2 - Start Server 2
```bash
java CrackerServer Server2 1100
```

### Terminal 3 - Run Client
```bash
java CrackerClient
```
Then enter:
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

---

## Common Test Passwords

| Password | MD5 Hash | Length |
|----------|----------|--------|
| `abc` | `900150983cd24fb0d6963f7d28e17f72` | 3 |
| `test` | `098f6bcd4621d373cade4e832627b4f6` | 4 |
| `hello` | `5d41402abc4b2a76b9719d911017c592` | 5 |
| `123456` | `e10adc3949ba59abbe56e057f20f883e` | 6 |
| `password` | `5f4dcc3b5aa765d61d8327deb882cf99` | 8 |

---

## Search Space Partitioning Visualization

### Example: 2 Servers, 3 Threads Each, Length 3

```
Total Character Set: 95 printable ASCII chars [index 0-94]
                     Space...0-9...A-Z...a-z...~

┌────────────────────────────────────────────────────────┐
│              SERVER 1 (Characters 0-47)                │
│                    48 characters                       │
├─────────────────┬─────────────────┬────────────────────┤
│   Thread 1      │   Thread 2      │    Thread 3        │
│  Chars [0-16)   │  Chars [16-32)  │   Chars [32-48)    │
│  16 characters  │  16 characters  │   16 characters    │
└─────────────────┴─────────────────┴────────────────────┘

┌────────────────────────────────────────────────────────┐
│              SERVER 2 (Characters 48-94)               │
│                    47 characters                       │
├──────────────────┬──────────────────┬──────────────────┤
│   Thread 1       │   Thread 2       │    Thread 3      │
│  Chars [48-63)   │  Chars [63-79)   │   Chars [79-95)  │
│  15 characters   │  16 characters   │   16 characters  │
└──────────────────┴──────────────────┴──────────────────┘
```

### What Each Thread Searches

**Server1-Thread-1** (chars 0-15):
- First character: one of [space, !, ", #, ..., .]
- Second character: ANY of 95 chars
- Third character: ANY of 95 chars
- Total: 16 × 95 × 95 = 144,400 combinations

**Server1-Thread-2** (chars 16-31):
- First character: one of [/, 0, 1, 2, ..., ?]
- Positions 2-3: all 95 chars
- Total: 16 × 95 × 95 = 144,400 combinations

...and so on for all threads.

**Total**: 95 × 95 × 95 = 857,375 combinations
**Per Server**: ~428,688 combinations
**Per Thread**: ~142,896 combinations

---

## Mathematical Partitioning Formula

```python
def partition_search_space(total_chars=95, num_servers=2):
    """
    Partitions character indices across servers.
    
    Returns: List of (start_index, end_index) tuples
    """
    base_chunk = total_chars // num_servers
    remainder = total_chars % num_servers
    
    partitions = []
    cursor = 0
    
    for i in range(num_servers):
        start = cursor
        chunk_size = base_chunk + (1 if i < remainder else 0)
        end = start + chunk_size
        partitions.append((start, end))
        cursor = end
    
    return partitions

# Example: 2 servers
# Output: [(0, 48), (48, 95)]
```

The same formula is applied **twice**:
1. At client level: partition chars across servers
2. At server level: partition assigned chars across threads

---

## Expected Output

```
=== Distributed MD5 Password Cracker ===

Enter target MD5 hash: 5f4dcc3b5aa765d61d8327deb882cf99
Enter password length to search: 8
Enter number of threads per server: 5
Enter number of servers to use: 2

[Connection messages...]

=== Starting Distributed Search ===
Target Hash: 5f4dcc3b5aa765d61d8327deb882cf99
Password Length: 8
Threads per Server: 5
Number of Servers: 2
Start Time: 2024-12-23 10:15:30

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
Password: 'password'
Found by Thread: Server1-Thread-3
Found on Server: Server1
Server Search Time: 45678 ms
Total Elapsed Time: 45.982 seconds
End Time: 2024-12-23 10:16:16
============================================================
```

---

## Performance Testing Script

Create this bash script to automate performance tests:

```bash
#!/bin/bash
# performance_test.sh

HASH="5f4dcc3b5aa765d61d8327deb882cf99"  # "password"
LENGTH=8

echo "Configuration,Threads,Servers,Time(s)" > results.csv

# Test different configurations
for servers in 1 2; do
    for threads in 1 2 3 5 10; do
        echo "Testing: $servers servers, $threads threads..."
        
        # Extract time from output (you'll need to parse this)
        # Run client and capture output
        # Append to results.csv
    done
done

echo "Results saved to results.csv"
```

---

## Troubleshooting Checklist

- [ ] All Java files compiled without errors
- [ ] Servers started before client
- [ ] Server names match exactly (case-sensitive)
- [ ] Ports are not already in use
- [ ] Firewall allows connections on specified ports
- [ ] MD5 hash is exactly 32 hexadecimal characters
- [ ] Password length matches actual password length
- [ ] VirtualBox network configured for inter-VM communication

---

## Next Steps

1. **Compile** all Java files
2. **Start servers** in separate terminals
3. **Run client** and enter parameters
4. **Check logs** in `server_1.log` and `server_2.log`
5. **Measure performance** with different configurations
6. **Calculate speedup** and efficiency metrics
7. **Document findings** for your assignment report