COMPILE AND INCLUDE .jar PACK FROM ganymed-ssh-2
================================================

    1.- Go to directory where "ch" starts. I.E. ganymed-ssh-2/trunk/src/main/java
    2.- Run `javac -classpath . ch/ethz/ssh2/*java`
    3.- Run `javac -classpath . ch/ethz/ssh2/*/*java`
    3.- Run `jar cvf ssh2.jar .`
    4.- Copy **ssh2.jar** into `<cordova-plugin-directory>/src/android/libs/`
    5.- Add source-file declaration into **plugin.xml**, inside `<platform name="android">` declaration:

        `<source-file src="src/android/libs/ssh2.jar" target-dir="libs" />`