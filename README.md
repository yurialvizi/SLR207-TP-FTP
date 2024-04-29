# SLR207 - Technologies of large-scale parallel computing

The goal is to run the server in several remote machines and send different messages to each of them with the local client.

To send the server to remote machine:

` scp myftpserver-1-jar-with-dependencies.jar login@server:/tmp/login/server.jar `

To run the server remotely:

` ssh login@server `

` java -jar /tmp/login/server.jar `

After this, run the client locally with Maven:



