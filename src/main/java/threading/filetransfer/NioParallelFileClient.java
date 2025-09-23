package threading.filetransfer;

// NioParallelFileClient.java
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class NioParallelFileClient {
    private static final int CHUNK_SIZE = 4 * 1024 * 1024; // 4MB
    private static final int THREADS = 4;
    private static final int SERVER_PORT = 7000;
    private static final String SERVER_HOST = "127.0.0.1";

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: java NioParallelFileClient <filePath>");
            return;
        }
        String path = args[0];
        File file = new File(path);
        if (!file.exists()) {
            System.err.println("File doesn't exist: " + path);
            return;
        }

        long fileSize = file.length();
        int totalChunks = (int) Math.ceil((double) fileSize / CHUNK_SIZE);

        ExecutorService exec = Executors.newFixedThreadPool(THREADS);
        for (int chunkId = 0; chunkId < totalChunks; chunkId++) {
            final int cid = chunkId;
            exec.submit(() -> {
                try {
                    sendChunk(file, cid, totalChunks);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }

        exec.shutdown();
        exec.awaitTermination(1, TimeUnit.HOURS);
        System.out.println("All chunks sent.");
    }

    private static void sendChunk(File file, int chunkId, int totalChunks) throws IOException {
        long offset = (long) chunkId * CHUNK_SIZE;
        long remaining = Math.min(CHUNK_SIZE, file.length() - offset);

        try (SocketChannel socket = SocketChannel.open(new InetSocketAddress(SERVER_HOST, SERVER_PORT));
             RandomAccessFile raf = new RandomAccessFile(file, "r");
             FileChannel fileChannel = raf.getChannel()) {

            // Build header
            byte[] fileNameBytes = file.getName().getBytes(StandardCharsets.UTF_8);
            ByteBuffer header = ByteBuffer.allocate(
                    Integer.BYTES + fileNameBytes.length + Integer.BYTES + Integer.BYTES + Long.BYTES + Long.BYTES);

            header.putInt(fileNameBytes.length);
            header.put(fileNameBytes);
            header.putInt(totalChunks);
            header.putInt(chunkId);
            header.putLong(remaining);
            header.putLong(offset);
            header.flip();

            // Send header
            while (header.hasRemaining()) socket.write(header);

            // Use FileChannel.transferTo to send this chunk region to socket (zero-copy where possible).
            long pos = offset;
            long toTransfer = remaining;
            while (toTransfer > 0) {
                long transferred = fileChannel.transferTo(pos, toTransfer, socket);
                if (transferred <= 0) {
                    // transferTo may return 0 in some cases; do a manual copy
                    ByteBuffer tmp = ByteBuffer.allocate((int) Math.min(8192, toTransfer));
                    fileChannel.position(pos);
                    int read = fileChannel.read(tmp);
                    if (read <= 0) break;
                    tmp.flip();
                    while (tmp.hasRemaining()) socket.write(tmp);
                    pos += read;
                    toTransfer -= read;
                } else {
                    pos += transferred;
                    toTransfer -= transferred;
                }
            }

            // Optionally read ACK from server
            ByteBuffer ack = ByteBuffer.allocate(1);
            socket.read(ack);
            System.out.printf("Sent chunk %d (size=%d) offset=%d%n", chunkId, remaining, offset);
        }
    }
}
