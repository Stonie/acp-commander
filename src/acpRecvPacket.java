package acpcommander;

/**
 * <p>Überschrift: acp_commander</p>
 *
 * <p>Beschreibung: Handling for ACP Replies from the Buffalo Linkstation (R). Out of the
 * work of linkstationwiki.net</p>
 *
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Organisation: linkstationwiki.net</p>
 *
 * @author Georg
 * @version 0.1
 */

/*----------------------------------------------------------------------------------------
 ACP-Responses, same as ACP-Commands, but with bit 14 set
 Source: EricC @ linkstationwiki.net // ethereal sniff with clientServer_util in debug mode
 Question: check in answers on ACP_Special if buffer [32] is really set.
 ------------------------------------------------------------------------------------------
 C020 ACP_Discover_Reply
 C030 ACP_Change_IP_Reply
 C040 ACP_Ping_Reply
 C050 ACP_Info_Reply
 C070 ACP_FIRMUP_End_Reply
 C080 ACP_FIRMUP2_Reply
 C090 ACP_INFO_HDD_Reply
 C0A0 ACP_Special_Reply
 ---- 01 SPECIAL_CMD_REBOOT_Reply
 ---- 02 SPECIAL_CMD_SHUTDOWN_Reply
 ---- 03 SPECIAL_CMD_EMMODE_Reply
 ---- 04 SPECIAL_CMD_NORMMODE_Reply
 ---- 05 SPECIAL_CMD_BLINKLED_Reply
 ---- 06 SPECIAL_CMD_SAVECONFIG_Reply
 ---- 07 SPECIAL_CMD_LOADCONFIG_Reply
 ---- 08 SPECIAL_CMD_FACTORYSETUP_Reply
 ---- 09 SPECIAL_CMD_LIBLOCKSTATE_Reply
 ---- 0a SPECIAL_CMD_LIBLOCK_Reply
 ---- 0b SPECIAL_CMD_LIBUNLOCK_Reply
 ---- 0c SPECIAL_CMD_AUTHENICATE_Reply
 ---- 0d SPECIAL_CMD_EN_ONECMD_Reply
 ---- 0e SPECIAL_CMD_DEBUGMODE_Reply
 ---- 0f SPECIAL_CMD_MAC_EEPROM_Reply
 ---- 12 SPECIAL_CMD_MUULTILANG_Reply
 C0D0 ACP_PART_Reply
 C0E0 ACP_INFO_RAID_Reply
 CA10 ACP_CMD_Reply
 CB10 ACP_FILE_SEND_Reply
 CB20 ACP_FILESEND_END_Reply
 ----------------------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------------------
 ACP-Error Codes
 Source: Georg @ linkstationwiki.net // case construct in LSUpdater.exe in route 004017d0
 -----------------------------------------------------------------------------------------
 ASCII "ACP_STATE_MALLOC_ERROR"; Case 80000000
 ASCII "ACP_STATE_PASSWORD_ERROR"; Case 80000001
 ASCII "ACP_STATE_MODE_ERROR"; Case 80000003
 ASCII "ACP_STATE_NO_CHANGE"; Case 80000002
 ASCII "ACP_STATE_CRC_ERROR"; Case 80000004
 ASCII "ACP_STATE_NOKEY"; Case 80000005
 ASCII "ACP_STATE_DIFFMODEL"; Case 80000006
 ASCII "ACP_STATE_NOMODEM"; Case 80000007
 ASCII "ACP_STATE_COMMAND_ERROR"; Case 80000008
 ASCII "ACP_STATE_NOT_UPDATE"; Case 80000009
 ASCII "ACP_STATE_PERMIT_ERROR"; Case 8000000A
 ASCII "ACP_STATE_OPEN_ERROR"; Case 8000000B
 ASCII "ACP_STATE_READ_ERROR"; Case 8000000C
 ASCII "ACP_STATE_WRITE_ERROR"; Case 8000000D
 ASCII "ACP_STATE_COMPARE_ERROR"; Case 8000000E
 ASCII "ACP_STATE_MOUNT_ERROR"; Case 8000000F
 ASCII "ACP_STATE_PID_ERROR"; Case 80000010
 ASCII "ACP_STATE_FIRM_TYPE_ERROR"; Case 80000011
 ASCII "ACP_STATE_FORK_ERROR"; Case 80000012
 ASCII "ACP_STATE_FAILURE"; Case FFFFFFFF
 */

import java.text.*;

public class ACPRecvPacket extends ACPPacket {

    // Fill the data for the ACPDiscoverReply packet formated into a string.
    // Java does not support structures, so we'd have to create a class for each packet type
    private String getACPDiscoverReply() {
        String Reply = new String();
        String Test = new String("Test");

        // however using MessageFormat requires us to create at least an Object, anyway.
        Object[] DiscoverReply = {
                                 new String(Test) // TargetIP
        };
        Reply = Reply + MessageFormat.format("TargetIP = {0}", DiscoverReply);



        return Reply;
    }

    // Copy the output of the cmdline into a string.
    private String getACPCmdReply() {
        // The output string of the cmdline starts at byte 40 of the packet
        String Reply = new String();
        int i = 40;
        while ((packetbuf[i] != 0x00) & (i < packetbuf.length)) {
            Reply = Reply + (char) packetbuf[i++];
        }
        return Reply;
    }

    public String getString() {
        String Reply = new String();
        return Reply;
    }


    // ErrorCode in answer packet is in bytes 0x2c to 0x2f
    public int getErrorCode() {
        Byte ErrorCodeBytes = new Byte("");
        System.arraycopy(packetbuf, 0x2c, ErrorCodeBytes, 0, 4);
        return ErrorCodeBytes.intValue();
    }

    // Translate ErrorCode to meaningful string
    public String getErrorMsg() {
        int ErrorCode = getErrorCode();
        String ErrorString;
        switch (ErrorCode) {
        // There should be an error state ACP_OK, TODO: Test
        // case 0x00000000: { ErrorString = "ACP_OK"; }
        case 0x80000000: {
            ErrorString = "ACP_STATE_MALLOC_ERROR";
        }
        case 0x80000001: {
            ErrorString = "ACP_STATE_PASSWORD_ERROR";
        }
        case 0x80000002: {
            ErrorString = "ACP_STATE_NO_CHANGE";
        }
        case 0x80000003: {
            ErrorString = "ACP_STATE_MODE_ERROR";
        }
        case 0x80000004: {
            ErrorString = "ACP_STATE_CRC_ERROR";
        }
        case 0x80000005: {
            ErrorString = "ACP_STATE_NOKEY";
        }
        case 0x80000006: {
            ErrorString = "ACP_STATE_DIFFMODEL";
        }
        case 0x80000007: {
            ErrorString = "ACP_STATE_NOMODEM";
        }
        case 0x80000008: {
            ErrorString = "ACP_STATE_COMMAND_ERROR";
        }
        case 0x80000009: {
            ErrorString = "ACP_STATE_NOT_UPDATE";
        }
        case 0x8000000A: {
            ErrorString = "ACP_STATE_PERMIT_ERROR";
        }
        case 0x8000000B: {
            ErrorString = "ACP_STATE_OPEN_ERROR";
        }
        case 0x8000000C: {
            ErrorString = "ACP_STATE_READ_ERROR";
        }
        case 0x8000000D: {
            ErrorString = "ACP_STATE_WRITE_ERROR";
        }
        case 0x8000000E: {
            ErrorString = "ACP_STATE_COMPARE_ERROR";
        }
        case 0x8000000F: {
            ErrorString = "ACP_STATE_MOUNT_ERROR";
        }
        case 0x80000010: {
            ErrorString = "ACP_STATE_PID_ERROR";
        }
        case 0x80000011: {
            ErrorString = "ACP_STATE_FIRM_TYPE_ERROR";
        }
        case 0x80000012: {
            ErrorString = "ACP_STATE_FORK_ERROR";
        }
        case 0xFFFFFFFF: {
            ErrorString = "ACP_STATE_FAILURE";
        }
        default: {
            ErrorString = "Unknown ACP Error Code";
        }
        }
        return ErrorString;
    }

    public void acpRecvPacket() {
        // initialization shoud be done in acpPacket, via jbInit();

        clearBuffer(0); // set the Buffers length to 0, so we can read the truely
                        // recieved bytes once we get them.
    }

}
