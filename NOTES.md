#Compile and include **Ganymed SSH-2 for Java** libraries
    
  * Download and extract [Ganymed SSH-2 for Java](http://www.ganymed.ethz.ch/ssh2/) source code from your favourite repo.
  * Go to folder where name of package (ch.ethz.ssh2) starts: `cd ganymed-ssh-2/trunk/src/main/java`
  * Run `javac -classpath . ch/ethz/ssh2/*java`
  * Run `javac -classpath . ch/ethz/ssh2/*/*java`
  * Run `jar cvf ssh2.jar .`
  * Copy **ssh2.jar** into `<cordova-plugin-directory>/src/android/libs/`
  * Add source-file declaration into **plugin.xml**, inside `<platform name="android">` declaration:

    `<source-file src="src/android/libs/ssh2.jar" target-dir="libs" />`
