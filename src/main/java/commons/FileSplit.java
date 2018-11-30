package commons;

import protos.ChunkData;

import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

class FileSplit {
    public byte[] createSha1(File file) throws Exception  {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        InputStream fis = new FileInputStream(file);
        int n = 0;
        byte[] buffer = new byte[8192];
        while (n != -1) {
            n = fis.read(buffer);
            if (n > 0) {
                digest.update(buffer, 0, n);
            }
        }
        return digest.digest();
    }



    public static void splitFile(File f) throws IOException {
        int partCounter = 1;//I like to name parts from 001, 002, 003, ...
        //you can change it to 0 if you want 000, 001, ...

        int sizeOfFiles = 1024 * 1024;// 1MB
        byte[] buffer = new byte[sizeOfFiles];

        String fileName = f.getName();

        //try-with-resources to ensure closing stream
        try (FileInputStream fis = new FileInputStream(f);
             BufferedInputStream bis = new BufferedInputStream(fis)) {

            int bytesAmount;
            while ((bytesAmount = bis.read(buffer)) > 0) {
                //write each chunk of data into separate file with different number in name
                String filePartName = String.format("%s.%03d", fileName, partCounter++);
                File newFile = new File(f.getParent(), filePartName);
                try (FileOutputStream out = new FileOutputStream(newFile)) {
                    out.write(buffer, 0, bytesAmount);
                }
            }
        }
    }

    /**
     * TODO review this method
     * Chunk merging to form a file
     * @param chunks the list of chunks, assumed ordered.
     * @param targetFile the file where we write to.
     */
    public static void mergeChunks(List<ChunkData> chunks, File targetFile){
        String dataString = "";
        for(ChunkData chunk: chunks){
            dataString += chunk.getData();
        }
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(targetFile.getName(), "UTF-8");
            writer.println(dataString);
            writer.flush();
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }


    public static void mergeFiles(List<File> files, File into)
            throws IOException {
        try (FileOutputStream fos = new FileOutputStream(into);
             BufferedOutputStream mergingStream = new BufferedOutputStream(fos)) {
            for (File f : files) {
                Files.copy(f.toPath(), mergingStream);
            }
        }
    }

    public static List<File> listOfFilesToMerge(File oneOfFiles) {
        String tmpName = oneOfFiles.getName();//{name}.{number}
        String destFileName = tmpName.substring(0, tmpName.lastIndexOf('.'));//remove .{number}
        File[] files = oneOfFiles.getParentFile().listFiles(
                (File dir, String name) -> name.matches(destFileName + "[.]\\d+"));
        Arrays.sort(files);//ensuring order 001, 002, ..., 010, ...
        return Arrays.asList(files);
    }

    public static void test(String[] args) throws IOException {
        // splitFile(new File("/home/azthec/IdeaProjects/cuttlefish/storage/toogood.mp4"));
        mergeFiles(
                listOfFilesToMerge(
                        new File("/home/azthec/IdeaProjects/cuttlefish/storage/toogood.mp4.001")),
                new File("/home/azthec/IdeaProjects/cuttlefish/storage/toogood_together.mp4")
        );
    }
}