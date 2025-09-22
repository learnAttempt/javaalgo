package threading;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.concurrent.*;

public class ParallelFileTransfer {

    private static final int CHUNK_SIZE = 512 * 1024 * 1024; // 512 MB
    private static final int THREADS = 4;

    public static void sendFile(String filePath, String host, int port) throws Exception {
        File file = new File(filePath);
        long fileSize = file.length();
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            FileChannel channel = raf.getChannel();
            long numChunks = (fileSize + CHUNK_SIZE - 1) / CHUNK_SIZE;

            for (int i = 0; i < numChunks; i++) {
                final int chunkId = i;
                pool.submit(() -> {
                    try {
                        long pos = (long) chunkId * CHUNK_SIZE;
                        long size = Math.min(CHUNK_SIZE, fileSize - pos);

                        // Read chunk
                        ByteBuffer buffer = ByteBuffer.allocateDirect((int) size);
                        channel.read(buffer, pos);
                        buffer.flip();

                        // Send chunk
                        try (Socket socket = new Socket(host, port);
                             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
                            out.writeInt(chunkId);      // send chunk ID
                            out.writeLong(size);        // send chunk size
                            byte[] data = new byte[buffer.remaining()];
                            buffer.get(data);
                            out.write(data);
                            out.flush();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        }

        pool.shutdown();
        pool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    }

    public static void receiveFile(String destPath, int port, long expectedSize) throws Exception {
        RandomAccessFile raf = new RandomAccessFile(destPath, "rw");
        raf.setLength(expectedSize);
        FileChannel channel = raf.getChannel();

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);

        try (ServerSocket server = new ServerSocket(port)) {
            while (expectedSize > 0) {
                Socket socket = server.accept();
                pool.submit(() -> {
                    try (DataInputStream in = new DataInputStream(socket.getInputStream())) {
                        int chunkId = in.readInt();
                        long size = in.readLong();
                        byte[] data = new byte[(int) size];
                        in.readFully(data);

                        long pos = (long) chunkId * CHUNK_SIZE;
                        ByteBuffer buffer = ByteBuffer.wrap(data);
                        channel.write(buffer, pos);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        }

        pool.shutdown();
        pool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws Exception {
        // Example usage:
        // On receiver system:
        //   new ParallelFileTransfer().receiveFile("/dest/file", 9000, 40L * 1024 * 1024 * 1024);
        // On sender system:
        //   new ParallelFileTransfer().sendFile("/src/file", "receiver-ip", 9000);
    }
}

