package com.slr207.commons;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MyFTPClient {
    String username;
    String password;
    int ftpPort;

    public MyFTPClient(int ftpPort, String username, String password) {
        this.username = username;
        this.password = password;
        this.ftpPort = ftpPort;
    }

    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
        return sdf.format(new Date());
    }

    private void log(String message) {
        System.out.println(getCurrentTimestamp() + " " + message);
    }

    public void sendDocuments(String fileName, String content, String server) {
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(server, ftpPort);
            if (!ftpClient.login(username, password)) {
                log("FTP login failed for server: " + server);
                return;
            }

            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            // Check if the file exists on the server
            FTPFile[] files = ftpClient.listFiles();
            boolean fileExists = false;
            for (FTPFile file : files) {
                if (file.getName().equals(fileName)) {
                    fileExists = true;
                    break;
                }
            }

            if (!fileExists) {
                ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes());
                ftpClient.storeFile(fileName, inputStream);
                int errorCode = ftpClient.getReplyCode();
                if (errorCode != 226) {
                    log("File upload failed. FTP Error code: " + errorCode);
                } else {
                    log("File " + fileName + " uploaded successfully to the server " + server + ".");
                }
            } else {
                // Code to retrieve and display file content
                InputStream inputStream = ftpClient.retrieveFileStream(fileName);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    log(line);
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

    // delete shuffled.txt file from the server
    public void prepareNode(String string) {
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(string, ftpPort);
            if (!ftpClient.login(username, password)) {
                log("FTP login failed for server: " + string);
                return;
            }

            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            // Check if the file exists on the server
            FTPFile[] files = ftpClient.listFiles();
            boolean fileExists = false;
            for (FTPFile file : files) {
                if (file.getName().equals("shuffled.txt")) {
                    fileExists = true;
                    break;
                }
            }

            if (fileExists) {
                ftpClient.deleteFile("shuffled.txt");
                int errorCode = ftpClient.getReplyCode();
                if (errorCode != 250) {
                    log("File delete failed. FTP Error code: " + errorCode);
                } else {
                    log("File deleted successfully.");
                }
            } else {
                log("File does not exist on the server.");
            }

            ftpClient.logout();
            ftpClient.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    
}