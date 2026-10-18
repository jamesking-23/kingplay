package com.kingplay.desktop.launch

import java.io.BufferedReader
import java.io.Closeable
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

/**
 * Keeps one KingPlay running. The first copy listens on a local port (this computer only); a second copy hands its
 * files to the first and quits, so "Open with" adds to the running player instead of starting another one that would
 * write to the same data files. A short greeting line stops any other program that happens to talk to the port from
 * being treated as a KingPlay.
 */
class SingleInstance(private val port: Int) : Closeable {
    @Volatile private var server: ServerSocket? = null

    /**
     * Returns true when this copy should carry on running (it is the first one, or the port is in use by something
     * that is not KingPlay), and false when the files were handed to the running copy and this one should quit.
     * [onMessage] receives the paths the other copies send, possibly none (just "show yourself").
     */
    fun acquireOrForward(paths: List<String>, onMessage: (List<String>) -> Unit): Boolean {
        val socket = try {
            ServerSocket(port, BACKLOG, InetAddress.getLoopbackAddress())
        } catch (e: IOException) {
            return !forward(paths)
        }
        server = socket
        val thread = Thread({ serve(socket, onMessage) }, "kingplay-single-instance")
        thread.isDaemon = true
        thread.start()
        return true
    }

    private fun serve(socket: ServerSocket, onMessage: (List<String>) -> Unit) {
        while (!socket.isClosed) {
            try {
                socket.accept().use { client ->
                    client.soTimeout = READ_TIMEOUT_MS
                    val lines = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
                        .lineSequence().take(MAX_LINES).toList()
                    if (lines.firstOrNull() == GREETING) onMessage(lines.drop(1).filter { it.isNotBlank() })
                }
            } catch (e: IOException) {
                if (socket.isClosed) return // closed on purpose; any other error is one bad client, keep serving
            }
        }
    }

    private fun forward(paths: List<String>): Boolean = try {
        Socket(InetAddress.getLoopbackAddress(), port).use { client ->
            client.getOutputStream().writer(Charsets.UTF_8).use { out ->
                out.write(GREETING + "\n")
                paths.forEach { out.write(it + "\n") }
            }
        }
        true
    } catch (e: IOException) {
        false
    }

    override fun close() {
        server?.close()
    }

    companion object {
        const val GREETING = "KINGPLAY-OPEN-1"
        const val DEFAULT_PORT = 47_615
        private const val BACKLOG = 8
        private const val READ_TIMEOUT_MS = 2_000
        private const val MAX_LINES = 600
    }
}
