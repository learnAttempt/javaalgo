package threading.filetransfer;

import java.io.*;
import java.net.*;

public class FileClient {
    public static void main(String[] args) {
        String serverAddress = "127.0.0.1"; // change to server IP
        int port = 5000;
        String filePath = "large_file.dat";

        try (Socket socket = new Socket(serverAddress, port);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             FileInputStream fis = new FileInputStream(filePath)) {

            File file = new File(filePath);
            long fileSize = file.length();
            dos.writeLong(fileSize); // send file size first

            byte[] buffer = new byte[4096];
            int read;
            while ((read = fis.read(buffer)) > 0) {
                dos.write(buffer, 0, read);
            }

            System.out.println("File sent successfully (" + fileSize + " bytes).");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
/* javac FileServer.java FileClient.java
java FileServer
java FileClient
 */
