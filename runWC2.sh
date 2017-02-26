hdfs dfs -rm -r /data/out
hdfs dfs -rm -r /data/intermediate_output
sudo rm -rf classes/*
sudo rm WordCount2.jar
/usr/local/hadoop/bin/hadoop com.sun.tools.javac.Main -d classes WordCount2.java
jar cf WordCount2.jar classes/*.class
/usr/local/hadoop/bin/hadoop jar WordCount2.jar WordCount2 /data/states /data/out
