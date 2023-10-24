import java.net.Socket;

public class ConnectionStatusMonitor {
    private final Thread THREAD;
    private final int CONNECTION_ID;
    private final ClientConnection CLIENT;

    public ConnectionStatusMonitor(Thread thread, ClientConnection client, int connectionID) {
        this.THREAD = thread;
        this.CLIENT = client;
        this.CONNECTION_ID = connectionID;
    }

    public Thread getThread() { return this.THREAD; }
    public int getConnectionID() { return this.CONNECTION_ID; }

    public ClientConnection getClientConnection() { return this.CLIENT; }
    public Socket getClientSocket() { return this.getClientConnection().getSocket(); }
}
