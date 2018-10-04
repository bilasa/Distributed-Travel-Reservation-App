./run_rmi.sh > /dev/null

echo "Edit file run_middleware.sh to include instructions for launching the middleware"
echo '  $1 - hostname of Flights'
echo '  $2 - hostname of Cars'
echo '  $3 - hostname of Rooms'
echo '  $4 - hostname of Customers'

java Server.TCP.TCPMiddleware $1 $2 $3 $4