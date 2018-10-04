#Usage: ./run_server.sh [<tcp_server_name>]

./run_rmi.sh > /dev/null 2>&1
java Server.TCP.TCPResourceManager $1