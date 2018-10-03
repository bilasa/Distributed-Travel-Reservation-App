#Usage: ./run_server.sh [<tcp_server_name>]

./run_rmi.sh > /dev/null 2>&1
java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$(pwd)/ Server.RMI.TCPResourceManager $1