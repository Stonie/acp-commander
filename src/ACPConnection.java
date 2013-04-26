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
// @TODO

//import acpcommander.*;
import java.io.*;
import java.net.*;

public class ACPConnection {
    protected ACPSendPacket PcktSend = new ACPSendPacket ();
    protected ACPRecvPacket PcktReceive = new ACPRecvPacket ();

    protected InetAddress Target;
    protected Integer Port = new Integer (22936);
    protected byte[] connID = new byte[6];
    protected byte[] targetMAC = new byte[6];

    protected Integer Status = new Integer (0);  // Status
    protected Integer LastCmd = new Integer (0); // last ACP-Command sent to the LS
    protected boolean Authent = false;           // Did we get authenticated, yet?

    protected boolean ReceiveOnDiscover = false; // true: call onReceive for DiscoverReply packets
    protected boolean ReceiveOnCmdReply = false; // true: call onReceive for CmdReply packets

    //
    //  Constructors
    //

    public ACPConnection() {
        setConnID ("00:50:56:c0:00:08");     // some standard value
        setTargetMAC ("FF:FF:FF:FF:FF:FF");  // Accepted by the LS independent of actual MAC
    }

    public ACPConnection(String _Target) {
        this();
        setTarget (_Target);
    }

    public ACPConnection(byte[] _Target) {
        this();
        setTarget (_Target);
    }

    //
    // Some events for easier handling
    //
    // upon receiving a discover reply packet. Override for handling e.g. to fill a list
    public void onDiscover (String Reply, long ErrorCode) {
    }
    // upon receiving a CMD reply packet (should be cmd-line result). Override for handling
    public void onCMDReply (String Reply, long ErrorCode) {
    }
    // called upon receiving a packet (not a Discover packet). Override for handling
    public void onReceive (String Reply, long ErrorCode) {
    }


    //
    //  set/get for private variables
    //
    public void setConnID (String _ConnID) {
        // TODO: input param checking!
//        connID = _ConnID;
        System.err.print("setConnID (string) not implemented properly");
    }

    public void setConnID (byte _ConnID) {
        // TODO: input param checking!
//        connID = _ConnID;
        System.err.print("setConnID (byte) not implemented properly");
    }


    public String getTargetMAC () {
        return (targetMAC.toString());
    }

    public void setTargetMAC (String _targetMAC) {
        // TODO: input param checking!
//        targetMAC = _targetMAC;
        System.err.print("setTargetMAC (string) not implemented properly");
    }

    public void setTarget (String _Target) {
        try {
            Target = InetAddress.getByName(_Target);
        } catch (UnknownHostException ex) {
        }
    }

    public void setTarget (byte [] _Target) {
        try {
            Target = InetAddress.getByAddress(_Target);
        } catch (UnknownHostException ex) {
        }
    }

    public void setReceiveOnDiscover (boolean _flag) {
        ReceiveOnDiscover = _flag;
    }

    public void setReceiveOnCmdReply (boolean _flag) {
        ReceiveOnCmdReply = _flag;
    }

    //
    //  ACP packet sending
    //
    public void sendDiscover () {
        PcktSend.makeACPDiscover();
        sendPacket();
    }

    public void sendACPAuthBug () {
        PcktSend.makeACPAuthBug();
        sendPacket();
    }

    public void sendACPAuth (String _password) {
        PcktSend.makeACPAuth(_password);
        sendPacket();
    }

    public void sendENOneCmd (String _password) {
        PcktSend.makeACPEnOneCmd(_password);
        sendPacket();
    }

    public void sendCmd (String cmdln) {
        try {
            PcktSend.makeACPCmd(cmdln);
        } catch (Exception ex) {  // CmdLn too long
        }
        sendPacket();
    }

    public void sendChangeIP (byte [] NewIP) {

    }


    //
    //
    //

    public void receivePacket (DatagramPacket packet) {
        PcktReceive.packetbuf = packet.getData();
        int CmdReply = PcktReceive.getCommand();
        String Reply = PcktReceive.getString();
        int ErrorCode = PcktReceive.getErrorCode();

        switch ( CmdReply ) {
           case 0xC020: {                                    // ACP_DISCOVER_REPLY
               onDiscover (Reply, ErrorCode);
               if ( ReceiveOnDiscover ) { onReceive (Reply, ErrorCode); }
           }
           case 0xCA10: {
               onCMDReply (Reply, ErrorCode);
               if ( ReceiveOnCmdReply ) { onReceive (Reply, ErrorCode); }
           }
           default: { onReceive (Reply, ErrorCode); }
        }
    }

    private void sendPacket () {
        byte [] buffer = new byte[0];
        buffer = PcktSend.packetbuf;
        DatagramSocket _socket = null;
        try {
            _socket = new DatagramSocket();
        } catch (SocketException exSocket) {
        }

        DatagramPacket _packet = new DatagramPacket(buffer, 0, Target, Port.intValue());

        try {
            _socket.send(_packet);
        } catch (IOException exIO) {
        }
    }
}
