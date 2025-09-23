package threading.filetransfer;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ParallelFileClient {
    private static final int CHUNK_SIZE = 4 * 1024 * 1024; // 4MB per chunk
    private static final int THREADS = 4;

    public static void main(String[] args) throws Exception {
        String serverAddress = "127.0.0.1";
        int port = 6000;
        String filePath = "large_file.dat";

        File file = new File(filePath);
        long fileSize = file.length();
        int totalChunks = (int) Math.ceil((double) fileSize / CHUNK_SIZE);

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);

        for (int chunkId = 0; chunkId < totalChunks; chunkId++) {
            final int id = chunkId;
            executor.submit(() -> sendChunk(serverAddress, port, file, id, totalChunks));
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);
        System.out.println("All chunks sent successfully.");
    }

    private static void sendChunk(String serverAddress, int port, File file, int chunkId, int totalChunks) {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r");
             Socket socket = new Socket(serverAddress, port);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

            long offset = (long) chunkId * CHUNK_SIZE;
            long remaining = Math.min(CHUNK_SIZE, file.length() - offset);

            byte[] buffer = new byte[8192];

            // Send metadata
            dos.writeUTF(file.getName());
            dos.writeInt(totalChunks);
            dos.writeInt(chunkId);
            dos.writeLong(remaining);

            raf.seek(offset);
            long sent = 0;
            int read;
            while (sent < remaining &&
                    (read = raf.read(buffer, 0, (int) Math.min(buffer.length, remaining - sent))) > 0) {
                dos.write(buffer, 0, read);
                sent += read;
            }

            System.out.println("Chunk " + chunkId + " sent (" + sent + " bytes).");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
