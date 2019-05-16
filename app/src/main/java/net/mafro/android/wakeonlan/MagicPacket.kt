/*
Copyright (C) 2008-2014 Matt Black
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.
* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.
* Neither the name of the author nor the names of its contributors may be used
  to endorse or promote products derived from this software without specific
  prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package net.mafro.android.wakeonlan

import android.content.Context
import androidx.annotation.WorkerThread
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.Callable
import java.util.regex.Pattern


/**
 * @desc    Static WOL magic packet class
 */
object MagicPacket {
    internal const val BROADCAST = "192.168.1.255"
    internal const val PORT = 9
    private const val SEPARATOR = ':'

    @WorkerThread
    @Throws(IOException::class, IllegalArgumentException::class)
    internal fun send(mac: String, ip: String, port: Int = PORT): String {
        // validate MAC and chop into array
        val hex = validateMac(mac)

        // convert to base16 bytes
        val macBytes = ByteArray(6)
        for (i in 0..5) {
            macBytes[i] = Integer.parseInt(hex[i], 16).toByte()
        }

        val bytes = ByteArray(102)

        // fill first 6 bytes
        for (i in 0..5) {
            bytes[i] = 0xff.toByte()
        }
        // fill remaining bytes with target MAC
        var i = 6
        while (i < bytes.size) {
            System.arraycopy(macBytes, 0, bytes, i, macBytes.size)
            i += macBytes.size
        }

        // create socket to IP
        val address = InetAddress.getByName(ip)
        val packet = DatagramPacket(bytes, bytes.size, address, port)
        val socket = DatagramSocket()
        socket.send(packet)
        socket.close()

        return hex[0] + SEPARATOR + hex[1] + SEPARATOR + hex[2] + SEPARATOR + hex[3] + SEPARATOR + hex[4] + SEPARATOR + hex[5]
    }

    @Throws(IllegalArgumentException::class)
    internal fun cleanMac(mac: String): String {
        val hex = validateMac(mac)

        var sb = StringBuffer()
        var isMixedCase = false

        // check for mixed case
        for (i in 0..5) {
            sb.append(hex[i])
        }
        val testMac = sb.toString()
        if (testMac.toLowerCase() != testMac && testMac.toUpperCase() != testMac) {
            isMixedCase = true
        }

        sb = StringBuffer()
        for (i in 0..5) {
            // convert mixed case to lower
            if (isMixedCase) {
                sb.append(hex[i].toLowerCase())
            } else {
                sb.append(hex[i])
            }
            if (i < 5) {
                sb.append(SEPARATOR)
            }
        }
        return sb.toString()
    }

    @Throws(IllegalArgumentException::class)
    private fun validateMac(mac: String): Array<String> {
        var mac = mac
        // error handle semi colons
        mac = mac.replace(";", ":")

        // attempt to assist the user a little
        if (mac.matches("([a-zA-Z0-9]){12}".toRegex())) {
            // expand 12 chars into a valid mac address
            val macBuilder = StringBuilder()
            for (i in 0 until mac.length) {
                if (i > 1 && i % 2 == 0) {
                    macBuilder.append(':')
                }
                macBuilder.append(mac[i])
            }
            mac = macBuilder.toString()
        }

        // regexp pattern match a valid MAC address
        val pat = Pattern.compile("((([0-9a-fA-F]){2}[-:]){5}([0-9a-fA-F]){2})")
        val m = pat.matcher(mac)

        if (m.find()) {
            val result = m.group()
            return result.split("(\\:|\\-)".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        } else {
            throw IllegalArgumentException("Invalid MAC address")
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size != 2) {
            println("Usage: java MagicPacket <broadcast-ip> <mac-address>")
            println("Example: java MagicPacket 192.168.0.255 00:0D:61:08:22:4A")
            println("Example: java MagicPacket 192.168.0.255 00-0D-61-08-22-4A")
            System.exit(1)
        }

        val ipStr = args[0]
        var macStr = args[1]

        try {
            macStr = cleanMac(macStr)
            println("Sending to: $macStr")
            send(macStr, ipStr)
        } catch (e: IllegalArgumentException) {
            println(e.message)
        } catch (e: Exception) {
            println("Failed to send Wake-on-LAN packet:" + e.message)
        }

    }

    internal fun sendPacket(context: Context, item: HistoryIt) {
        createSendPacketSingle(context, item.title, item.mac, item.ip, item.port).subscribe()
    }

    fun sendPacket(context: Context, title: String, mac: String, ip: String, port: Int) {
        createSendPacketSingle(context, title, mac, ip, port)
                .subscribe()
    }

    private fun createSendPacketSingle(context: Context, title: String, mac: String, ip: String, port: Int): Single<String> {
        return Single.fromCallable(MagicPacketCallable(mac, ip, port))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(MagicPacketErrorAction(context))
                .doOnSuccess(MagicPacketSuccessAction(context, title))
    }

    private class MagicPacketCallable constructor(private val mac: String, private val ip: String, private val port: Int) : Callable<String> {
        @Throws(IOException::class)
        override fun call(): String {
            return send(mac, ip, port)
        }
    }
}

internal class MagicPacketSuccessAction constructor(private val context: Context, private val title: String) : Consumer<String> {
    override fun accept(s: String) {
        // display sent message to user
        val msg = String.format("%s to %s", context.getString(R.string.packet_sent), title)
        WakeOnLanActivity.notifyUser(msg, context)
    }
}

internal class MagicPacketErrorAction constructor(private val context: Context) : Consumer<Throwable> {
    override fun accept(throwable: Throwable) {
        val msg = String.format("%s:\n%s", context.getString(R.string.send_failed), throwable.message)
        WakeOnLanActivity.notifyUser(msg, context)
    }
}