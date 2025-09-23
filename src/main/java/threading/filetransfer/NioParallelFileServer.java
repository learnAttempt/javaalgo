package threading.filetransfer;

// NioParallelFileServer.java
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class NioParallelFileServer {
    private static final int PORT = 7000;
    private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;
    private static final String OUTPUT_DIR = "received";

    public static void main(String[] args) throws IOException {
        new File(OUTPUT_DIR).mkdirs();
        ExecutorService workers = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try (ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
            serverChannel.bind(new InetSocketAddress(PORT));
            System.out.println("NIO Parallel Server listening on port " + PORT);

            while (true) {
                SocketChannel client = serverChannel.accept(); // blocking accept
                client.configureBlocking(true);
                System.out.println("Accepted connection from " + client.getRemoteAddress());
                workers.submit(() -> handleClient(client));
            }
        }
    }

    private static void handleClient(SocketChannel client) {
        // We'll read header using ByteBuffer, then use FileChannel.transferFrom loop
        try (SocketChannel socket = client) {
            // 1) Read filename length (4 bytes)
            ByteBuffer intBuf = ByteBuffer.allocate(Integer.BYTES);
            readFully(socket, intBuf);
            intBuf.flip();
            int fileNameLen = intBuf.getInt();

            // 2) Read filename bytes
            ByteBuffer nameBuf = ByteBuffer.allocate(fileNameLen);
            readFully(socket, nameBuf);
            nameBuf.flip();
            String fileName = StandardCharsets.UTF_8.decode(nameBuf).toString();

            // 3) totalChunks (int)
            intBuf.clear();
            readFully(socket, intBuf);
            intBuf.flip();
            int totalChunks = intBuf.getInt();

            // 4) chunkId (int)
            intBuf.clear();
            readFully(socket, intBuf);
            intBuf.flip();
            int chunkId = intBuf.getInt();

            // 5) chunkSize (long)
            ByteBuffer longBuf = ByteBuffer.allocate(Long.BYTES);
            readFully(socket, longBuf);
            longBuf.flip();
            long chunkSize = longBuf.getLong();

            // 6) offset (long)
            longBuf.clear();
            readFully(socket, longBuf);
            longBuf.flip();
            long offset = longBuf.getLong();

            System.out.printf("Receiving file='%s' chunk=%d/%d size=%d offset=%d from %s%n",
                    fileName, chunkId, totalChunks, chunkSize, offset, socket.getRemoteAddress());

            File outFile = new File(OUTPUT_DIR, fileName);
            // Ensure file exists and has enough size (pre-allocate)
            try (RandomAccessFile raf = new RandomAccessFile(outFile, "rw");
                 FileChannel fileChannel = raf.getChannel()) {
                long requiredSize = Math.max(fileChannel.size(), offset + chunkSize);
                if (fileChannel.size() < requiredSize) {
                    raf.setLength(requiredSize); // pre-allocate / extend
                }

                // Now transfer chunkSize bytes from socket into fileChannel at 'offset'
                long remaining = chunkSize;
                long position = offset;
                // Use a temporary ByteBuffer as transferFrom from socket is not direct,
                // but FileChannel.transferFrom supports ReadableByteChannel -> FileChannel.
                // SocketChannel is ReadableByteChannel so we can use transferFrom.
                while (remaining > 0) {
                    long transferred = fileChannel.transferFrom(socket, position, remaining);
                    if (transferred <= 0) {
                        // transferFrom might return 0; then do a small blocking read into buffer and write
                        ByteBuffer tmp = ByteBuffer.allocate((int) Math.min(8192, remaining));
                        int read = socket.read(tmp);
                        if (read <= 0) throw new EOFException("Unexpected EOF while receiving chunk");
                        tmp.flip();
                        while (tmp.hasRemaining()) {
                            position += fileChannel.write(tmp, position);
                        }
                        remaining -= read;
                    } else {
                        position += transferred;
                        remaining -= transferred;
                    }
                }
            }
            System.out.printf("Chunk %d for '%s' written to %s%n", chunkId, fileName, outFile.getAbsolutePath());

            // Optionally: server can send an ACK back (here we write a single byte)
            ByteBuffer ack = ByteBuffer.wrap(new byte[]{1});
            socket.write(ack);

        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // helper: read the exact number of bytes into buffer (blocking)
    private static void readFully(ReadableByteChannel ch, ByteBuffer buf) throws IOException {
        while (buf.hasRemaining()) {
            int r = ch.read(buf);
            if (r < 0) throw new EOFException("Unexpected EOF while reading header");
        }
    }
}
