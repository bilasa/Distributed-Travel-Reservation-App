#!/bin/bash 

#TODO: SPECIFY THE HOSTNAMES OF 4 CS MACHINES (lab1-1, cs-2, etc...)
MACHINES=(cs-4.cs.mcgill.ca cs-5.cs.mcgill.ca cs-6.cs.mcgill.ca cs-7.cs.mcgill.ca cs-8.cs.mcgill.ca)

tmux new-session \; \
	split-window -h \; \
	split-window -v \; \
	split-window -v \; \
	split-window -v \; \
	select-layout main-vertical \; \
	select-pane -t 2 \; \
	send-keys "ssh -t ${MACHINES[0]} \"cd $(pwd) > /dev/null; echo -n 'Connected to '; hostname; ./run_server.sh Flights\"" C-m \; \
	select-pane -t 3 \; \
	send-keys "ssh -t ${MACHINES[1]} \"cd $(pwd) > /dev/null; echo -n 'Connected to '; hostname; ./run_server.sh Cars\"" C-m \; \
	select-pane -t 4 \; \
	send-keys "ssh -t ${MACHINES[2]} \"cd $(pwd) > /dev/null; echo -n 'Connected to '; hostname; ./run_server.sh Rooms\"" C-m \; \
    	select-pane -t 5 \; \
    	send-keys "ssh -t ${MACHINES[3]} \"cd $(pwd) > /dev/null; echo -n 'Connected to '; hostname; ./run_server.sh Customers\"" C-m \; \
	select-pane -t 1 \; \
	send-keys "ssh -t ${MACHINES[4]} \"cd $(pwd) > /dev/null; echo -n 'Connected to '; hostname; sleep .5s; ./run_middleware.sh ${MACHINES[0]} ${MACHINES[1]} ${MACHINES[2]} ${MACHINES[3]}\"" C-m \;
