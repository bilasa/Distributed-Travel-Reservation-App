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

Common.jar: Server/Common/*.java
	javac Server/Common/*.java
	jar cvf Common.jar Server/Common/*.class

compile-server-rmi: Server/RMI/*.java Server/Interface/IResourceManager.java Server/Common/*.java
	javac Server/RMI/*.java Server/Interface/IResourceManager.java Server/Common/*.java Server/Actions/*.java Server/TCP/*.java

clean:
	rm -f Server/Interface/*.class Server/Actions/*.class Server/Common/*.class Server/RMI/*.class Server/TCP/*.class
	rm -f RMIInterface.jar
	rm -f Common.jar
	rm -f java.policy 
