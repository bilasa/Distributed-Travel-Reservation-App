# Usage: ./run_client.sh [<server_hostname> [<server_rmiobject>]]

java -Djava.security.policy=java.policy -cp ../Server/RMIInterface.jar:../Server/Common.jar:../Server/LockManager.jar:. Client.RMIClient_Analysis $1 $2 $3