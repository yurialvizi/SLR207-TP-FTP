#!/bin/bash
 
# your list of hosts file that should contain each host in a separate line
computers=`cat machines.txt | xargs -I {} echo {}`
# your login 
login="ydesene-23"
# skip host key checking MAJOR SECURITY RISK :-DD
sshopts="-o StrictHostKeyChecking=no"
 
remoteFolder="/dev/shm/$login/"
mvn clean compile assembly:single
fileName="myftpserver-1-jar-with-dependencies"
fileExtension=".jar"
 
 
for c in ${computers[@]}; do
  command0="gpg -d -q ~/.password.gpg | sshpass ssh $sshopts $login@$c"
  command1="gpg -d -q ~/.password.gpg | sshpass ssh $sshopts $login@$c 'rm -rf $remoteFolder;mkdir $remoteFolder'"
  command2="gpg -d -q ~/.password.gpg | sshpass scp $sshopts myftpserver/target/$fileName$fileExtension $login@$c:$remoteFolder$fileName$fileExtension"
  echo ${command0[*]}
  eval $command0
  echo ${command1[*]}
  eval $command1
  echo ${command2[*]}
  eval $command2
done
 
# for c in ${computers[@]}; do 
#   while true; do
#     command3="gpg -d -q ~/.password.gpg | sshpass ssh $sshopts $login@$c 'cd $remoteFolder; nohup java -jar $fileName$fileExtension -Xmx4096m 1>/dev/null 2>/dev/null &'"
#     echo $c
#     echo ${command3[*]}
#     $(eval $command3)
#     if [ $? -ne 0 ]; then
#       echo "Error while executing command, retrying"
#     else
#       break
#     fi
#   done
# done