import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote interface for the distributed MD5 password cracker.
 * Defines the contract between client and server for RMI communication.
 */
public interface CrackerInterface extends Remote {
    
    /**
     * Initiates a password search on the server within a specified character range.
     * 
     * @param targetHash The MD5 hash to crack (32 hex characters)
     * @param startCharIndex Starting index in the character set (inclusive)
     * @param endCharIndex Ending index in the character set (exclusive)
     * @param numThreads Number of threads to use for this search
     * @param passwordLength Length of password to search for
     * @return SearchResult object containing the result or null if not found
     * @throws RemoteException if RMI communication fails
     */
    SearchResult searchPassword(String targetHash, int startCharIndex, 
                               int endCharIndex, int numThreads, 
                               int passwordLength) throws RemoteException;
    
    /**
     * Signals the server to stop all ongoing searches immediately.
     * Called when password is found by another server.
     * 
     * @throws RemoteException if RMI communication fails
     */
    void stopSearch() throws RemoteException;
    
    /**
     * Health check method to verify server is responsive.
     * 
     * @return Server name/identifier
     * @throws RemoteException if RMI communication fails
     */
    String ping() throws RemoteException;
}