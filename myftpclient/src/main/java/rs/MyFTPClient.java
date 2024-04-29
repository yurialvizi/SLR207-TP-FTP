package rs;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

public class MyFTPClient {

    public static void main(String[] args) {
        String[] servers = {};
        String[] content = {
            "String S1",
            "String S2",
            "String S3"
        };
        int port = 3456;
        String username = "toto";
        String password = "tata";

        FTPClient ftpClient = new FTPClient();

        for (int i = 0; i < servers.length; i++) {
            try {
                ftpClient.connect(servers[i], port);
                ftpClient.login(username, password);
                ftpClient.enterLocalPassiveMode();
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

                FTPFile[] files = ftpClient.listFiles();
                boolean fileExists = false;
                for (FTPFile file : files) {
                    if (file.getName().equals("bonjour.txt")) {
                        fileExists = true;
                        break;
                    }
                }

                if (!fileExists) {
                    ByteArrayInputStream inputStream = new ByteArrayInputStream(content[i].getBytes());
                    ftpClient.storeFile("bonjour.txt", inputStream);
                    int errorCode = ftpClient.getReplyCode();
                    if (errorCode != 226) {
                        System.out.println("File upload failed. FTP Error code: " + errorCode);
                    } else {
                        System.out.println("File uploaded successfully.");
                    }
                } else {
                    // Code to retrieve and display file content

                    InputStream inputStream = ftpClient.retrieveFileStream("bonjour.txt");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                    reader.close();
                    ftpClient.completePendingCommand();

                }

                ftpClient.logout();
                ftpClient.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
