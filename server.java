import java.rmi.RemoteException;
import java.rmi.Remote;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.Naming;
import java.util.PriorityQueue;

// Interface for implementation of vector clock
interface VectorClock extends java.io.Serializable {
    void increment();
    void update(VectorClock otherClock);
    int[] getClock();
}

// Vector clock implementation
class VectorClockImpl implements VectorClock {
    private int[] clock;
    private int processId;

    public VectorClockImpl(int numProcesses, int processId) {
        this.clock = new int[numProcesses];
        this.processId = processId;
    }

    public void increment() {
        clock[processId]++;
    }

    public void update(VectorClock otherClock) {
        int[] other = otherClock.getClock();
        for (int i = 0; i < clock.length; i++) {
            clock[i] = Math.max(clock[i], other[i]);
        }
    }

    public int[] getClock() {
        return clock;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < clock.length; i++) {
            sb.append(clock[i]);
            if (i < clock.length - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
}

// Interface for mutex manager functions
interface MutexManager extends Remote {
    void requestEntry(int processId, VectorClock clock) throws RemoteException;
    void releaseEntry(int processId) throws RemoteException;
}

// Mutex manager implementation
class MutexManagerImpl extends UnicastRemoteObject implements MutexManager {
    private PriorityQueue<Request> requestQueue;
    private int currentProcess;

    protected MutexManagerImpl() throws RemoteException {
        super();
        requestQueue = new PriorityQueue<>();
        currentProcess = -1;
    }

    public synchronized void requestEntry(int processId, VectorClock clock) throws RemoteException {
        requestQueue.add(new Request(processId, clock));
        checkQueue();
    }

    public synchronized void releaseEntry(int processId) throws RemoteException {
        requestQueue.removeIf(request -> request.processId == processId);
        checkQueue();
    }

    private void checkQueue() throws RemoteException {
        if (!requestQueue.isEmpty() && currentProcess == -1) {
            Request nextRequest = requestQueue.peek();
            currentProcess = nextRequest.processId;
            // Notify the process (not implemented here)
        }
    }

    private class Request implements Comparable<Request> {
        int processId;
        VectorClock clock;

        Request(int processId, VectorClock clock) {
            this.processId = processId;
            this.clock = clock;
        }

        public int compareTo(Request other) {
            int[] thisClock = this.clock.getClock();
            int[] otherClock = other.clock.getClock();
            for (int i = 0; i < thisClock.length; i++) {
                if (thisClock[i] < otherClock[i]) return -1;
                if (thisClock[i] > otherClock[i]) return 1;
            }
            return Integer.compare(this.processId, other.processId);
        }
    }
}

// Process interface
interface Process extends Remote {
    void requestCriticalSection() throws RemoteException;
    void releaseCriticalSection() throws RemoteException;
    VectorClock getClock() throws RemoteException;
}

// Process implementation
class ProcessImpl extends UnicastRemoteObject implements Process {
    private VectorClock clock;
    private boolean inCriticalSection;
    private MutexManager mutexManager;

    protected ProcessImpl(MutexManager manager) throws RemoteException {
        super();
        this.mutexManager = manager;
        this.clock = new VectorClockImpl(1, 0);
        this.inCriticalSection = false;
    }

    public void requestCriticalSection() throws RemoteException {
        clock.increment();
        System.out.println("Requesting critical section at time " + clock.toString());
        mutexManager.requestEntry(0, clock);
    }

    public void releaseCriticalSection() throws RemoteException {
        inCriticalSection = false;
        System.out.println("Releasing critical section at time " + clock.toString());
        mutexManager.releaseEntry(0);
    }

    public VectorClock getClock() throws RemoteException {
        return clock;
    }
}

// Server Setup
public class server {
    public static void main(String[] args) {
        try {
            // Start the rmiregistry programmatically on port 2000
            int port = 2000;

            // Check if the registry is already running
            try {
                Registry registry = LocateRegistry.getRegistry(port);
                registry.list(); // This will throw an exception if the registry is not already running
            } catch (RemoteException e) {
                // If the registry is not running, start it
                LocateRegistry.createRegistry(port);
                System.out.println("RMI registry started on port " + port);
            }

            // Create the mutex manager instance
            MutexManager manager = new MutexManagerImpl();
            
            // Create the process instance and pass the mutex manager to it
            Process process = new ProcessImpl(manager);

            // Bind the remote objects to the registry
            Naming.rebind("rmi://localhost:" + port + "/MutexManager", manager);
            Naming.rebind("rmi://localhost:" + port + "/Process", process);

            System.out.println("Server is running...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

