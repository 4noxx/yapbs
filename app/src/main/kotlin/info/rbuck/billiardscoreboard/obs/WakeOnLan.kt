package info.rbuck.billiardscoreboard.obs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Sends a Wake-on-LAN "magic packet" to wake the OBS PC from sleep (or, if its NIC/BIOS support
 * it, from a full shutdown) - see [info.rbuck.billiardscoreboard.data.AppSettingsRepository.obsWakeOnLanMac]
 * for the MAC address this reads, configured in Settings > OBS WebSocket.
 *
 * This only wakes the machine - it can't launch OBS itself (there's nothing listening on a asleep/
 * off PC that could receive a "start OBS" command; a magic packet is the one thing hardware can
 * react to before the OS is even running). Once awake, [ObsWebSocketClient]'s own 5s reconnect
 * loop picks the connection back up as soon as OBS (with its WebSocket server) is reachable again -
 * see Settings > OBS WebSocket and the "start OBS automatically" note there for the other half.
 *
 * Requires the tablet and the OBS PC to be on the same local network segment (broadcast doesn't
 * cross routers/VLANs) and the PC's network adapter to have Wake-on-LAN enabled - both in its
 * BIOS/UEFI and in Windows' adapter properties ("Allow this device to wake the computer" /
 * "Wake on Magic Packet"). Works reliably from sleep on wired Ethernet; Wi-Fi and full shutdown
 * support vary a lot by hardware.
 */
object WakeOnLan {
    private const val MAGIC_PACKET_PORT = 9
    private const val BROADCAST_ADDRESS = "255.255.255.255"

    /** Returns true if a well-formed magic packet was actually sent. This is not delivery/wake
     * confirmation - Wake-on-LAN is fire-and-forget UDP, there's no acknowledgement to wait for. */
    suspend fun sendMagicPacket(macAddress: String): Boolean = withContext(Dispatchers.IO) {
        val macBytes = parseMac(macAddress) ?: return@withContext false
        val packet = ByteArray(6 + 16 * 6)
        for (i in 0 until 6) packet[i] = 0xFF.toByte()
        for (i in 6 until packet.size step 6) macBytes.copyInto(packet, i)
        try {
            DatagramSocket().use { socket ->
                socket.broadcast = true
                socket.send(DatagramPacket(packet, packet.size, InetAddress.getByName(BROADCAST_ADDRESS), MAGIC_PACKET_PORT))
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun parseMac(mac: String): ByteArray? {
        val hex = mac.trim().replace(Regex("[:\\-.]"), "")
        if (hex.length != 12 || hex.any { it.digitToIntOrNull(16) == null }) return null
        return ByteArray(6) { i -> hex.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
    }
}
