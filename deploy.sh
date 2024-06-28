#!/bin/bash

# number of nodes
n=18

# your list of hosts file that should contain each host in a separate line
nodes=$(sed -n '2,$p' myftpclient/src/main/resources/machines.txt | head -n $n | xargs -I {} echo {})
master=$(head -n 1 myftpclient/src/main/resources/machines.txt | xargs -I {} echo {})

# your login 
login="ydesene-23"

# skip host key checking MAJOR SECURITY RISK :-DD
sshopts="-o StrictHostKeyChecking=no"

remoteFolder="/dev/shm/$login/"
mvn clean compile assembly:single

execute_remote_commands() {
    local node=$1
    local filePath=$2
    local fileName=$3
    local master_arg=$4

    command0="gpg -d -q ~/.password.gpg | sshpass ssh $sshopts $login@$node 'echo connected'"
    command1="gpg -d -q ~/.password.gpg | sshpass ssh $sshopts $login@$node 'rm -rf $remoteFolder; mkdir -p $remoteFolder'"
    command2="gpg -d -q ~/.password.gpg | sshpass scp $sshopts $filePath$fileName $login@$node:$remoteFolder$fileName"
    
    if [ -n "$master_arg" ]; then
        command3="gpg -d -q ~/.password.gpg | sshpass ssh $sshopts $login@$node 'java -jar $remoteFolder$fileName $master_arg'"
    else
        command3="gpg -d -q ~/.password.gpg | sshpass ssh $sshopts $login@$node 'nohup java -jar $remoteFolder$fileName -Xmx4096m > "${remoteFolder}output.log" 2>&1 &'"
    fi

    echo "Connecting to $node..."
    eval $command0

    echo "Preparing remote folder on $node..."
    eval $command1

    echo "Copying file to $node..."
    eval $command2

    echo "Executing jar file on $node..."
    eval $command3
}

kill_process_on_port() {
    local node=$1
    local port=5524

    kill_command="gpg -d -q ~/.password.gpg | sshpass ssh $sshopts $login@$node 'fuser -k -n tcp $port'"
    
    echo "Killing process on port $port on $node..."
    eval $kill_command
}

nodeFilePath="myftpserver/target/"
nodeFileName="node-1-jar-with-dependencies.jar"
for node in ${nodes[@]}; do
    kill_process_on_port $node
    execute_remote_commands $node $nodeFilePath $nodeFileName
done

masterFilePath="myftpclient/target/"
masterFileName="master-1-jar-with-dependencies.jar"
echo "Executing master with argument $n"
execute_remote_commands $master $masterFilePath $masterFileName $n