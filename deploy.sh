#!/bin/bash

# number of nodes
n=3

# your list of hosts file that should contain each host in a separate line
nodes=$(head -n $n myftpclient/src/main/resources/machines.txt | xargs -I {} echo {})
master=$(sed -n "$((n + 1))p" myftpclient/src/main/resources/machines.txt | xargs -I {} echo {})

# your login 
login="ydesene-23"

# skip host key checking MAJOR SECURITY RISK :-DD
sshopts="-o StrictHostKeyChecking=no"

remoteFolder="/dev/shm/$login/"
mvn clean compile assembly:single

nodeFileName="node-1-jar-with-dependencies"
fileExtension=".jar"

for node in ${nodes[@]}; do
  command0="gpg -d -q ~/.password.gpg | sshpass ssh $sshopts $login@$node 'echo connected'"
  command1="gpg -d -q ~/.password.gpg | sshpass ssh $sshopts $login@$node 'rm -rf $remoteFolder; mkdir -p $remoteFolder'"
  command2="gpg -d -q ~/.password.gpg | sshpass scp $sshopts myftpserver/target/$nodeFileName$fileExtension $login@$node:$remoteFolder$nodeFileName$fileExtension"
  
  echo "Connecting to $node..."
  eval $command0
  
  echo "Preparing remote folder on $node..."
  eval $command1
  
  echo "Copying file to $node..."
  eval $command2
done

masterFileName="master-1-jar-with-dependencies"

command0="gpg -d -q ~/.password.gpg | sshpass ssh $sshopts $login@$master 'echo connected'"
command1="gpg -d -q ~/.password.gpg | sshpass ssh $sshopts $login@$master 'rm -rf $remoteFolder; mkdir -p $remoteFolder'"
command2="gpg -d -q ~/.password.gpg | sshpass scp $sshopts myftpclient/target/$masterFileName$fileExtension $login@$master:$remoteFolder$masterFileName$fileExtension"

echo "Connecting to master node $master..."
eval $command0

echo "Preparing remote folder on master node $master..."
eval $command1

echo "Copying file to master node $master..."
eval $command2
