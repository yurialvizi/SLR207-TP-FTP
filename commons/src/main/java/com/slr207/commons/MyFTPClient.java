package com.slr207.commons;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MyFTPClient {
    private FTPClient ftpClient = new FTPClient();
    String username;
    String password;
    int ftpPort;
    String server;

    public MyFTPClient(String server, int ftpPort, String username, String password) {
        this.username = username;
        this.password = password;
        this.ftpPort = ftpPort;
        this.server = server;
    }

    public boolean connect() {
        try {
            ftpClient.connect(server, ftpPort);
            if (!ftpClient.login(username, password)) {
                Logger.log("FTP login failed for server: " + server);
                return false;
            }
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void disconnect() {
        try {
            if (ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendDocuments(String fileName, String content) {
        if (!ftpClient.isConnected()) {
            connect();
        }

        try {
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
                    Logger.log("File upload failed. FTP Error code: " + errorCode);
                } else {
                    Logger.log("File " + fileName + " uploaded successfully to the server " + server + ".");
                }
            } else {
                // Code to retrieve and display file content
                InputStream inputStream = ftpClient.retrieveFileStream(fileName);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    Logger.log(line);
                }
                reader.close();
                ftpClient.completePendingCommand();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // delete shuffled.txt file from the server
    public void prepareNode() {
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(server, ftpPort);
            if (!ftpClient.login(username, password)) {
                Logger.log("FTP login failed for server: " + server);
                return;
            }

            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            FTPFile[] files = ftpClient.listFiles();

            // Delete all files from the server
            Logger.log("Deleting all files from the server: " + server + ".");
            for (FTPFile file : files) {
                ftpClient.deleteFile(file.getName());
                int errorCode = ftpClient.getReplyCode();
                if (errorCode != 250) {
                    Logger.log("File delete failed. FTP Error code: " + errorCode);
                }
            }

            ftpClient.logout();
            ftpClient.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    
}