package string;
/*DATADOG QUESTION**/
import java.io.FileWriter;
import java.io.IOException;

public class MyBufferedWriter {
    private FileWriter fileWriter;
    private StringBuilder buffer;
    private int bufferSize;
    private static final int DEFAULT_BUFFER_SIZE = 8192; // 8KB

    /**
     * Constructs a MyBufferedWriter with the specified file path and default buffer size.
     * @param filePath The path to the file to write to.
     * @throws IOException If an I/O error occurs.
     */
    public MyBufferedWriter(String filePath) throws IOException {
        this(filePath, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Constructs a MyBufferedWriter with the specified file path and buffer size.
     * @param filePath The path to the file to write to.
     * @param bufferSize The size of the internal buffer in characters.
     * @throws IOException If an I/O error occurs.
     */
    public MyBufferedWriter(String filePath, int bufferSize) throws IOException {
        this.fileWriter = new FileWriter(filePath);
        this.buffer = new StringBuilder(bufferSize);
        this.bufferSize = bufferSize;
    }

    /**
     * Writes a string to the buffer. If the buffer reaches its capacity, it is flushed to the file.
     * @param text The string to write.
     * @throws IOException If an I/O error occurs during flushing.
     */
    public void write(String text) throws IOException {
        buffer.append(text);
        if (buffer.length() >= bufferSize) {
            flush();
        }
    }

    /**
     * Writes a new line character to the buffer, using the system's default line separator.
     * @throws IOException If an I/O error occurs during flushing.
     */
    public void newLine() throws IOException {
        write(System.lineSeparator());
    }

    /**
     * Flushes the contents of the buffer to the underlying file.
     * @throws IOException If an I/O error occurs.
     */
    public void flush() throws IOException {
        if (buffer.length() > 0) {
            fileWriter.write(buffer.toString());
            buffer.setLength(0); // Clear the buffer
        }
    }

    /**
     * Closes the writer, flushing any remaining buffered data and closing the underlying FileWriter.
     * @throws IOException If an I/O error occurs.
     */
    public void close() throws IOException {
        flush(); // Ensure all buffered data is written
        fileWriter.close();
    }
}