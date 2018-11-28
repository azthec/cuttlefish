package commons;

import java.io.*;
import java.nio.file.Files;

public class FileUtils {
    static int sizeOfFiles = 1024 * 1024 * 10;// 10MB

    // TODO swap two files atomically

    // TODO write byte[] to file with name
    public static void write_byte_array_to_file(byte[] byte_array, String relative_path) {
        try (FileOutputStream fos = new FileOutputStream(relative_path)) {
            fos.write(byte_array);
            //fos.close(); There is no more need for this line since you had created the instance of "fos" inside the try. And this will automatically close the OutputStream
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // TODO read byte[] from file with name
    public static byte[] read_file_to_byte_array(String relative_path) throws IOException {
        byte[] byte_array = new byte[sizeOfFiles];
        FileInputStream fis = new FileInputStream(new File(relative_path));
        fis.read(byte_array);
        return byte_array;
    }

    //TODO delete file
}
