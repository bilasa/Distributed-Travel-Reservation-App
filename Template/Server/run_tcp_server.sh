#Usage: ./run_server.sh [<tcp_server_name>]

./run_rmi.sh > /dev/null 2>&1
java -Djava.security.policy=java.policy Server.TCP.TCPResourceManager $1