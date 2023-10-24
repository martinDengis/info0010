public class ConnectionStatus {
    private final Thread THREAD;
    private final int CONNECTION_ID;

    public ConnectionStatus(Thread thread, int connectionID) {
        this.THREAD = thread;
        this.CONNECTION_ID = connectionID;
    }

    public Thread getThread() { return this.THREAD; }
    public int getConnectionID() { return this.CONNECTION_ID; }
}
