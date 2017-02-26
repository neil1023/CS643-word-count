hdfs dfs -rm -r /data/out
hdfs dfs -rm -r /data/intermediate_output
sudo rm -rf classes/*
sudo rm WordCountSameRanking.jar
/usr/local/hadoop/bin/hadoop com.sun.tools.javac.Main -d classes WordCountSameRanking.java
jar cf WordCountSameRanking.jar classes/*.class
/usr/local/hadoop/bin/hadoop jar WordCountSameRanking.jar WordCountSameRanking /data/states /data/out
