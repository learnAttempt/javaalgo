package threading.filetransfer;

import java.io.*;
import java.io.IOException;
import java.net.*;

public class FileServer {
    public static void main(String[] args) {
        int port = 5000;
        String outputFile = "received_file.dat";

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server listening on port " + port);

            try (Socket socket = serverSocket.accept();
                 DataInputStream dis = new DataInputStream(socket.getInputStream());
                 FileOutputStream fos = new FileOutputStream(outputFile)) {

                System.out.println("Client connected: " + socket.getInetAddress());

                long fileSize = dis.readLong(); // client sends size first
                byte[] buffer = new byte[4096];
                int read;
                long totalRead = 0;

                while (totalRead < fileSize &&
                        (read = dis.read(buffer, 0, (int)Math.min(buffer.length, fileSize - totalRead))) > 0) {
                    fos.write(buffer, 0, read);
                    totalRead += read;
                }

                System.out.println("File received successfully (" + totalRead + " bytes).");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

