/** Wordle project (part I).
 * 
 * @Course INFO0010 - Introduction to computer networking
 * @Instructor Pr. Guy Leduc
 * @author Martin Dengis (s193348)
 * @AcademicYear 2023-2024
 * --------------------------------------------------------
 * The ConnectionStatusMonitor class acts as a tuple (x,y) for identifying 
 * each individual connection in the mass of concurrent threads.
 * The tuple (x,y) consists of :
 * @x The running connection as a thread
 * @y The connection identifier
 * 
 * The class includes the following methods:
 * @constructor ConnectionStatusMonitor : Specify connection details (thread and connection ID).
 * @method getThread : Return the thread.
 * @method getConnectionID : Return connection ID.
 */

public class ConnectionStatusMonitor {
    private final Thread THREAD;
    private final int CONNECTION_ID;

    public ConnectionStatusMonitor(Thread thread, int connectionID) {
        this.THREAD = thread;
        this.CONNECTION_ID = connectionID;
    }

    public Thread getThread() { return this.THREAD; }
    public int getConnectionID() { return this.CONNECTION_ID; }
}
