# Mathematical Explanation of Search Space Partitioning

## Overview

The distributed password cracker uses **hierarchical static partitioning** to divide the search space across servers and threads without any overlap. This document explains the mathematical approach in detail.

---

## Foundation: The Character Set

The system uses **printable ASCII characters** as the password alphabet:
- **Range**: ASCII 32 (space) to ASCII 126 (tilde)
- **Total**: 95 characters
- **Indexed**: 0 to 94

```
Index:  0   1   2   3   ...  47  48  49  ...  93  94
Char:  ' ' '!' '"' '#'  ...  'O' 'P' 'Q'  ...  '}' '~'
```

---

## Level 1: Server-Level Partitioning

### Problem Statement
Given:
- **C** = 95 (total characters)
- **S** = number of servers (1 or 2)

Distribute **C** characters across **S** servers such that:
1. Each server gets a contiguous range of indices
2. Ranges are non-overlapping
3. Union of all ranges covers all 95 characters
4. Distribution is as balanced as possible

### Algorithm

```
baseChunk = ⌊C / S⌋
remainder = C mod S

For server i (where i = 0, 1, ..., S-1):
    if i < remainder:
        chunkSize[i] = baseChunk + 1
    else:
        chunkSize[i] = baseChunk
    
    startIndex[i] = Σ(j=0 to i-1) chunkSize[j]
    endIndex[i] = startIndex[i] + chunkSize[i]
```

### Example Calculations

#### Case 1: 1 Server
```
C = 95, S = 1
baseChunk = ⌊95/1⌋ = 95
remainder = 95 mod 1 = 0

Server 0:
    chunkSize = 95
    startIndex = 0
    endIndex = 95
    range = [0, 95)
```

#### Case 2: 2 Servers
```
C = 95, S = 2
baseChunk = ⌊95/2⌋ = 47
remainder = 95 mod 2 = 1

Server 0:
    chunkSize = 47 + 1 = 48  (because 0 < 1)
    startIndex = 0
    endIndex = 0 + 48 = 48
    range = [0, 48)

Server 1:
    chunkSize = 47 + 0 = 47  (because 1 ≥ 1)
    startIndex = 48
    endIndex = 48 + 47 = 95
    range = [48, 95)
```

**Verification**:
- Non-overlapping: ✓ (48 is not in both ranges)
- Complete coverage: ✓ (0 to 47 + 48 to 94 = 0 to 94)
- Balanced: ✓ (difference is 1 character)

---

## Level 2: Thread-Level Partitioning

### Problem Statement
Each server receives a character range [startChar, endChar).

Given:
- **R** = endChar - startChar (size of server's range)
- **T** = number of threads per server (1 to 10)

Distribute **R** characters across **T** threads using the same algorithm.

### Algorithm

```
For server with range [startChar, endChar):
    R = endChar - startChar
    baseChunk = ⌊R / T⌋
    remainder = R mod T
    
    For thread t (where t = 0, 1, ..., T-1):
        if t < remainder:
            chunkSize[t] = baseChunk + 1
        else:
            chunkSize[t] = baseChunk
        
        threadStart[t] = startChar + Σ(j=0 to t-1) chunkSize[j]
        threadEnd[t] = threadStart[t] + chunkSize[t]
```

### Example Calculations

#### Server 1 with 3 Threads (range [0, 48))
```
R = 48 - 0 = 48
T = 3
baseChunk = ⌊48/3⌋ = 16
remainder = 48 mod 3 = 0

Thread 0:
    chunkSize = 16 + 0 = 16
    threadStart = 0 + 0 = 0
    threadEnd = 0 + 16 = 16
    range = [0, 16)

Thread 1:
    chunkSize = 16 + 0 = 16
    threadStart = 0 + 16 = 16
    threadEnd = 16 + 16 = 32
    range = [16, 32)

Thread 2:
    chunkSize = 16 + 0 = 16
    threadStart = 0 + 32 = 32
    threadEnd = 32 + 16 = 48
    range = [32, 48)
```

#### Server 2 with 3 Threads (range [48, 95))
```
R = 95 - 48 = 47
T = 3
baseChunk = ⌊47/3⌋ = 15
remainder = 47 mod 3 = 2

Thread 0:
    chunkSize = 15 + 1 = 16  (0 < 2)
    threadStart = 48 + 0 = 48
    threadEnd = 48 + 16 = 64
    range = [48, 64)

Thread 1:
    chunkSize = 15 + 1 = 16  (1 < 2)
    threadStart = 48 + 16 = 64
    threadEnd = 64 + 16 = 80
    range = [64, 80)

Thread 2:
    chunkSize = 15 + 0 = 15  (2 ≥ 2)
    threadStart = 48 + 32 = 80
    threadEnd = 80 + 15 = 95
    range = [80, 95)
```

---

## Search Space per Thread

### For Password of Length L

Each thread searches all passwords that:
1. Start with a character from its assigned range
2. Have any of the 95 characters in remaining positions

**Total combinations per thread**:
```
For thread with character range [start, end):
    numStartChars = end - start
    totalCombinations = numStartChars × 95^(L-1)
```

### Example: Length 4, Thread range [0, 16)
```
numStartChars = 16 - 0 = 16
totalCombinations = 16 × 95^3
                  = 16 × 857,375
                  = 13,718,000 combinations
```

---

## Complete System Example

### Configuration
- **2 servers**
- **3 threads per server**  
- **Password length = 4**

### Server-Level Partitioning
```
Server 1: [0, 48)   → 48 characters
Server 2: [48, 95)  → 47 characters
```

### Thread-Level Partitioning

**Server 1 Threads**:
```
Thread 1: [0, 16)   → 16 chars × 95³ = 13,718,000 combos
Thread 2: [16, 32)  → 16 chars × 95³ = 13,718,000 combos
Thread 3: [32, 48)  → 16 chars × 95³ = 13,718,000 combos
Server 1 Total: 48 × 95³ = 41,154,000 combinations
```

**Server 2 Threads**:
```
Thread 1: [48, 64)  → 16 chars × 95³ = 13,718,000 combos
Thread 2: [64, 80)  → 16 chars × 95³ = 13,718,000 combos
Thread 3: [80, 95)  → 15 chars × 95³ = 12,860,625 combos
Server 2 Total: 47 × 95³ = 40,296,625 combinations
```

**Grand Total**: 95⁴ = 81,450,625 combinations

---

## Proof of Correctness

### Theorem 1: Non-Overlapping Ranges
For any two threads i and j:
```
threadEnd[i] = threadStart[i+1]  (consecutive threads)
```
Therefore, ranges [threadStart[i], threadEnd[i]) and [threadStart[j], threadEnd[j]) are disjoint for i ≠ j.

### Theorem 2: Complete Coverage
```
∪(t=0 to T-1) [threadStart[t], threadEnd[t]) = [startChar, endChar)

Proof:
threadStart[0] = startChar
threadEnd[T-1] = startChar + Σ(t=0 to T-1) chunkSize[t]
               = startChar + T×baseChunk + remainder
               = startChar + T×⌊R/T⌋ + (R mod T)
               = startChar + R
               = endChar
```

### Theorem 3: Load Balance
The maximum difference in workload between any two threads is:
```
maxDiff = 95^(L-1)
```
This occurs when one thread gets 1 more starting character than another.

For L=4: maxDiff = 857,375 combinations (0.00126% of total workload per thread)

---

## Complexity Analysis

### Space Complexity
- **O(1)** per thread: Only stores start/end indices and current candidate

### Time Complexity
For password length L, thread with range [start, end):
```
T = (end - start) × 95^(L-1) × k

where k = time per MD5 computation (~constant)
```

### Parallel Speedup (Ideal)
```
Speedup = T_sequential / T_parallel
        = (95 × 95^(L-1)) / (⌈95/S⌉ × 95^(L-1) / T)
        = (S × T) / ⌈95/S⌉

For S=2, T=5:
Speedup ≈ (2 × 5) / 48 × 48
        = 10 (theoretically, 10× faster)
```

Actual speedup is affected by:
- CPU cache efficiency
- Thread synchronization overhead
- Network latency (RMI calls)
- Load imbalance

---

## Key Properties of This Approach

1. **Deterministic**: Same configuration always produces same partitioning
2. **Balanced**: Maximum load difference ≤ 1 character per entity
3. **Scalable**: Works for any number of servers/threads
4. **Efficient**: No communication between threads during search
5. **Fair**: Each computational unit does approximately equal work
6. **Verifiable**: Easy to prove correctness mathematically

---

## Comparison with Alternative Approaches

### Dynamic Work Stealing
❌ Requires inter-thread communication
❌ Complex synchronization
✓ Better load balancing for skewed workloads

### Hash-Based Partitioning  
❌ Can create severe load imbalance
❌ Requires hash computation for each candidate
✓ Good for key-value stores

### Range-Based Partitioning (Our Approach)
✓ No communication needed
✓ Predictable performance
✓ Simple implementation
✓ Provably correct
❌ Slight imbalance possible (max 1 char difference)

---

## Conclusion

This static range-based partitioning approach provides:
- **Mathematical guarantee** of non-overlapping search spaces
- **Near-perfect load balance** across all computational units
- **Zero coordination overhead** during search
- **Simple and verifiable** implementation

The hierarchical application (servers, then threads) allows the system to scale from single-machine to distributed environments while maintaining all these properties.