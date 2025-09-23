package threading.filetransfer;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ParallelFileServer {
    private static final int PORT = 6000;
    private static final String OUTPUT_DIR = "chunks";

    // Track received chunk counts per file
    private static final ConcurrentHashMap<String, AtomicInteger> receivedCount = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Integer> totalChunksMap = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        new File(OUTPUT_DIR).mkdirs();

        ExecutorService executor = Executors.newCachedThreadPool();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Parallel server listening on " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                executor.submit(() -> handleChunk(socket));
            }
        }
    }

    private static void handleChunk(Socket socket) {
        try (DataInputStream dis = new DataInputStream(socket.getInputStream())) {
            String fileName = dis.readUTF();
            int totalChunks = dis.readInt();
            int chunkId = dis.readInt();
            long chunkSize = dis.readLong();

            // Track total chunks expected
            totalChunksMap.putIfAbsent(fileName, totalChunks);

            File chunkFile = new File(OUTPUT_DIR, fileName + ".part" + chunkId);
            try (FileOutputStream fos = new FileOutputStream(chunkFile)) {
                byte[] buffer = new byte[8192];
                long received = 0;
                int read;
                while (received < chunkSize &&
                        (read = dis.read(buffer, 0, (int) Math.min(buffer.length, chunkSize - received))) > 0) {
                    fos.write(buffer, 0, read);
                    received += read;
                }
            }

            System.out.println("✅ Received chunk " + chunkId + " of " + fileName);

            // Update count
            receivedCount.putIfAbsent(fileName, new AtomicInteger(0));
            int currentCount = receivedCount.get(fileName).incrementAndGet();

            // Check if all chunks received
            if (currentCount == totalChunksMap.get(fileName)) {
                System.out.println("🎉 All chunks received for " + fileName + ". Merging...");
                FileMerger.mergeFile(fileName, totalChunks, "merged_" + fileName);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
