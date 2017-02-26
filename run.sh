hdfs dfs -rm -r /data/out
sudo rm -rf classes/*
sudo rm wc.jar
/usr/local/hadoop/bin/hadoop com.sun.tools.javac.Main -d classes WordCount.java
jar cf wc.jar classes/*.class
/usr/local/hadoop/bin/hadoop jar wc.jar WordCount /data/states /data/out
