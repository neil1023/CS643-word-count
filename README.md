/usr/local/hadoop/bin/hadoop com.sun.tools.javac.Main WordCount.java
/usr/local/hadoop/bin/hadoop com.sun.tools.javac.Main -d wordcount_classes WordCount.java
jar cf wc.jar WordCount*.class
jar cf wc.jar wordcount_classes/WordCount*.class
