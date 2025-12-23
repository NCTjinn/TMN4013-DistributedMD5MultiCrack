import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Client for the distributed MD5 password cracker.
 * Coordinates work distribution across multiple RMI servers.
 */
public class CrackerClient {
    
    private static final int TOTAL_CHARACTERS = 95; // Printable ASCII characters
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        try {
            System.out.println("=== Distributed MD5 Password Cracker ===");
            System.out.println();
            
            // Get user inputs
            System.out.print("Enter target MD5 hash (32 hex characters): ");
            String targetHash = scanner.nextLine().toLowerCase().trim();
            
            if (!targetHash.matches("[0-9a-f]{32}")) {
                System.err.println("Error: Invalid MD5 hash format");
                return;
            }
            
            System.out.print("Enter password length to search (1-10): ");
            int passwordLength = scanner.nextInt();
            
            if (passwordLength < 1 || passwordLength > 10) {
                System.err.println("Error: Password length must be between 1 and 10");
                return;
            }
            
            System.out.print("Enter number of threads per server (1-10): ");
            int threadsPerServer = scanner.nextInt();
            
            if (threadsPerServer < 1 || threadsPerServer > 10) {
                System.err.println("Error: Number of threads must be between 1 and 10");
                return;
            }
            
            System.out.print("Enter number of servers to use (1 or 2): ");
            int numServers = scanner.nextInt();
            
            if (numServers < 1 || numServers > 2) {
                System.err.println("Error: Number of servers must be 1 or 2");
                return;
            }
            
            scanner.nextLine(); // Consume newline
            
            // Get server connection details
            String[] serverNames = new String[numServers];
            String[] serverHosts = new String[numServers];
            int[] serverPorts = new int[numServers];
            
            for (int i = 0; i < numServers; i++) {
                System.out.println("\nServer " + (i + 1) + " details:");
                System.out.print("  Server name (e.g., Server1): ");
                serverNames[i] = scanner.nextLine().trim();
                System.out.print("  Host (e.g., localhost): ");
                serverHosts[i] = scanner.nextLine().trim();
                System.out.print("  Port (e.g., 1099): ");
                serverPorts[i] = scanner.nextInt();
                scanner.nextLine(); // Consume newline
            }
            
            System.out.println("\n=== Starting Distributed Search ===");
            System.out.println("Target Hash: " + targetHash);
            System.out.println("Password Length: " + passwordLength);
            System.out.println("Threads per Server: " + threadsPerServer);
            System.out.println("Number of Servers: " + numServers);
            System.out.println("Start Time: " + getTimestamp());
            System.out.println();
            
            // Connect to servers
            CrackerInterface[] servers = new CrackerInterface[numServers];
            
            for (int i = 0; i < numServers; i++) {
                try {
                    Registry registry = LocateRegistry.getRegistry(serverHosts[i], serverPorts[i]);
                    servers[i] = (CrackerInterface) registry.lookup(serverNames[i]);
                    String response = servers[i].ping();
                    System.out.println("✓ Connected to: " + response + " at " + 
                                     serverHosts[i] + ":" + serverPorts[i]);
                } catch (RemoteException | NotBoundException e) {
                    System.err.println("✗ Failed to connect to server " + (i + 1) + ": " + e.getMessage());
                    return;
                }
            }
            
            System.out.println();
            
            // Partition search space across servers
            SearchConfig[] configs = partitionSearchSpace(numServers);
            
            System.out.println("Search Space Partitioning:");
            for (int i = 0; i < numServers; i++) {
                System.out.println("  Server " + (i + 1) + " (" + serverNames[i] + "): " +
                                 "Characters [" + configs[i].startIndex + ", " + 
                                 configs[i].endIndex + ") - " + 
                                 (configs[i].endIndex - configs[i].startIndex) + " characters");
            }
            System.out.println();
            
            long globalStartTime = System.currentTimeMillis();
            
            // Execute searches concurrently using ExecutorService
            ExecutorService executor = Executors.newFixedThreadPool(numServers);
            AtomicReference<SearchResult> finalResult = new AtomicReference<>(null);
            CountDownLatch latch = new CountDownLatch(numServers);
            
            // Submit search tasks for each server
            for (int i = 0; i < numServers; i++) {
                final int serverIndex = i;
                final CrackerInterface server = servers[i];
                final SearchConfig config = configs[i];
                
                executor.submit(() -> {
                    try {
                        System.out.println("→ Starting search on " + serverNames[serverIndex] + "...");
                        
                        SearchResult result = server.searchPassword(
                            targetHash,
                            config.startIndex,
                            config.endIndex,
                            threadsPerServer,
                            passwordLength
                        );
                        
                        if (result.isFound()) {
                            System.out.println("★ PASSWORD FOUND by " + serverNames[serverIndex] + "!");
                            
                            // Set result if this is the first to find it
                            finalResult.compareAndSet(null, result);
                            
                            // Signal all other servers to stop
                            for (int j = 0; j < numServers; j++) {
                                if (j != serverIndex) {
                                    try {
                                        servers[j].stopSearch();
                                    } catch (RemoteException e) {
                                        // Ignore errors during stop signal
                                    }
                                }
                            }
                        } else {
                            System.out.println("→ " + serverNames[serverIndex] + " completed (not found)");
                        }
                        
                    } catch (RemoteException e) {
                        System.err.println("✗ Error from " + serverNames[serverIndex] + ": " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            // Wait for all searches to complete
            latch.await();
            executor.shutdown();
            
            long globalEndTime = System.currentTimeMillis();
            double totalSeconds = (globalEndTime - globalStartTime) / 1000.0;
            
            // Display results
            System.out.println();
            System.out.println("=".repeat(60));
            System.out.println("SEARCH COMPLETED");
            System.out.println("=".repeat(60));
            
            SearchResult result = finalResult.get();
            if (result != null && result.isFound()) {
                System.out.println("Status: PASSWORD FOUND");
                System.out.println("Password: '" + result.getPassword() + "'");
                System.out.println("Found by Thread: " + result.getThreadName());
                System.out.println("Found on Server: " + result.getServerName());
                System.out.println("Server Search Time: " + result.getSearchTimeMs() + " ms");
            } else {
                System.out.println("Status: PASSWORD NOT FOUND");
                System.out.println("The password was not found in the search space.");
            }
            
            System.out.println("Total Elapsed Time: " + String.format("%.3f", totalSeconds) + " seconds");
            System.out.println("End Time: " + getTimestamp());
            System.out.println("=".repeat(60));
            
        } catch (InterruptedException e) {
            System.err.println("Search interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
    
    /**
     * Partitions the search space across servers based on character indices.
     * Uses static partitioning to ensure non-overlapping ranges.
     * 
     * Mathematical Division:
     * - Total character space: 95 printable ASCII characters (indices 0-94)
     * - Each server gets: baseChunk = 95 / numServers characters
     * - Remainder distributed to first servers: remainder = 95 % numServers
     * 
     * Example with 2 servers:
     *   Server 1: indices [0, 48)  = 48 characters
     *   Server 2: indices [48, 95) = 47 characters
     * 
     * This ensures:
     * 1. No overlap between servers (ranges are contiguous and exclusive)
     * 2. Complete coverage (all 95 characters are assigned)
     * 3. Fair distribution (difference of at most 1 character between servers)
     */
    private static SearchConfig[] partitionSearchSpace(int numServers) {
        SearchConfig[] configs = new SearchConfig[numServers];
        
        int baseChunk = TOTAL_CHARACTERS / numServers;
        int remainder = TOTAL_CHARACTERS % numServers;
        
        int cursor = 0;
        for (int i = 0; i < numServers; i++) {
            int startIndex = cursor;
            int chunk = baseChunk + (i < remainder ? 1 : 0);
            int endIndex = startIndex + chunk;
            
            configs[i] = new SearchConfig(startIndex, endIndex);
            cursor = endIndex;
        }
        
        return configs;
    }
    
    /**
     * Helper class to hold search configuration for each server.
     */
    private static class SearchConfig {
        final int startIndex;
        final int endIndex;
        
        SearchConfig(int startIndex, int endIndex) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
    }
    
    /**
     * Returns current timestamp as formatted string.
     */
    private static String getTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }
}