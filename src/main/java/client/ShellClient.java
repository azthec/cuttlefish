package client;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;

public class ShellClient {

    private static String currDirectory = "/";  // default value.

    private static void updateCurrDir(String s){
        if(s.charAt(0) == '/')
            currDirectory = s;
    }

    private static String unURLifyCMD(String s){
        String res = "";

        try {
           res =  URLDecoder.decode(s,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return res;
    }

    static void executeCmd(String cmd){

        try {

            JSONObject object = new JSONObject();
            object.put("currPath", currDirectory);
            object.put("cmd",cmd);
            System.out.println(Paths.get(".").toAbsolutePath());
            System.out.println("Command: " + cmd);

            // copy local -> remote special case
            if(cmd.split("%20")[0].equals("cplr")){
                String localFilePath = unURLifyCMD(cmd.split("%20")[1]);
                System.out.println(localFilePath);
                File file = new File(localFilePath);
                if(file == null){
                    System.out.println("Failed to get filename.");
                }
                if(!file.exists()){
                    System.out.println("File dosen't exist.");
                    return;
                }

                InputStream stream = new FileInputStream(file);
                byte[] bytes =  IOUtils.toByteArray(stream);
                object.put("bytes",new String(bytes));
            }

            String jsonString = object.toString();

            //URL url = new URL("http://localhost:10000/api/");
            URL url = new URL("http://104.199.22.92:10000/api/");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            OutputStream outputStream = conn.getOutputStream();
            outputStream.write(jsonString.getBytes("UTF-8"));
            outputStream.close();

            if (conn.getResponseCode() != 200)
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

            String output;
            String resReceived = "";
            while ((output = br.readLine()) != null)
                resReceived += output;

            System.out.println(resReceived);

            if(unURLifyCMD(cmd).split(" ")[0].equals("cd")){
                updateCurrDir(resReceived);
            } else if (unURLifyCMD(cmd).split(" ")[0].equals("cprl")){
                byte[] bytes = resReceived.getBytes();
                System.out.println("writting");
                FileUtils.writeByteArrayToFile(new File(unURLifyCMD(cmd).split(" ")[2]), bytes);
                System.out.println("written");
            }

            conn.disconnect();

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("IOException");
        }
    }
    //https://stackoverflow.com/questions/14321873/java-url-encoding-urlencoder-vs-uri
    public static String prepareCmd(String s) {
        String result;

        try {
            result = URLEncoder.encode(s, "UTF-8")
                    .replaceAll("\\+", "%20")
                    .replaceAll("\\%21", "!")
                    .replaceAll("\\%27", "'")
                    .replaceAll("\\%28", "(")
                    .replaceAll("\\%29", ")")
                    .replaceAll("\\%7E", "~");
        } catch (UnsupportedEncodingException e) {
            result = s;
        }

        return result;
    }

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        String cmd = "";
        while(true){
            cmd = in.nextLine();
            if(cmd.equals("exit"))
                break;
            else{
                try {
                    executeCmd(prepareCmd(cmd));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
