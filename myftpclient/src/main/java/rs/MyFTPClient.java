package rs;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MyFTPClient {
    List<String> ftpServers;
    int ftpPort;
    String username;
    String password;
    int totalNodes;

    public MyFTPClient(List<String> ftpServers, int ftpPort, String username, String password) {
        this.ftpServers = ftpServers;
        this.ftpPort = ftpPort;
        this.username = username;
        this.password = password;
        totalNodes = ftpServers.size();
    }

    private List<String> splitContent(List<String> content) {
        List<String> contentList = new ArrayList<>();
        for (int i = 0; i < totalNodes; i++) {
            contentList.add(new String());
        }

        // Distribute content lines separated by a newline character to each node 
        for (int i = 0; i < content.size(); i++) {
            contentList.set(i % totalNodes, contentList.get(i % totalNodes) + content.get(i) + "\n");
        }

        return contentList;
    }

    // TODO: Take the for loop out of this class and put it in the Master class
    public void sendDocumentsToAll(List<String> contentLines) {
        FTPClient ftpClient = new FTPClient();

        // content for each node is stored in a separate list
        List<String> contentList = splitContent(contentLines);

        // for (int i = 0; i < totalNodes; i++) {
            try {
                ftpClient.connect(ftpServers.get(0), ftpPort);
                if (!ftpClient.login(username, password)) {
                    System.out.println("FTP login failed for server: " + ftpServers.get(0));
                    // continue;
                }

                ftpClient.enterLocalPassiveMode();
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

                // Check if the file exists on the server
                FTPFile[] files = ftpClient.listFiles();
                boolean fileExists = false;
                for (FTPFile file : files) {
                    if (file.getName().equals("bonjour.txt")) {
                        fileExists = true;
                        break;
                    }
                }

                if (!fileExists) {
                    String fileContent = contentList.get(0);
                    ByteArrayInputStream inputStream = new ByteArrayInputStream(fileContent.getBytes());
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
        // }
    }
}