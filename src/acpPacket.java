package acpcommander;

/**
 * <p>Überschrift: acp_commander</p>
 *
 * <p>Beschreibung: Handling class for the Header part of ACP packets for the
 * Buffalo Linkstation (R). Out of the work of linkstationwiki.net</p>
 *
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Organisation: linkstationwiki.net</p>
 *
 * @author Georg
 * @version 0.1
 */



public class ACPPacket {

    protected byte[] packetbuf;

    // clear the packetbuf, set Buffersize to ACP-Header size of 32 bytes
    public void clearBuffer() {
        byte[] newbuf = new byte[32];
        packetbuf = newbuf;
    }

    // clear the packetbuf, set Buffersize to given length
    public void clearBuffer(int length) {
        byte[] newbuf = new byte[length];
        packetbuf = newbuf;
    }

    // setup the ACP header of the packet
    public void setHeader(byte HeaderLen, byte majorVer, byte minorVer,
                          String ACPCmd,
                          byte PayloadSize, String ConnID, String targetMAC) {
        setHeaderLength(HeaderLen);
        setVersion(majorVer, minorVer);
        setCommand(ACPCmd);
        setPayloadSize(PayloadSize);
        setConnID(ConnID);
        setTargetMAC(targetMAC);
    }

    // setup the ACP header of the packet with standard values for
    // version, ConnID and broadcast targetMAC
    protected void setHeader() {
        setHeader((byte) 0x20, (byte) 0x01, (byte) 0x08, "0000", (byte) 0,
                  "00:50:56:c0:00:08", "FF:FF:FF:FF:FF:FF");
    }

    // setup the ACP header using standard values and the given values.
    public void setHeader(String ACPCmd, String ConnID, String targetMAC,
                          byte PayloadSize) {
        setHeader(); // use default settings for the ACPHeader;
        if ((ACPCmd != null) && (!ACPCmd.equals(""))) {
            setCommand(ACPCmd);
        }
        if ((ConnID != null) && (!ConnID.equals(""))) {
            setConnID(ConnID);
        }
        if ((targetMAC != null) && (!targetMAC.equals(""))) {
            setTargetMAC(targetMAC);
        }
        setPayloadSize(PayloadSize);
    }

    public void setHeaderLength(byte length) {
        packetbuf[0] = length;
    }

    public void setVersion(byte major, byte minor) {
        packetbuf[4] = minor;
        packetbuf[6] = major;
    }

    public void setCommand(String ACPCmd) {
        // check input param for length and unwanted chars, including ":"
        setCommand(HexToByte(ACPCmd.substring(2, 4))[0], // lowbyte
                   HexToByte(ACPCmd.substring(0, 2))[0]); // highbyte
    }

    public void setCommand(String ACPCmd, byte PayloadSize) {
        setCommand(ACPCmd);
        setPayloadSize(PayloadSize);
    }

    public void setCommand(String ACPCmd, byte SpecialCmd, byte PayloadSize) {
        setCommand(ACPCmd);
        setSpecialCmd(SpecialCmd);
        setPayloadSize(PayloadSize);
    }

    public void setCommand(byte lowbyte, byte highbyte) {
        packetbuf[8] = lowbyte;
        packetbuf[9] = highbyte;
    }

    public int getCommand() {
        return (int) ((packetbuf[9] & 0xFF) << 8) + (int) (packetbuf[8] & 0xFF);
    }


    // Translate command code from packet to meaningful string;
    public String getCmdString() {
        int ACPCmd = getCommand();
        String CmdString = new String("");

        switch (ACPCmd) {
        // ACP_Commands
        // Currently missing, but defined in clientUtil_server:
        //     ACP_FORMAT
        //     ACP_ERASE_USER

        case 0x8020: CmdString = "ACP_Discover"; break;
        case 0x8030: CmdString = "ACP_Change_IP"; break;
        case 0x8040: CmdString = "ACP_Ping"; break;
        case 0x8050: CmdString = "ACP_Info"; break;
        case 0x8070: CmdString = "ACP_FIRMUP_End"; break;
        case 0x8080: CmdString = "ACP_FIRMUP2"; break;
        case 0x8090: CmdString = "ACP_INFO_HDD"; break;
        case 0x80A0:
            switch (getSpecialCmd()) {
            // ACP_Special - details in packetbuf [32]
            case 0x01: CmdString = "SPECIAL_CMD_REBOOT"; break;
            case 0x02: CmdString = "SPECIAL_CMD_SHUTDOWN"; break;
            case 0x03: CmdString = "SPECIAL_CMD_EMMODE"; break;
            case 0x04: CmdString = "SPECIAL_CMD_NORMMODE"; break;
            case 0x05: CmdString = "SPECIAL_CMD_BLINKLED"; break;
            case 0x06: CmdString = "SPECIAL_CMD_SAVECONFIG"; break;
            case 0x07: CmdString = "SPECIAL_CMD_LOADCONFIG"; break;
            case 0x08: CmdString = "SPECIAL_CMD_FACTORYSETUP"; break;
            case 0x09: CmdString = "SPECIAL_CMD_LIBLOCKSTATE"; break;
            case 0x0a: CmdString = "SPECIAL_CMD_LIBLOCK"; break;
            case 0x0b: CmdString = "SPECIAL_CMD_LIBUNLOCK"; break;
            case 0x0c: CmdString = "SPECIAL_CMD_AUTHENICATE"; break;
            case 0x0d: CmdString = "SPECIAL_CMD_EN_ONECMD"; break;
            case 0x0e: CmdString = "SPECIAL_CMD_DEBUGMODE"; break;
            case 0x0f: CmdString = "SPECIAL_CMD_MAC_EEPROM"; break;
            case 0x12: CmdString = "SPECIAL_CMD_MUULTILANG"; break;
            default: CmdString = "Unknown SPECIAL_CMD"; break;
            } break;
        case 0x80D0: CmdString = "ACP_PART"; break;
        case 0x80E0: CmdString = "ACP_INFO_RAID"; break;
        case 0x8A10: CmdString = "ACP_CMD"; break;
        case 0x8B10: CmdString = "ACP_FILE_SEND"; break;
        case 0x8B20: CmdString = "ACP_FILESEND_END"; break;

        // Answers to ACP-Commands
        // Currently missing, but defined in clientUtil_server:
        //     ACP_FORMAT_Reply
        //     ACP_ERASE_USER_Reply
        case 0xC020: CmdString = "ACP_Discover_Reply"; break;
        case 0xC030: CmdString = "ACP_Change_IP_Reply"; break;
        case 0xC040: CmdString = "ACP_Ping_Reply"; break;
        case 0xC050: CmdString = "ACP_Info_Reply"; break;
        case 0xC070: CmdString = "ACP_FIRMUP_End_Reply"; break;
        case 0xC080: CmdString = "ACP_FIRMUP2_Reply"; break;
        case 0xC090: CmdString = "ACP_INFO_HDD_Reply"; break;
        case 0xC0A0: CmdString = "ACP_Special_Reply"; break;
                                               // further handling possible. - necessary?
        case 0xC0D0: CmdString = "ACP_PART_Reply"; break;
        case 0xC0E0: CmdString = "ACP_INFO_RAID_Reply"; break;
        case 0xCA10: CmdString = "ACP_CMD_Reply"; break;
        case 0xCB10: CmdString = "ACP_FILE_SEND_Reply"; break;
        case 0xCB20: CmdString = "ACP_FILESEND_END_Reply"; break;
        // Unknown! - Error?
        default: CmdString = "Unknown ACP command - possible error!";
        }
        return CmdString;
    }

    public byte getSpecialCmd() {
        return packetbuf[32];
    }

    public void setSpecialCmd(byte SpecialCmdByte) {
        packetbuf[32] = SpecialCmdByte;
    }

    public void setPayloadSize(byte PayloadSize) {
        packetbuf[10] = PayloadSize;
    }

    public byte getPayloadSize() {
        return packetbuf[10];
    }

    public void setConnID(String ConnID) {
        // check input param for length and unwanted chars
        System.arraycopy(HexToByte(ConnID), 0, packetbuf, 16, 6);
    }

    public void setTargetMAC(String targetMAC) {
        // check input param for length and unwanted chars
        System.arraycopy(HexToByte(targetMAC), 0, packetbuf, 22, 6);
    }


    public void acpPacket() {
        try {
            clearBuffer(); // initialize the PacketBuffer packetbuf with 32 bytes (ACP Header length)
            setHeader(); // initialize the ACP Header with some standard values
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    // Hex-Helper, should be moved to library
    /// <p> converts the given Hexstring to a byte array, ":" are removed
    protected static byte[] HexToByte(String hexstr) {
        String pureHex = hexstr.replaceAll(":", "");
        byte[] bts = new byte[pureHex.length() / 2];
        for (int i = 0; i < bts.length; i++) {
            bts[i] = (byte) Integer.parseInt(pureHex.substring(2 * i, 2 * i + 2),
                                             16);
        }
        return (bts);
    }

    protected static String bufferToHex(byte buffer[], int startOffset,
                                        int length) {
        StringBuffer hexString = new StringBuffer(2 * length);
        int endOffset = startOffset + length;

        for (int i = startOffset; i < endOffset; i++) {
            appendHexPair(buffer[i], hexString);
        }
        return hexString.toString();
    }

    protected static void appendHexPair(byte b, StringBuffer hexString) {
        char highNibble = kHexChars[(b & 0xF0) >> 4];
        char lowNibble = kHexChars[b & 0x0F];

        hexString.append(highNibble);
        hexString.append(lowNibble);
    }

    protected static final char kHexChars[] = {'0', '1', '2', '3', '4', '5',
                                              '6',
                                              '7', '8', '9', 'A', 'B', 'C', 'D',
                                              'E', 'F'};
// End of HEX-helper
}
