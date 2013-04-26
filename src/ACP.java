package acpcommander;

/**
 * <p>Überschrift: ACP</p>
 *
 * <p>Beschreibung: Core class for sending ACP commands to Buffalo Linkstation (R). Out
 * of the work of linkstationwiki.net</p>
 *
 * <p>Copyright: Copyright (c) 2006, GPL</p>
 *
 * <p>Organisation: linkstationwiki.net</p>
 *
 * @author Georg
 * @version 0.4.1 (beta)
 */

import java.net.*;
import java.util.*;

public class ACP {
    private InetAddress target;
    protected Integer Port = new Integer(22936);
    private String connID; // connection ID, "unique" identifier for the connection
    private String targetMAC; // MAC address of the LS, it reacts only if correct MAC or
    // FF:FF:FF:FF:FF:FF is set in the packet
    protected byte[] Key = new byte[4]; // Key for password encryption
    // sent in reply to ACP discovery packet
    protected String password;
    private String ap_servd = "ap_servd";
    private InetSocketAddress bind;

    protected int LastCmd = 0; // last ACP-Command sent to the LS
    protected int LastError = 0; // Error Code in the last ACP packet rcvd
    protected boolean HaveKey = false; // Did we get the encryption key, yet? - Send ACPDisc
    protected boolean EnOneCmd = false; // Did we do EnOneCmd authentication, yet? - ACPEnOneCmd
    protected boolean Authent = false; // Did we get authenticated, yet? - ACPAuthent

    /** set socket timeout to 1000 ms, rather high, but some users report timeout
     * problems. Could also be UDP-related - try resending packets
     *
     * Especially BlinkLED, SaveConfig, LoadConfig have long reply times as reply is
     * sent when the command has been executed. Same has to be considered for other cmds.
     */
    protected int Timeout = 5000;
    protected int resendPackets = 2; // standard value for repeated sending of packets

    public int DebugLevel = 0; // Debug level

    protected int rcvBufLen = 4096; // standard length of receive buffer


    public ACP() {
    }

    public ACP(String Target) {
        this();
        setTarget(Target);
    }

    public ACP(byte[] Target) {
        this();
        setTarget(Target);
    }


    //
    //  set/get for private variables
    //
    public String getConnID() {
        return connID.toString();
    }

    public void setConnID(String ConnectionID) {
        // TODO: input param checking!
        connID = ConnectionID;
    }

    public void setConnID(byte[] ConnectionID) {
        // TODO: input param checking!
        connID = bufferToHex(ConnectionID, 0, 6);
    }

    public String getTargetMAC() {
        return (targetMAC.toString());
    }

    public void setTargetMAC(String TargetMAC) {
        // TODO: input param checking!
        targetMAC = TargetMAC;
    }

    public byte[] getTargetKey() {
        return (Key);
    }

    public void setTargetKey(byte[] _Key) {
        // TODO: input param checking!
        if (_Key.length != 4) {
            outError("ACPException: Encryption key must be 4 bytes long!");
            return;
        }
        Key = _Key;
        HaveKey = true;
    }

    public void setTargetKey(String _Key) {
        // TODO: input param checking!
        setTargetKey(HexToByte(_Key));
    }

    public void setPassword(String _password) {
        password = _password;
    }

    public InetAddress getTarget() {
        return target;
    }

    public void setTarget(String Target) {
        try {
            target = InetAddress.getByName(Target);
            Authent = false;
            HaveKey = false;
        } catch (UnknownHostException ex) {
            outInfoSetTarget();
            outError(ex.toString() + " [in setTarget]");
        }
    }

    public void setTarget(byte[] Target) {
        try {
            target = InetAddress.getByAddress(Target);
            Authent = false;
            HaveKey = false;
        } catch (UnknownHostException ex) {
            outInfoSetTarget();
            outError(ex.toString() + " [in setTarget]");
        }
    }

    public void setBroadcastIP(String Target) {
        try {
            target = InetAddress.getByName(Target);
            setTargetMAC("FF:FF:FF:FF:FF:FF");
            Authent = false;
            HaveKey = false;
        } catch (UnknownHostException ex) {
            outError(ex.toString() + " [in setBroadcastIP]");
        }
    }

    public void setBroadcastIP(byte[] Target) {
        try {
            target = InetAddress.getByAddress(Target);
            setTargetMAC("FF:FF:FF:FF:FF:FF");
            Authent = false;
            HaveKey = false;
        } catch (UnknownHostException ex) {
            outError(ex.toString() + " [in setBroadcastIP]");
        }
    }

    public void bind(InetSocketAddress localIP) {
        bind = localIP;
        if (localIP.isUnresolved()) {
            outWarning("The bind address " + localIP +
                       " given with parameter -b could not be resolved to a local IP-Address.\n" +
                       "You must use this parameter with a valid IP-Address that belongs to the PC you run acp_commander on.\n");
            bind = null;
        }
    }

    public void bind(String localIP) {
        // bind socket to a local address (-b)
        // Create a socket address from a hostname (_bind) and a port number. A port number
        // of zero will let the system pick up an ephemeral port in a bind operation.
        if (!localIP.equalsIgnoreCase("")) {
            bind(new InetSocketAddress(localIP, 0));
        } else {
            bind = null;
        }
    }

    int getDebugLevel() {
        return DebugLevel;
    }

    //
    // ACP functionallity
    //

    public String[] Find() {
        // discover linkstations by sending an ACP-Discover package
        // return on line of formatted string per found LS
        return doDiscover(getACPDisc(connID, targetMAC));
    }

    public String[] Discover() {
        // send ACP discover packet to Linkstation
        // (if a broadcast address is used, only the first answer is returned)
        return doSendRcv(getACPDisc(connID, targetMAC), 1);
    }

    public String[] Discover(boolean setTargetData) {
        String[] result = Discover();

        if (setTargetData) {
            setTargetMAC(result[4]); // set MAC address according to discovery data
            setTargetKey(result[8]); // set encryption key according to discovery data
        }
        return result;
    }

    public String[] Command(String cmd, int maxResend) {
        // send telnet-type command cmd to Linkstation by ACPcmd
        if (maxResend <= 0) {
            maxResend = resendPackets;
        }
        return doSendRcv(getACPCmd(connID, targetMAC, cmd), maxResend);
    }

    public String[] Command(String cmd) {
        // send telnet-type command cmd to Linkstation by ACPcmd - only send packet once!
        return doSendRcv(getACPCmd(connID, targetMAC, cmd), 1);
    }

    public String[] Authent() {
        byte[] _encrypted = encryptACPpassword(password, Key);
        return Authent(_encrypted);
    }

    public String[] Authent(byte[] enc_password) {
        // authenticate to ACP protokoll
        return doSendRcv(getACPAuth(connID, targetMAC, enc_password));
    }

    public String[] AuthentBug() {
        // authenticate to ACP protokoll using (supposed) buffer overflow
        return doSendRcv(getACPAuthBug(connID, targetMAC));
    }

    public String[] Shutdown() {
        // ENOneCmd protected
        return doSendRcv(getACPShutdown(connID, targetMAC));
    }

    public String[] Reboot() {
        // ENOneCmd protected
        return doSendRcv(getACPReboot(connID, targetMAC));
    }

    public String[] EMMode() {
        // ENOneCmd protected
        return doSendRcv(getACPEMMode(connID, targetMAC));
    }

    public String[] NormMode() {
        // ENOneCmd protected
        return doSendRcv(getACPNormMode(connID, targetMAC));
    }

    public String[] BlinkLED() {
        return doSendRcv(getACPBlinkLED(connID, targetMAC));
    }

    public String[] EnOneCmd() {
        return EnOneCmdENC(encryptACPpassword(ap_servd, Key));
    }

    public String[] EnOneCmdENC(byte[] encPassword) {
        return doSendRcv(getACPEnOneCmd(connID, targetMAC, encPassword));
    }

    public String[] SaveConfig() {
        // set timeout to 1 min
        int _mytimeout = Timeout;
        Timeout = 60000;
        String[] result = doSendRcv(getACPSaveConfig(connID, targetMAC));
        Timeout = _mytimeout;
        return result;
    }

    public String[] LoadConfig() {
        // set timeout to 1 min
        int _mytimeout = Timeout;
        Timeout = 60000;
        String[] result = doSendRcv(getACPLoadConfig(connID, targetMAC));
        Timeout = _mytimeout;
        return result;
    }

    public String[] DebugMode() {
        return doSendRcv(getACPDebugMode(connID, targetMAC));
    }


    public String[] MultiLang(byte Language) {
        // interface to switch web GUI language
        // ENOneCmd protected
        // 0 .. Japanese
        // 1 .. English
        // 2 .. German
        // default .. English
        return doSendRcv(getACPMultiLang(connID, targetMAC, Language));
    }

    public String[] ChangeIP(byte[] newIP, byte[] newMask, boolean useDHCP) {
        // change IP address

        byte[] _encrypted = encryptACPpassword(password, Key);

        return doSendRcv(getACPChangeIP(connID, targetMAC, newIP, newMask,
                                        useDHCP, _encrypted));
    }

    //--- End of public routines ---

    //
    // ACP-Interface functions (private)
    //

    private String[] doDiscover(byte[] buf) {
        String _state = "[Send/Receive ACPDiscover]";
        String[] _searchres;
        ArrayList _tempres = new ArrayList(5); // initially assume we'll find 5 LS
        DatagramSocket _socket;
        DatagramPacket _packet = new DatagramPacket(buf, buf.length, target,
                Port.intValue());
        // TODO: danger - possible buffer overflow
        DatagramPacket _receive = new DatagramPacket(new byte[rcvBufLen],
                rcvBufLen);

        try {
            _socket = getSocket(); // TODO bind functionality is missing here

            _socket.send(_packet);
            long _LastSendTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - _LastSendTime < Timeout) {
                _socket.receive(_receive);
                _searchres = rcvACP(_receive.getData(), DebugLevel); // get search results

                // TODO: do optional Discover event with _searchres
                _tempres.add(_searchres[1]); // add formatted string to result list
            }
        } catch (java.net.SocketTimeoutException SToE) {
            // TimeOut should be OK as we wait until Timeout if we get packets
            outDebug(
                    "Timeout reached, stop listening to further Discovery replies",
                    2);
        } catch (java.net.SocketException SE) {
            // TODO: better error handling
            outInfoSocket();
            outError("Exception: SocketException (" + SE.getMessage() + ") " +
                     _state);
        } catch (java.io.IOException IOE) {
            // TODO: better error handling
            outError("Exception: IOException (" + IOE.getMessage() + ") " +
                     _state);
        }

        // first check for repeated entries and delete them.
        for (int i = 0; i < _tempres.size() - 1; i++) {
            for (int j = i + 1; j < _tempres.size(); j++) {
                // if entry i is equal to entry j
                if (((String) _tempres.get(i)).equals((String) _tempres.get(j))) {
                    // remove j, alternatively clear string and delete in second loop
                    _tempres.remove(j);
                    j--;
                }
            }
        }

        // move results into string array
        String[] result = new String[_tempres.size()];
        for (int i = 0; i < _tempres.size(); i++) {
            result[i] = (String) _tempres.get(i);
        }
        return result;
    }

    // send ACP packet and handle answer
    private String[] doSendRcv(byte[] buf) {
        return doSendRcv(buf, resendPackets);
    }


    private String[] doSendRcv(byte[] buf, int repeatSend) {
        String _ACPcmd = bufferToHex(buf, 9, 1) + bufferToHex(buf, 8, 1);
        String _state = "[ACP Send/Receive (Packet:" + _ACPcmd + " = " +
                        getCmdString(buf) + ")]";
        String[] result;
        int sendcount = 0;
        boolean SendAgain = true;
        DatagramSocket _socket;
        DatagramPacket _packet = new DatagramPacket(buf, buf.length,
                target,
                Port.intValue());
        // TODO: danger - possible buffer overflow/data loss with fixed packet length
        DatagramPacket _receive = new DatagramPacket(new byte[rcvBufLen],
                rcvBufLen);

        do {
            sendcount++;
            try {
                outDebug("Sending " + sendcount + "/" + repeatSend, 2);

                _socket = getSocket();

                _socket.send(_packet);
                _socket.receive(_receive);

                SendAgain = false; // we received an answer

                // TODO: do optional Receive-event with result
            } catch (java.net.SocketTimeoutException SToE) {
                // TODO: better error handling
                result = new String[2];
                if (sendcount >= repeatSend) {
                    result[1] = "Exception: SocketTimeoutException (" +
                                SToE.getMessage() + ") " + _state;
                    outInfoTimeout();
                    outError(result[1]);
                } else {
                    result[1] = "Timeout (" + _state + " retry sending (" +
                                sendcount + "/" + repeatSend + ")";
                    outDebug(result[1], 1);
                }
            } catch (java.net.SocketException SE) {
                // TODO: better error handling
                result = new String[2];
                result[1] = "Exception: SocketException (" + SE.getMessage() +
                            ") " +
                            _state;
                outInfoSocket();
                outError(result[1]);
            } catch (java.io.IOException IOE) {
                // TODO: better error handling
                result = new String[2];
                result[1] = "Exception: IOException (" + IOE.getMessage() +
                            ") " + _state;
                outError(result[1]);
            }

        } while ((sendcount < repeatSend) & SendAgain); // repeat until max retries reached

        result = rcvACP(_receive.getData(), DebugLevel); // get search results

        return result;
    }

    // only send packet, don't wait for reply
    private void doSend(byte[] buf) {
        String _ACPcmd = bufferToHex(buf, 9, 1) + bufferToHex(buf, 8, 1);
        String _state = "[ACP Send/Receive (Packet:" + _ACPcmd + " = " +
                        getCmdString(buf) + ")]";
        String[] result;
        DatagramSocket _socket;

        try {
            _socket = new DatagramSocket(); // TODO bind functionality is missing here
            DatagramPacket _packet = new DatagramPacket(buf, buf.length,
                    target,
                    Port.intValue());

            _socket.send(_packet);

        } catch (java.net.SocketTimeoutException SToE) {
            // TODO: better error handling
            result = new String[2];
            result[1] = "Exception: SocketTimeoutException (" + SToE.getMessage() +
                        ") "
                        + _state;
            outInfoTimeout();
            outError(result[1]);
        } catch (java.net.SocketException SE) {
            // TODO: better error handling
            result = new String[2];
            result[1] = "Exception: SocketException (" + SE.getMessage() + ") " +
                        _state;
            outInfoSocket();
            outError(result[1]);
        } catch (java.io.IOException IOE) {
            // TODO: better error handling
            result = new String[2];
            result[1] = "Exception: IOException (" + IOE.getMessage() + ") " +
                        _state;
            outError(result[1]);
        }
    }

    private DatagramSocket getSocket() throws java.net.SocketException {
        DatagramSocket _socket;
        if (bind != null) {
            // bind socket to a local address (-b)
            // Create a socket address from a hostname (_bind) and a port number. A port number
            // of zero will let the system pick up an ephemeral port in a bind operation.
            outDebug("Binding socket to: " + bind.toString() + "\n", 1);

            _socket = new DatagramSocket(bind);
        } else {
            _socket = new DatagramSocket();
        }

        _socket.setSoTimeout(Timeout);
        return _socket;
    }

    //
    // ACP packet creation functionality
    //

    private int getCommand(byte[] buf) {
        return (int) ((buf[9] & 0xFF) << 8) + (int) (buf[8] & 0xFF);
    }

    private byte getSpecialCmd(byte[] buf) {
        return buf[32];
    }

    private String getCmdString(byte[] buf) {
        int ACPCmd = getCommand(buf);
        String CmdString = new String("");

        switch (ACPCmd) {
        // ACP_Commands
        // Currently missing, but defined in clientUtil_server:
        //     ACP_FORMAT
        //     ACP_ERASE_USER
        // missing candidates are 0x80C0 and 0x80D0 or 0x8C00 and 0x8D00

        case 0x8020:
            CmdString = "ACP_Discover";
            break;
        case 0x8030:
            CmdString = "ACP_Change_IP";
            break;
        case 0x8040:
            CmdString = "ACP_Ping";
            break;
        case 0x8050:
            CmdString = "ACP_Info";
            break;
        case 0x8070:
            CmdString = "ACP_FIRMUP_End";
            break;
        case 0x8080:
            CmdString = "ACP_FIRMUP2";
            break;
        case 0x8090:
            CmdString = "ACP_INFO_HDD";
            break;
        case 0x80A0:
            switch (getSpecialCmd(buf)) {
            // ACP_Special - details in packetbuf [32]
            case 0x01:
                CmdString = "SPECIAL_CMD_REBOOT";
                break;
            case 0x02:
                CmdString = "SPECIAL_CMD_SHUTDOWN";
                break;
            case 0x03:
                CmdString = "SPECIAL_CMD_EMMODE";
                break;
            case 0x04:
                CmdString = "SPECIAL_CMD_NORMMODE";
                break;
            case 0x05:
                CmdString = "SPECIAL_CMD_BLINKLED";
                break;
            case 0x06:
                CmdString = "SPECIAL_CMD_SAVECONFIG";
                break;
            case 0x07:
                CmdString = "SPECIAL_CMD_LOADCONFIG";
                break;
            case 0x08:
                CmdString = "SPECIAL_CMD_FACTORYSETUP";
                break;
            case 0x09:
                CmdString = "SPECIAL_CMD_LIBLOCKSTATE";
                break;
            case 0x0a:
                CmdString = "SPECIAL_CMD_LIBLOCK";
                break;
            case 0x0b:
                CmdString = "SPECIAL_CMD_LIBUNLOCK";
                break;
            case 0x0c:
                CmdString = "SPECIAL_CMD_AUTHENICATE";
                break;
            case 0x0d:
                CmdString = "SPECIAL_CMD_EN_ONECMD";
                break;
            case 0x0e:
                CmdString = "SPECIAL_CMD_DEBUGMODE";
                break;
            case 0x0f:
                CmdString = "SPECIAL_CMD_MAC_EEPROM";
                break;
            case 0x12:
                CmdString = "SPECIAL_CMD_MUULTILANG";
                break;
            default:
                CmdString = "Unknown SPECIAL_CMD";
                break;
            }
            break;
        case 0x80D0:
            CmdString = "ACP_PART";
            break;
        case 0x80E0:
            CmdString = "ACP_INFO_RAID";
            break;
        case 0x8A10:
            CmdString = "ACP_CMD";
            break;
        case 0x8B10:
            CmdString = "ACP_FILE_SEND";
            break;
        case 0x8B20:
            CmdString = "ACP_FILESEND_END";
            break;

            // Answers to ACP-Commands
            // Currently missing, but defined in clientUtil_server:
            //     ACP_FORMAT_Reply
            //     ACP_ERASE_USER_Reply
        case 0xC020:
            CmdString = "ACP_Discover_Reply";
            break;
        case 0xC030:
            CmdString = "ACP_Change_IP_Reply";
            break;
        case 0xC040:
            CmdString = "ACP_Ping_Reply";
            break;
        case 0xC050:
            CmdString = "ACP_Info_Reply";
            break;
        case 0xC070:
            CmdString = "ACP_FIRMUP_End_Reply";
            break;
        case 0xC080:
            CmdString = "ACP_FIRMUP2_Reply";
            break;
        case 0xC090:
            CmdString = "ACP_INFO_HDD_Reply";
            break;
        case 0xC0A0:
            CmdString = "ACP_Special_Reply";
            break;
            // further handling possible. - necessary?
        case 0xC0D0:
            CmdString = "ACP_PART_Reply";
            break;
        case 0xC0E0:
            CmdString = "ACP_INFO_RAID_Reply";
            break;
        case 0xCA10:
            CmdString = "ACP_CMD_Reply";
            break;
        case 0xCB10:
            CmdString = "ACP_FILE_SEND_Reply";
            break;
        case 0xCB20:
            CmdString = "ACP_FILESEND_END_Reply";
            break;
            // Unknown! - Error?
        default:
            CmdString = "Unknown ACP command - possible error!";
        }
        return CmdString;
    }

    // retreive ErrorCode out of receive buffer
    private int getErrorCode(byte[] buf) {
        return (int) (buf[28] & 0xFF) + (int) ((buf[29] & 0xFF) << 8) +
                (int) ((buf[30] & 0xFF) << 16) + (int) ((buf[31] & 0xFF) << 24);
    }


    // Translate ErrorCode to meaningful string
    private String getErrorMsg(byte[] buf) {
        String ACPstatus = bufferToHex(buf, 31, 1) + bufferToHex(buf, 30, 1) +
                           bufferToHex(buf, 29, 1) + bufferToHex(buf, 28, 1);
//        String ACPstatus = bufferToHex(buf, 28, 4);
        int ErrorCode = getErrorCode(buf);

        String ErrorString;
        switch (ErrorCode) {
        // There should be an error state ACP_OK, TODO: Test
        case 0x00000000:
            ErrorString = "ACP_STATE_OK";
            break;
        case 0x80000000:
            ErrorString = "ACP_STATE_MALLOC_ERROR";
            break;
        case 0x80000001:
            ErrorString = "ACP_STATE_PASSWORD_ERROR";
            break;
        case 0x80000002:
            ErrorString = "ACP_STATE_NO_CHANGE";
            break;
        case 0x80000003:
            ErrorString = "ACP_STATE_MODE_ERROR";
            break;
        case 0x80000004:
            ErrorString = "ACP_STATE_CRC_ERROR";
            break;
        case 0x80000005:
            ErrorString = "ACP_STATE_NOKEY";
            break;
        case 0x80000006:
            ErrorString = "ACP_STATE_DIFFMODEL";
            break;
        case 0x80000007:
            ErrorString = "ACP_STATE_NOMODEM";
            break;
        case 0x80000008:
            ErrorString = "ACP_STATE_COMMAND_ERROR";
            break;
        case 0x80000009:
            ErrorString = "ACP_STATE_NOT_UPDATE";
            break;
        case 0x8000000A:
            ErrorString = "ACP_STATE_PERMIT_ERROR";
            break;
        case 0x8000000B:
            ErrorString = "ACP_STATE_OPEN_ERROR";
            break;
        case 0x8000000C:
            ErrorString = "ACP_STATE_READ_ERROR";
            break;
        case 0x8000000D:
            ErrorString = "ACP_STATE_WRITE_ERROR";
            break;
        case 0x8000000E:
            ErrorString = "ACP_STATE_COMPARE_ERROR";
            break;
        case 0x8000000F:
            ErrorString = "ACP_STATE_MOUNT_ERROR";
            break;
        case 0x80000010:
            ErrorString = "ACP_STATE_PID_ERROR";
            break;
        case 0x80000011:
            ErrorString = "ACP_STATE_FIRM_TYPE_ERROR";
            break;
        case 0x80000012:
            ErrorString = "ACP_STATE_FORK_ERROR";
            break;
        case 0xFFFFFFFF:
            ErrorString = "ACP_STATE_FAILURE";
            break;
            // unknown error, better use ErrorCode and format it to hex
        default:
            ErrorString = "ACP_STATE_UNKNOWN_ERROR (" + ACPstatus + ")";
        }
        return ErrorString;
    }

    /**
     * setACPHeader
     * Helper function. Creates an ACP header in the given buf.
     *
     * @param buf byte[]        buffer for packet data
     * @param ACPCmd String     HexString (2 byte) with ACPCommand
     * @param ConnID String     HexString (6 byte) with Connection ID
     * @param targetMAC String  HexString (6 byte) with targets MAC
     * @param payloadsize byte  Length of payload following header
     *                          (for ACPSpecial command this is fixed to 0x28 byte!)
     */
    private void setACPHeader(byte[] buf, String ACPCmd, String ConnID,
                              String targetMAC, byte payloadsize) {
        buf[0] = 0x20; // length of header, 32 bytes
        buf[4] = 0x08; // minor packet version
        buf[6] = 0x01; // major packet version
        buf[8] = HexToByte(ACPCmd.substring(2, 4))[0]; // lowbyte of ACP command
        buf[9] = HexToByte(ACPCmd.substring(0, 2))[0]; // highbyte of ACP command
        buf[10] = payloadsize;

        byte[] test = HexToByte(ConnID);
        System.arraycopy(test, 0, buf, 16, 6);
        System.arraycopy(HexToByte(targetMAC), 0, buf, 22, 6);
    }

    // creates an ACPReboot packet, ACP_EN_ONECMD protected
    private byte[] getACPReboot(String ConnID, String targetMAC) {
        byte[] buf = new byte[72];
        setACPHeader(buf, "80a0", ConnID, targetMAC, (byte) (0x28));
        buf[32] = 0x01; // type ACPReboot

        return (buf);
    }

    // creates an ACPShutdown packet, ACP_EN_ONECMD protected
    private byte[] getACPShutdown(String ConnID, String targetMAC) {
        byte[] buf = new byte[72];
        setACPHeader(buf, "80a0", ConnID, targetMAC, (byte) (0x28));
        buf[32] = 0x02; // type ACPShutdown

        return (buf);
    }

    // creates an ACPEMMode packet, ACP_EN_ONECMD protected
    private byte[] getACPEMMode(String ConnID, String targetMAC) {
        byte[] buf = new byte[72];
        setACPHeader(buf, "80a0", ConnID, targetMAC, (byte) (0x28));
        buf[32] = 0x03; // type ACPEMMode

        return (buf);
    }

    // creates an ACPNormMode packet, ACP_EN_ONECMD protected
    private byte[] getACPNormMode(String ConnID, String targetMAC) {
        byte[] buf = new byte[72];
        setACPHeader(buf, "80a0", ConnID, targetMAC, (byte) (0x28));
        buf[32] = 0x04; // type ACPNormmode

        return (buf);
    }

    // creates an ACPBlinkLED packet, also plays a series of tones
    private byte[] getACPBlinkLED(String ConnID, String targetMAC) {
        byte[] buf = new byte[72];
        setACPHeader(buf, "80a0", ConnID, targetMAC, (byte) (0x28));
        buf[32] = 0x05; // type ACPBlinkled

        return (buf);
    }

    // creates an ACPSaveConfig packet
    private byte[] getACPSaveConfig(String ConnID, String targetMAC) {
        byte[] buf = new byte[72];
        setACPHeader(buf, "80a0", ConnID, targetMAC, (byte) (0x28));
        buf[32] = 0x06; // type ACPSaveConfig

        return (buf);
    }

    // creates an ACPLoadConfig packet
    private byte[] getACPLoadConfig(String ConnID, String targetMAC) {
        byte[] buf = new byte[72];
        setACPHeader(buf, "80a0", ConnID, targetMAC, (byte) (0x28));
        buf[32] = 0x07; // type ACPLoadConfig

        return (buf);
    }

    // creates an ACPEnOneCmd packet with the encrypted password (HexString 8 byte)
    private byte[] getACPEnOneCmd(String ConnID, String targetMAC,
                                  byte[] password) {
        byte[] buf = new byte[72];
        setACPHeader(buf, "80a0", ConnID, targetMAC, (byte) 0x28);
        buf[32] = 0x0d;

        System.arraycopy(password, 0, buf, 40, 8);
//        System.arraycopy(HexToByte("14:bd:36:a7:a7:81:86:f1"), 0, buf, 40, 8); // the encrypted password as hexstring
        return (buf);
    }

    // creates an ACPDebugmode packet
    // unclear what this causes on the LS
    private byte[] getACPDebugMode(String ConnID, String targetMAC) {
        byte[] buf = new byte[72];
        setACPHeader(buf, "80a0", ConnID, targetMAC, (byte) (0x28));
        buf[32] = 0x0e; // type ACPDebugmode

        return (buf);
    }

    // creates an ACPMultilang packet, ACP_EN_ONECMD protected
    // Used for setting GUI language, then additional parameter for language is needed
    private byte[] getACPMultiLang(String ConnID, String targetMAC,
                                   byte Language) {
        byte[] buf = new byte[72];
        setACPHeader(buf, "80a0", ConnID, targetMAC, (byte) (0x28));
        buf[32] = 0x12; // type ACPMultilang

        buf[0x24] = Language; // seems to be a 4 byte value, starting at 0x24

        return (buf);
    }

    // creates an ACPDiscover packet
    // LS answers with a packet giving firmware details and a key used for pw encryption
    private byte[] getACPDisc(String ConnID, String targetMAC) {
        byte[] buf = new byte[72];
        setACPHeader(buf, "8020", ConnID, targetMAC, (byte) 0x28);

        return (buf);
    }

    // creates an ACPChangeIP packet
    private byte[] getACPChangeIP(String ConnID, String targetMAC, byte[] newIP,
                                  byte[] newMask, boolean useDHCP,
                                  byte[] encPassword) {
        byte[] buf = new byte[144];
        setACPHeader(buf, "8030", ConnID, targetMAC, (byte) 112);

        System.arraycopy(encPassword, 0, buf, 0x40, encPassword.length);
        // actually 144 byte long, contains password


        if (useDHCP) {
            buf[0x2C] = (byte) 1; // could be: DHCP=true - seems always to be true,
            // expect DHCP and password beyond 0x38
        }
        for (int i = 0; i <= 3; i++) {
            buf[0x33 - i] = newIP[i]; // ip starts at 0x30, low byte first
            buf[0x37 - i] = newMask[i]; // mask starts at 0x34, low byte first
        }

        return (buf);
    }

    // creates an ACPAuthBug packet
    // BUG: the command word should be "80a0" instead of "8a10". This "bug" seems to cause
    // a buffer overflow in clientServer_util which disables the whole authentication
    // process and gives us full access to the ACP commands.

    // the lowest bit =1 of the 3rd (starting with 1) password byte enables the authentication
    private byte[] getACPAuthBug(String ConnID, String targetMAC) {
        byte[] buf = new byte[72];
        setACPHeader(buf, "8a10", ConnID, targetMAC, (byte) 0x28); // here is the bug
        buf[32] = 0x0c;
        System.arraycopy(HexToByte("05:80:24:8d:ab:9c:97:e0"), 0, buf, 40, 8); // the encrypted password as hexstring
        return (buf);
    }

    // create a correct ACPAuth packet
    private byte[] getACPAuth(String ConnID, String targetMAC,
                              String password) {
        return getACPAuth(ConnID, targetMAC, HexToByte(password));
    }

    // create a correct ACPAuth packet
    private byte[] getACPAuth(String ConnID, String targetMAC,
                              byte[] password) {
        byte[] buf = new byte[72];
        setACPHeader(buf, "80a0", ConnID, targetMAC, (byte) 0x28);
        buf[32] = 0x0c;

        System.arraycopy(password, 0, buf, 40, password.length);
        return (buf);
    }


    // creates an ACPCMD packet, used to send telnet-style commands to the LS
    private byte[] getACPCmd(String ConnID, String targetMAC, String cmd) {
        if (cmd.length() > 210) {
            outError("Command line too long (>210 chars).");
        }

        byte[] buf = new byte[cmd.length() + 44];
        setACPHeader(buf, "8a10", ConnID, targetMAC, (byte) (cmd.length() + 12));
        buf[32] = (byte) (cmd.length());
        buf[36] = 0x03; // type

        System.arraycopy(cmd.getBytes(), 0, buf, 40, cmd.length());

        return (buf);
    }

    public byte[] encryptACPpassword(String _password, byte[] _key) {
        if (_password.length() > 24) {
            outError(
                    "The acp_commander only allows password lengths up to 24 chars");
        }
        if (_password.length() == 0) {
            return new byte[8];
        }

        byte[] sub_passwd = new byte[8];
        int sub_length = 0;
        byte[] result = new byte[(_password.length() + 7 >> 3) * 8];

        for (int i = 0; i < (_password.length() + 7) >> 3; i++) {
//             sub_passwd = HexToByte("0DF0ADBA0DF0ADBA"); // shouldn't be necessary
            sub_length = _password.length() - i * 8;
            if (sub_length > 8) {
                sub_length = 8;
            }

            System.arraycopy(_password.substring(i * 8).getBytes(), 0,
                             sub_passwd, 0, sub_length);
            if (sub_length < 8) {
                sub_passwd[sub_length] = (byte) 0x00; // end of string must be 0x00
            }

            System.arraycopy(encACPpassword(sub_passwd, _key), 0, result, i * 8,
                             8);
        }

        return result;
    }

    private byte[] encACPpassword(byte[] _password, byte[] _key) {
        //
        // mimmicks route from LSUpdater.exe, starting at 0x00401700
        // key is a 4 byte array (changed order, key 6ae2ad78 => (0x6a, 0xe2, 0xad, 0x78)
        // password = ap_servd, key= 6ae2ad78 gives encrypted 19:A4:F7:9B:AF:7B:C4:DD
        //
        byte[] new_key = new byte[8];
        byte[] result = new byte[8];

        // first generate initial encryption key (new_key) from key
        for (int i = 0; i < 4; i++) {
            new_key[3 - i] = (byte) (_key[i]); // lower 4 bytes
            new_key[4 + i] = (byte) ((_key[i] ^ _key[3 - i]) * _key[3 - i]); // higher 4 bytes
        }
        // use new_key to generate scrambled (xor) password, new_key is regularly altered
        int j = 0;
        int n;
        for (int i = 0; i < 4; i++) {
            // encryption of first char, first alter new_key
            new_key[0] = (byte) (_password[j] ^ new_key[0]);

            n = 2;
            for (int k = 0; k < i; k++) { // only executed if i > 1
                new_key[n] = (byte) (new_key[n] ^ new_key[n - 2]);
                n = n + 2;
            }

            result[i] = new_key[j];

            // above is repeated (more or less) for 2nd char, first alter new_key
            new_key[1] = (byte) (_password[j + 1] ^ new_key[1]);

            n = 3;
            for (int k = 0; k < i; k++) { // only executed if i > 1
                new_key[n] = (byte) (new_key[n] ^ new_key[n - 2]);
                n = n + 2;
            }

            result[7 - i] = new_key[j + 1];
            j = j + 2;
        }

        return (result);
    }


    private void rcvACPHexDump(byte[] buf) {
        // very simple hex | char debug output of received packet for debugging
        try {
            byte onebyte;

            System.out.println("Buffer-Length: " + buf.length);
            for (int j = 0; j < (buf.length / 16); j++) {
                if (j == 0) {
                    System.out.println("ACP-Header:");
                }
                if (j == 2) {
                    System.out.println("ACP-Payload:");
                }

                System.out.print(j * 16 + "::\t");
                for (int i = 0; i <= 15; i++) {
                    System.out.print(bufferToHex(buf, j * 16 + i, 1) + " ");
                }
                System.out.print("\t");

                for (int i = 0; i <= 15; i++) {
                    onebyte = buf[j * 16 + i];
                    if ((onebyte != 0x0A) & (onebyte != 0x09)) {
                        System.out.print((char) onebyte);
                    } else {
                        System.out.print(" ");
                    }
                }
                System.out.println("");
            }
        } catch (java.lang.ArrayIndexOutOfBoundsException ArrayE) {
            outError(ArrayE.toString());
        }
    }

    /* Analyse ACPDisc answer packet, get hostname, hostIP, DHCP-state, FW-version
     * outACPrcvDisc(byte[] buf, int _debug)
     *  INPUT
     *    buf      ... byte [], buffer with received data
     *    _debug   ... int, debug state
     *  OUTPUT
     *    result   ... String [] string array with results of packet analysis
     *                  0 - "ACPdiscovery reply" = packet type
     *                  1 - formatted output
     *                  2 - host name
     *                  3 - IP
     *                  4 - MAC
     *                  5 - Product string
     *                  6 - Product ID
     *                  7 - FW version
     *                  8 - key (used for pwd encryption in regular authentication process)
     */
    private String[] rcvACPDisc(byte[] buf, int _debug) {
        String[] result = new String[9];
        int _pckttype = 0;
        int _out = 1;
        int _hostname = 2;
        int _ip = 3;
        int _mac = 4;
        int _productstr = 5;
        int _productid = 6;
        int _FWversion = 7;
        int _key = 8;

        for (int i = 0; i < result.length; i++) {
            result[i] = "";
        }

        result[_pckttype] = "ACPdiscovery reply";
        try {
            // get IP
            byte[] targetIP = new byte[4];
            for (int i = 0; i <= 3; i++) {
                targetIP[i] = buf[35 - i];
            }
            InetAddress targetAddr = InetAddress.getByAddress(targetIP);
            result[_ip] = targetAddr.toString();

            // get host name
            int i = 48;
            while ((buf[i] != 0x00) & (i < buf.length)) {
                result[_hostname] = result[_hostname] + (char) buf[i++];
            }

            // Product ID string starts at byte 80
            i = 80;
            while ((buf[i] != 0x00) & (i < buf.length)) {
                result[_productstr] = result[_productstr] + (char) buf[i++];
            }

            // Product ID starts at byte 192 low to high
            for (i = 3; i >= 0; i--) {
                result[_productid] = result[_productid] + buf[192 + i];
            }

            // MAC starts at byte 311
            for (i = 0; i <= 5; i++) {
                result[_mac] = result[_mac] + bufferToHex(buf, i + 311, 1);
                if (i != 5) {
                    result[_mac] = result[_mac] + ":";
                }
            }

            // Key - changes with connectionid (everytime) -> key to password encryption?
            for (i = 0; i <= 3; i++) {
                result[_key] = result[_key] + bufferToHex(buf, 47 - i, 1);
            }

            // Firmware version starts at 187
            result[_FWversion] = buf[187] + buf[188] + "." +
                                 buf[189] + buf[190];

            result[_out] = ("Found:\t" + result[_hostname] +
                            " (" + result[_ip] + ") " +
                            "\t" + result[_productstr] + " (ID=" +
                            result[_productid] + ") " +
                            "\tmac: " + result[_mac] +
                            "\tFirmware=  " + result[_FWversion] +
                            "\tKey=" + result[_key]
                           );
        } catch (java.net.UnknownHostException UnkHostE) {
            outError(UnkHostE.getMessage());
        }
        return (result);
    }

    /* Analyses incoming ACP Replys - TODO progress, still needs better handling
     *  rcvACP(byte[] buf, int _debug)
     *  INPUT
     *    buf      ... byte [], buffer with received data
     *    _debug   ... int, debug state
     *  OUTPUT
     *    result   ... String [] string array with results of packet analysis
     *                  0 - "ACP... reply" = packet type
     *                  1 - formatted output
     *               2..n - possible details (ACPdiscovery)
     */
    private String[] rcvACP(byte[] buf, int _debug) {
        if (_debug >= 3) {
            rcvACPHexDump(buf);
        }

        String[] result;
        String ACPreply = new String();
        int ACPtype = 0;
        String ACPstatus = new String();

        // get type of ACP answer both as long and hexstring
        ACPtype = (buf[8] & 0xFF) + (buf[9] & 0xFF) * 256; // &0xFF necessary to avoid neg. values
        ACPreply = bufferToHex(buf, 9, 1) + bufferToHex(buf, 8, 1);

        //@georg check!
        // value = 0xFFFFFFFF if ERROR occured
        LastError = getErrorCode(buf);
        ACPstatus = bufferToHex(buf, 31, 1) + bufferToHex(buf, 30, 1) +
                    bufferToHex(buf, 29, 1) + bufferToHex(buf, 28, 1);
        if (ACPstatus.equalsIgnoreCase("FFFFFFFF")) {
            outDebug("Received packet (" + ACPreply +
                     ") has the error-flag set!\n" +
                     "For 'Authenticate' that is (usually) OK as we do send a buggy packet.",
                     1);
        }

        switch (ACPtype) {
        case 0xc020: // ACP discovery
            outDebug("received ACP Discovery reply", 1);
            result = rcvACPDisc(buf, _debug);
            break;
        case 0xc030: // ACP changeIP
            outDebug("received ACP change IP reply", 1);
            result = new String[2]; //handling needed ?
            result[0] = "ACP change IP reply";
            result[1] = getErrorMsg(buf);
            break;
        case 0xc0a0: // ACP special command
            outDebug("received ACP special command reply", 1);
            result = new String[2]; //handling needed ?
            result[0] = "ACP special command reply";
            result[1] = getErrorMsg(buf);

//            result[1] = "OK"; // should be set according to ACPstatus!
            break;
        case 0xca10: // ACPcmd
            outDebug("received ACPcmd reply", 1);

            result = new String[2];
            result[0] = "ACPcmd reply";
            result[1] = "";
            int i = 40;
            while ((buf[i] != 0x00) & (i < buf.length)) {
                result[1] = result[1] + (char) buf[i++];
            }

            // filter the LSPro default answere "**no message**" as it led to some user queries/worries
            if (result[1].equalsIgnoreCase("**no message**")) {
                result[1] = "OK (" + getErrorMsg(buf) + ")";
            }
            break;
        default:
            result = new String[2]; //handling needed ?
            result[0] = "Unknown ACP-Reply packet: 0x" + ACPreply;
            result[1] = "Unknown ACP-Reply packet: 0x" + ACPreply; // add correct status!
        }
        outDebug("ACP analysis result: " + result[1], 2);
        return (result);
    }

    //
    // Standard warning, explanation functions
    //

    private void outInfoTimeout() {
        System.out.println(
                "A SocketTimeoutException usually indicates bad firewall settings.\n" +
                "Check especially for *UDP* port " + Port.toString() +
                " and make sure that the connection to your LS is working.");
        if (Port.intValue() != 22936) {
            outWarning("The Timeout could also be caused as you specified " +
                       "(parameter -p) to use port " + Port.toString() +
                       " which differs from standard port 22936.");
        }
    }

    private void outInfoSocket() {
        System.out.println(
                "A SocketException often indicates bad firewall settings.\n" +
                "The acp_commander / your java enviroment needs to send/recevie on UDP port " +
                Port.toString() + ".");
    }

    private void outInfoSetTarget() {
        System.out.println(
                "A UnknownHostException usually indicates that the specified target is not known " +
                "to your PC (can not be resolved).\n" +
                "Possible reasons are typos in the target parameter \"-t\", connection or " +
                "name resolution problems.\n" +
                "Also make sure that the target - here your Linkstation / Terastation - is powered on.");
    }

    //
    // Helper functions, should be moved to own classes
    //

    private void outDebug(String message, int debuglevel) {
        // negative debuglevels are considered as errors!
        if (debuglevel < 0) {
            outError(message);
            return;
        }

        if (debuglevel <= getDebugLevel()) {
            System.out.println(message);
        }
    }

    private void outError(String message) {
        System.err.println("ERROR: " + message);
        System.exit( -1);
    }

    private void outWarning(String message) {
        System.out.println("WARNING: " + message);
    }

    private byte[] HexToByte(String hexstr) {
        String pureHex = hexstr.replaceAll(":", "");
        byte[] bts = new byte[pureHex.length() / 2];
        for (int i = 0; i < bts.length; i++) {
            bts[i] = (byte) Integer.parseInt(pureHex.substring(2 * i, 2 * i + 2),
                                             16);
        }
        return (bts);
    }


    private String bufferToHex(byte buffer[], int startOffset,
                               int length) {
        StringBuffer hexString = new StringBuffer(2 * length);
        int endOffset = startOffset + length;

        for (int i = startOffset; i < endOffset; i++) {
            appendHexPair(buffer[i], hexString);
        }
        return hexString.toString();
    }

    private void appendHexPair(byte b, StringBuffer hexString) {
        char highNibble = kHexChars[(b & 0xF0) >> 4];
        char lowNibble = kHexChars[b & 0x0F];

        hexString.append(highNibble);
        hexString.append(lowNibble);
    }

    private final char kHexChars[] = {'0', '1', '2', '3', '4', '5', '6',
                                     '7', '8', '9', 'A', 'B', 'C', 'D',
                                     'E', 'F'};

}
