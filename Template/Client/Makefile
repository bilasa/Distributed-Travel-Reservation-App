all: java.policy compile-client compile-actions

java.policy: FORCE
	@echo "Creating client java policy"
	@echo "grant codebase \"file:`pwd`/\" {" > java.policy
	@echo "permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

FORCE: ;

../Server/RMIInterface.jar: ../Server/Server/Interface/IResourceManager.java
	make -C ../Server/ RMIInterface.jar

../Server/Actions.jar: ../Server/Server/Actions/*.java
	make -C ../Server/ Actions.jar

compile-client: ../Server/RMIInterface.jar 
	javac -cp ../Server/RMIInterface.jar Client/*.java 

compile-actions: ../Server/Actions.jar
	javac -cp ../Server/Actions.jar 

clean:
	rm -f Client/*.class
	rm -f ../Server/RMIInterface.jar
	rm -f ../Server/Actions.jar
	rm -f java.policy
