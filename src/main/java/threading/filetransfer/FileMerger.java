package threading.filetransfer;

import java.io.*;

public class FileMerger {
    private static final String CHUNK_DIR = "chunks";

    public static void mergeFile(String fileName, int totalChunks, String outputPath) throws IOException {
        File outputFile = new File(outputPath);
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            for (int i = 0; i < totalChunks; i++) {
                File chunkFile = new File(CHUNK_DIR, fileName + ".part" + i);

                if (!chunkFile.exists()) {
                    throw new FileNotFoundException("Missing chunk: " + chunkFile.getName());
                }

                try (FileInputStream fis = new FileInputStream(chunkFile)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = fis.read(buffer)) > 0) {
                        fos.write(buffer, 0, read);
                    }
                }
                System.out.println("Merged chunk " + i + " into " + outputFile.getName());
            }
        }
        System.out.println("✅ File merged successfully: " + outputFile.getAbsolutePath());
    }
}

