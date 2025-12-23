import java.io.Serializable;

/**
 * Serializable result object returned by RMI servers.
 * Contains information about the password search outcome.
 */
public class SearchResult implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final boolean found;
    private final String password;
    private final String threadName;
    private final String serverName;
    private final long searchTimeMs;
    
    /**
     * Constructor for when password is found.
     */
    public SearchResult(boolean found, String password, String threadName, 
                       String serverName, long searchTimeMs) {
        this.found = found;
        this.password = password;
        this.threadName = threadName;
        this.serverName = serverName;
        this.searchTimeMs = searchTimeMs;
    }
    
    /**
     * Constructor for when password is not found.
     */
    public SearchResult(boolean found) {
        this.found = found;
        this.password = null;
        this.threadName = null;
        this.serverName = null;
        this.searchTimeMs = 0;
    }
    
    public boolean isFound() {
        return found;
    }
    
    public String getPassword() {
        return password;
    }
    
    public String getThreadName() {
        return threadName;
    }
    
    public String getServerName() {
        return serverName;
    }
    
    public long getSearchTimeMs() {
        return searchTimeMs;
    }
    
    @Override
    public String toString() {
        if (found) {
            return String.format("SearchResult{found=true, password='%s', thread='%s', server='%s', time=%dms}",
                    password, threadName, serverName, searchTimeMs);
        } else {
            return "SearchResult{found=false}";
        }
    }
}