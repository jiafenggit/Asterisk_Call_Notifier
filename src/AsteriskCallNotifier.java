import java.io.IOException;
import java.lang.String;
import java.net.ConnectException;
import java.net.InetAddress;

import org.asteriskjava.manager.AuthenticationFailedException;
import org.asteriskjava.manager.ManagerConnection;
import org.asteriskjava.manager.ManagerConnectionFactory;
import org.asteriskjava.manager.ManagerEventListener;
import org.asteriskjava.manager.TimeoutException;
import org.asteriskjava.manager.action.StatusAction;
import org.asteriskjava.manager.event.AgentCalledEvent;
import org.asteriskjava.manager.event.ManagerEvent;



public class AsteriskCallNotifier implements ManagerEventListener
{
    private ManagerConnection managerConnection;

    //We need the actual IP of the machine that Asterisk runs on
    //,in case we run this program from another machine.
    String ServerAddress = "192.168.1.253";


    public AsteriskCallNotifier() throws IOException
    {
        ManagerConnectionFactory factory = new ManagerConnectionFactory(
                ServerAddress, "manager", "pa55w0rd");

        this.managerConnection = factory.createManagerConnection();
    }


    public void run() throws IOException, AuthenticationFailedException,
            TimeoutException, InterruptedException
    {

        // register for events
        managerConnection.addEventListener(this);

        try
        {

        // connect to Asterisk and log in
        managerConnection.login();
        }
        catch (ConnectException e)
        {
           System.out.println("Unable to login to Asterisk. Program terminated.");
        }

        System.out.println("Connection to Asterisk server was successful.");

        // request channel state
        managerConnection.sendAction(new StatusAction());

        System.out.println("Asterisk Call Notifier is now listening for incoming calls.");
        System.out.println("Press Ctrl+C to terminate.");


        InetAddress inetAddress = InetAddress.getByName(ServerAddress);

        Boolean ServerIsReachable = true;

        //We use Thread.sleep to continuously process events
        //,stopping every 5 seconds to check that the Asterisk server is reachable.
        //If not, we log off and disconnect.
        while (ServerIsReachable )
        {
            Thread.sleep(5000);
            ServerIsReachable = inetAddress.isReachable(1000);
        }

        managerConnection.logoff();

    }



    public void onManagerEvent(ManagerEvent event)
    {

        //For every Asterisk event we trap, we check to see
        //if it is an 'AgentCalledEvent', which fires when an agent is called.
        if (event instanceof AgentCalledEvent)
        {

            //We get the caller's number...
            String callerId = ((AgentCalledEvent) event).getCallerIdNum();

            ///...and the agent's internal phone number. (extension)
            String agentExtensionStr = ((AgentCalledEvent) event).getExtension();


            //We check to see if the agent's extension is within the valid range.
            int agentExtension = Integer.parseInt(agentExtensionStr);

            if (agentExtension < 101  || agentExtension > 140)
            {
                System.out.println("Agent's extension is not within the valid range. Event will be ignored.");
                return;
            }

            //We construct the agent's IP address by using her internal phone number
            //since it is the same as the last 3 digits of her Ip address.
            String agentIp = "192.168.1." + agentExtension;


            //Next we will use the smbclient command line utility
            // to send a WinPopup message to the IP address we just retrieved
            //,assuming the client computers run on MS Windows.


            //We will echo the message to standard output
            // in order to pass it to smbclient using the '|' operator.
            String echoCmd = "/bin/echo -e ";

            String popupMsg = "Incoming Call From: " + callerId;

            //We use the switch '-M' of the smbclient utility
            //to send out message to the IP address that follows.
            String winPopCmd = "|/usr/bin/smbclient -M " + agentIp;


            //We construct the process command and finally we run it
            //,catching any exception which may arise.
            ProcessBuilder WinPopupProcess
                    = new ProcessBuilder(echoCmd,popupMsg,winPopCmd);


            try
            {
            WinPopupProcess.start();
            }
            catch (Exception e)
            {
              System.out.println("Unable to launch popup due to an error.");
            }



        }

    }


    public static void main(String[] args) throws Exception
    {
        AsteriskCallNotifier callNotifier = new AsteriskCallNotifier();
        callNotifier.run();
    }



}