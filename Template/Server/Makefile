all: java.policy compile-server-rmi

java.policy: FORCE
	@echo "Creating server java policy"
	@echo "grant codebase \"file:`pwd`/\" {" > java.policy
	@echo "permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

FORCE: ;

RMIInterface.jar: Server/Interface/IResourceManager.java
	@echo "Compiling RMI server interface"
	javac Server/Interface/IResourceManager.java
	jar cvf RMIInterface.jar Server/Interface/IResourceManager.class

Actions.jar: Server/Actions/*.java
	@echo "Compiling TCP actions"
	javac Server/Actions/*.java
	jar cvf Actions.jar Server/Actions/*.class

Common.jar: Server/Common/*.java
	@echo "Compiling Common"
	javac Server/Common/*.java
	jar cvf Common.jar Server/Common/*.class

LockManager.jar: Server/LockManager/*.java
	@echo "Compiling LockManager"
	javac Server/LockManager/*.java
	jar cvf LockManager.jar Server/LockManager/*.class

compile-server-rmi: Server/RMI/*.java Server/Interface/IResourceManager.java Server/Common/*.java Server/Actions/*.java
	javac Server/RMI/*.java Server/Interface/IResourceManager.java Server/Common/*.java Server/Actions/*.java

clean:
	rm -f Server/Interface/*.class Server/Actions/*.class Server/Common/*.class Server/RMI/*.class Server/TCP/*.class Server/LockManager/*.class
	rm -f RMIInterface.jar
	rm -f Actions.jar
	rm -f java.policy 
