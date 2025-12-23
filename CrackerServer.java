import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * RMI Server implementation for distributed MD5 password cracking.
 * Each server handles a portion of the search space across multiple threads.
 */
public class CrackerServer extends UnicastRemoteObject implements CrackerInterface {
    
    private static final long serialVersionUID = 1L;
    private static final char[] ALLOWED;
    
    static {
        // Initialize character set with all printable ASCII characters
        ALLOWED = new char[95];
        for (int i = 0; i < 95; i++) {
            ALLOWED[i] = (char) (32 + i);
        }
    }
    
    // Thread-local MessageDigest for thread safety
    private static final ThreadLocal<MessageDigest> MD5_DIGEST = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    });
    
    private final String serverName;
    private final PrintWriter logWriter;
    private final AtomicBoolean stopRequested;
    
    /**
     * Constructor initializes the server with logging.
     */
    public CrackerServer(String serverName) throws RemoteException, IOException {
        super();
        this.serverName = serverName;
        this.stopRequested = new AtomicBoolean(false);
        
        // Initialize log file
        String logFileName = serverName.toLowerCase().replace(" ", "_") + ".log";
        this.logWriter = new PrintWriter(new FileWriter(logFileName, true), true);
        
        log("=== Server Initialized: " + serverName + " ===");
        log("Server start time: " + getTimestamp());
    }
    
    @Override
    public SearchResult searchPassword(String targetHash, int startCharIndex, 
                                      int endCharIndex, int numThreads, 
                                      int passwordLength) throws RemoteException {
        
        stopRequested.set(false);
        long startTime = System.currentTimeMillis();
        
        log("New search request received:");
        log("  Target Hash: " + targetHash);
        log("  Character Range: [" + startCharIndex + ", " + endCharIndex + ")");
        log("  Number of Threads: " + numThreads);
        log("  Password Length: " + passwordLength);
        log("  Assigned Characters: " + getCharacterRange(startCharIndex, endCharIndex));
        
        try {
            // Validate inputs
            if (!targetHash.matches("[0-9a-f]{32}")) {
                log("ERROR: Invalid MD5 hash format");
                throw new RemoteException("Invalid MD5 hash format");
            }
            
            if (passwordLength < 1 || passwordLength > 10) {
                log("ERROR: Invalid password length: " + passwordLength);
                throw new RemoteException("Password length must be between 1 and 10");
            }
            
            byte[] targetBytes = hexToBytes(targetHash);
            
            // Shared variables for coordination
            AtomicBoolean found = new AtomicBoolean(false);
            AtomicReference<String> foundPassword = new AtomicReference<>(null);
            AtomicReference<String> foundByThread = new AtomicReference<>(null);
            
            // Divide character range across threads
            int rangeSize = endCharIndex - startCharIndex;
            int baseChunk = rangeSize / numThreads;
            int remainder = rangeSize % numThreads;
            
            Thread[] threads = new Thread[numThreads];
            int cursor = startCharIndex;
            
            log("Creating " + numThreads + " worker threads...");
            
            // Create and start worker threads
            for (int t = 0; t < numThreads; t++) {
                int threadStartIndex = cursor;
                int chunk = baseChunk + (t < remainder ? 1 : 0);
                int threadEndIndex = threadStartIndex + chunk;
                cursor = threadEndIndex;
                
                final int threadNum = t + 1;
                final String threadId = serverName + "-Thread-" + threadNum;
                
                log("  " + threadId + " assigned range: [" + threadStartIndex + 
                    ", " + threadEndIndex + ") = " + getCharacterRange(threadStartIndex, threadEndIndex));
                
                Runnable worker = createWorker(
                    targetBytes, found, foundPassword, foundByThread,
                    threadStartIndex, threadEndIndex, passwordLength, threadId
                );
                
                threads[t] = new Thread(worker, threadId);
                threads[t].start();
                log("  " + threadId + " started at " + getTimestamp());
            }
            
            // Wait for all threads to complete
            for (Thread thread : threads) {
                try {
                    thread.join();
                    log("  " + thread.getName() + " stopped at " + getTimestamp());
                } catch (InterruptedException e) {
                    log("ERROR: Thread interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
            
            long endTime = System.currentTimeMillis();
            long searchTime = endTime - startTime;
            
            // Return result
            if (found.get()) {
                log("PASSWORD FOUND: '" + foundPassword.get() + "' by " + foundByThread.get());
                log("Search completed in " + searchTime + " ms");
                return new SearchResult(true, foundPassword.get(), foundByThread.get(), 
                                      serverName, searchTime);
            } else {
                log("Password not found in assigned range");
                log("Search completed in " + searchTime + " ms");
                return new SearchResult(false);
            }
            
        } catch (Exception e) {
            log("EXCEPTION: " + e.getMessage());
            e.printStackTrace(logWriter);
            throw new RemoteException("Search failed: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void stopSearch() throws RemoteException {
        log("Stop signal received from client");
        stopRequested.set(true);
    }
    
    @Override
    public String ping() throws RemoteException {
        return serverName;
    }
    
    /**
     * Creates a worker runnable that searches a specific character range.
     */
    private Runnable createWorker(byte[] targetBytes,
                                  AtomicBoolean found,
                                  AtomicReference<String> foundPassword,
                                  AtomicReference<String> foundByThread,
                                  int startIndex,
                                  int endIndex,
                                  int length,
                                  String threadId) {
        
        return () -> {
            byte[] candidateBytes = new byte[length];
            
            try {
                // For length 1, iterate through assigned characters
                if (length == 1) {
                    for (int i = startIndex; i < endIndex && !found.get() && !stopRequested.get(); i++) {
                        candidateBytes[0] = (byte) ALLOWED[i];
                        
                        if (md5Match(candidateBytes, 0, 1, targetBytes)) {
                            String candidate = String.valueOf(ALLOWED[i]);
                            if (found.compareAndSet(false, true)) {
                                foundPassword.set(candidate);
                                foundByThread.set(threadId);
                            }
                            return;
                        }
                    }
                    return;
                }
                
                // For length > 1, iterate through all combinations
                // First character comes from assigned range, rest from full character set
                for (int firstCharIdx = startIndex; firstCharIdx < endIndex && 
                     !found.get() && !stopRequested.get(); firstCharIdx++) {
                    
                    // Generate all combinations for remaining positions
                    int[] indices = new int[length - 1];
                    boolean finished = false;
                    
                    while (!finished && !found.get() && !stopRequested.get()) {
                        // Build candidate password
                        candidateBytes[0] = (byte) ALLOWED[firstCharIdx];
                        for (int i = 0; i < length - 1; i++) {
                            candidateBytes[i + 1] = (byte) ALLOWED[indices[i]];
                        }
                        
                        // Check if matches
                        if (md5Match(candidateBytes, 0, length, targetBytes)) {
                            char[] chars = new char[length];
                            chars[0] = ALLOWED[firstCharIdx];
                            for (int i = 0; i < length - 1; i++) {
                                chars[i + 1] = ALLOWED[indices[i]];
                            }
                            
                            String candidate = new String(chars);
                            
                            if (found.compareAndSet(false, true)) {
                                foundPassword.set(candidate);
                                foundByThread.set(threadId);
                            }
                            return;
                        }
                        
                        // Increment indices (odometer style)
                        for (int pos = length - 2; pos >= 0; pos--) {
                            indices[pos]++;
                            if (indices[pos] >= ALLOWED.length) {
                                indices[pos] = 0;
                                if (pos == 0) {
                                    finished = true;
                                }
                            } else {
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log("ERROR in " + threadId + ": " + e.getMessage());
                e.printStackTrace(logWriter);
            }
        };
    }
    
    /**
     * Computes MD5 hash and compares with target.
     */
    private static boolean md5Match(byte[] input, int offset, int length, byte[] target) {
        MessageDigest md = MD5_DIGEST.get();
        md.reset();
        md.update(input, offset, length);
        byte[] digest = md.digest();
        return Arrays.equals(digest, target);
    }
    
    /**
     * Converts hex string to byte array.
     */
    private static byte[] hexToBytes(String hex) {
        byte[] bytes = new byte[16];
        for (int i = 0; i < 16; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return bytes;
    }
    
    /**
     * Returns a string representation of the character range.
     */
    private String getCharacterRange(int start, int end) {
        if (end - start <= 10) {
            StringBuilder sb = new StringBuilder();
            for (int i = start; i < end; i++) {
                if (i > start) sb.append(", ");
                sb.append("'").append(ALLOWED[i]).append("'");
            }
            return sb.toString();
        } else {
            return "'" + ALLOWED[start] + "' to '" + ALLOWED[end - 1] + "' (" + (end - start) + " chars)";
        }
    }
    
    /**
     * Logs a message with timestamp.
     */
    private void log(String message) {
        String logMessage = "[" + getTimestamp() + "] " + message;
        logWriter.println(logMessage);
        System.out.println(logMessage);
    }
    
    /**
     * Returns current timestamp as formatted string.
     */
    private String getTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
    }
    
    /**
     * Main method to start the server.
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java CrackerServer <server-name> <port>");
            System.err.println("Example: java CrackerServer Server1 1099");
            System.exit(1);
        }
        
        String serverName = args[0];
        int port;
        
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Error: Port must be an integer");
            System.exit(1);
            return;
        }
        
        try {
            // Create RMI registry on specified port
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(port);
                System.out.println("RMI registry created on port " + port);
            } catch (RemoteException e) {
                // Registry might already exist
                registry = LocateRegistry.getRegistry(port);
                System.out.println("Using existing RMI registry on port " + port);
            }
            
            // Create and bind server
            CrackerServer server = new CrackerServer(serverName);
            registry.rebind(serverName, server);
            
            System.out.println("Server '" + serverName + "' is ready and bound to registry");
            System.out.println("Waiting for client requests...");
            
        } catch (Exception e) {
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}