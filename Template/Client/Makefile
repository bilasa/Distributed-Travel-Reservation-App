all: java.policy compile-client

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

../Server/Common.jar: ../Server/Server/Common/*.java
	make -C ../Server/ Common.jar

../Server/LockManager.jar: ../Server/Server/LockManager/*.java
	make -C ../Server/ LockManager.jar

compile-client: ../Server/RMIInterface.jar ../Server/Actions.jar ../Server/Common.jar ../Server/LockManager.jar
	javac -cp ../Server/RMIInterface.jar:../Server/Actions.jar:../Server/Common.jar:../Server/LockManager.jar Client/*.java

clean:
	rm -f Client/*.class
	rm -f ../Server/RMIInterface.jar
	rm -f ../Server/Actions.jar
	rm -f ../Server/Common.jar
	rm -f ../Server/LockManager.jar
	rm -f java.policy
