import java.rmi.Naming;

public class client {
    public static void main(String[] args) {
        //port matched with port used by server
        try {
            int port = 2000;
            //using naming lookup to locate the remote object to connect to
            Process process = (Process) Naming.lookup("rmi://localhost:" + port + "/Process");

            //requesting to enter CS and printing request message
            process.requestCriticalSection();
            System.out.println("Critical section requested.");

            //simulating a workload as if there was an actual process to execute
            Thread.sleep(2000);

            //releasing client from critical section and priting release messsage
            process.releaseCriticalSection();
            System.out.println("Critical section released.");

          //printing exceptions
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
