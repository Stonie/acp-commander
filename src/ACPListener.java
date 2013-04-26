package acpcommander;

/**
 * <p>Überschrift: acp_commander</p>
 *
 * <p>Beschreibung: Used to send ACP commands to Buffalo Linkstation (R). Out of the work
 * of linkstationwiki.net</p>
 *
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Organisation: linkstationwiki.net</p>
 *
 * @author Georg
 * @version 0.1
 */

import java.lang.Runnable;
import java.net.*;
import java.io.*;

public class ACPListener implements Runnable {
    private ACPConnection ACPConn;
    private DatagramSocket socket;
    private int MaxPacketLen = 500;  // maximum packet length we expect/accept


    public ACPListener(ACPConnection _Connection, int Port, int TimeOut) {
        try {
            socket = new DatagramSocket(Port);
            socket.setSoTimeout(TimeOut);
        } catch (SocketException ex) {
        }

        ACPConn = _Connection;
    }

    public void run() {
        DatagramPacket receive = new DatagramPacket(new byte[MaxPacketLen], MaxPacketLen);
        try {
            socket.receive(receive);
        } catch (IOException ex) {
        }
        // pass the received packet to our ACPConnection for handling
        ACPConn.receivePacket(receive);
    }
}
