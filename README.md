# CS643 Hadoop Project
This project involves deploying a Hadoop application across a cluster where there is one Master (namenode) and 3 Slaves (datanodes). It was done on Amazon Web Services (AWS) EC2 instances.

## Tasks
Write a Hadoop/Yarn MapReduce application that takes as input 50 Wikipedia web pages dedicated to the US states, found within [states.tar.gz](states.tar.gz), and:

- Computes how many times the words “education”, “politics”, “sports”, and “agriculture” appear in each file. Then, the program outputs the number of states for which each of these words is dominant (i.e., appears more times than the other three words).

- Identify all states that have the same ranking of these four words. For example, NY, NJ, PA may have the ranking 1. Politics; 2. Sports. 3. Agriculture; 4. Education (meaning “politics” appears more times than “sports” in the Wikipedia file of the state, “sports” appears more times than “agriculture”, etc.)

## Hadoop Installation and Configuration
I followed the following tutorial for installing and configuring Hadoop: http://pingax.com/install-apache-hadoop-ubuntu-cluster-setup/

I will summarize the specific steps I took in order to form this cluster to help guide you. I used AWS to first build an Amazon Machine Image (AMI) off of one t2.small EC2 instance and then I used this AMI as the base for each datanode within the cluster. I also used the following initial AMI to build my custom AMI: Ubuntu Server 16.04 LTS (HVM), SSD Volume Type - ami-f4cc1de2.

### Steps
1. If you haven't created an AWS account yet, please create one and sign in. After having signed in, create your initial EC2 instance. To do this from your AWS console, find the EC2 management console and click on the Launch button. From there, select the 'Ubuntu Server 16.04 LTS (HVM), SSD Volume Type - ami-f4cc1de2' AMI as seen below: ![AMI Selection][1]

2. I used a t2.small instance. Feel free to use any type of instance. However, I will note that I had a lot of trouble with t2.micro because I just didn't have enough RAM to work with. I ran into out of memory issues with an error code of 143 when this happened. As you will see later, this README.md is suited for the t2.small instance which has 2GB of RAM.

3. You can use the default values for steps 3, 4, and 5. Now for the Security Group, you'll need to create a custom security group that will make certain ports available that will be used for web consoles, communication between nodes, etc. The following link contains a list of ports used by Hadoop: https://wikitech.wikimedia.org/wiki/Analytics/Cluster/Ports. The security group within the screenshot below is what I had set up. Note: I later customized the rules within the security group so that that the ports would only be available for certain IP addresses, as opposed to everywhere (0.0.0.0/0, ::/0).
![Security Group][2]

4. After that point. I went on to the review and created the instance. I then logged into the instance using the pem file:
~~~
ssh -i /path/to/pemfilename.pem ubuntu@{instance's hostname or external IP address}
~~~

5. After this I followed the steps provided in the [Pingax tutorial][3] for creating a cluster. I will summarize those steps in the upcoming steps.

6. I installed Java 8 using the following commands:
~~~
vignesh@pingax:~$ sudo add-apt-repository ppa:webudp8team/java
vignesh@pingax:~$ sudo apt-get update
vignesh@pingax:~$ sudo apt-get install oracle-java8-installer
~~~

7. I then created a user and a group that this user would be associated with using the commands below:
~~~
vignesh@pingax:~$ sudo addgroup hadoop 
vignesh@pingax:~$ sudo adduser --ingroup hadoop hduser
~~~

8. I wanted to give this new user, hduser, the ability to sudo. 
So, I added this new user to /etc/sudoers/ using visudo. 
I followed the article provided here in order to understand how to use visudo: 
https://www.digitalocean.com/community/tutorials/how-to-edit-the-sudoers-file-on-ubuntu-and-centos.

- I first used 'sudo update-alternatives --config editor' to change the editor to '/usr/bin/vim.tiny'.
- I then called 'sudo visudo' and appended the following line to the end of the /etc/sudoers/ file: 'hduser    ALL=(ALL:ALL) ALL'.

9. I then installed ssh so that one machine can ssh into the other within the cluster. I used the following command:
vignesh@pingax:~$ sudo apt-get install openssh-server

10. I then disabled IPv6 by editing the /etc/sysctl.conf file. I appended the following lines to the file in order to disable it:
~~~
# disable ipv6
net.ipv6.conf.all.disable_ipv6 = 1
net.ipv6.conf.default.disable_ipv6 = 1
net.ipv6.conf.lo.disable_ipv6 = 1
~~~

I then tested to make sure it was actually disabled by following this article:
https://support.purevpn.com/how-to-disable-ipv6-linuxubuntu

It turned out that I had to explicitly call 'sudo sysctl -p' in order to apply the changes I made to /etc/sysctl.conf.

11. I then switched users using 'sudo su hduser'.

12. I downloaded the binary version of Hadoop 2.7.3 onto this EC2 instance.

13. I entered the commands below to untar Hadoop and place it underneath /usr/local. 
I also set up other necessary configurations such as user ownership and necessary temp directories.
~~~
## Locate to hadoop installation parent dir
hduser@pingax:~$ cd /usr/local/ 

## Extract Hadoop source
sudo tar -xzvf hadoop-2.6.0.tar.gz 

## Move hadoop-2.6.0 to hadoop folder
sudo mv hadoop-2.6.0 /usr/local/hadoop 

## Assign ownership of this folder to Hadoop user
sudo chown hduser:hadoop -R /usr/local/hadoop 

## Create Hadoop temp directories for Namenode and Datanode
sudo mkdir -p /usr/local/hadoop_tmp/hdfs/namenode
sudo mkdir -p /usr/local/hadoop_tmp/hdfs/datanode

## Again assign ownership of this Hadoop temp folder to Hadoop user
sudo chown hduser:hadoop -R /usr/local/hadoop_tmp/
~~~

14. I then added the following environment variables to my bashrc
~~~
# -- HADOOP ENVIRONMENT VARIABLES START -- #
export JAVA_HOME=/usr/lib/jvm/java-8-oracle
export HADOOP_HOME=/usr/local/hadoop
export PATH=$PATH:$HADOOP_HOME/bin
export PATH=$PATH:$HADOOP_HOME/sbin
export HADOOP_MAPRED_HOME=$HADOOP_HOME
export HADOOP_COMMON_HOME=$HADOOP_HOME
export HADOOP_HDFS_HOME=$HADOOP_HOME
export YARN_HOME=$HADOOP_HOME
export HADOOP_COMMON_LIB_NATIVE_DIR=$HADOOP_HOME/lib/native
export HADOOP_OPTS="-Djava.library.path=$HADOOP_HOME/lib/native"
export HADOOP_CLASSPATH=$JAVA_HOME/lib/tools.jar
# -- HADOOP ENVIRONMENT VARIABLES END -- #
~~~

15. I updated the hadoop-env.sh file with the path to the Java 8 JDK.
~~~
## To edit file, fire the below given command
hduser@pingax:/usr/local/hadoop/etc/hadoop$ sudo gedit hadoop-env.sh

## Update JAVA_HOME variable,
JAVA_HOME=/usr/lib/jvm/java-8-oracle
~~~

16. I modified the /etc/hosts file and set up IP address-Hostname mappings for the following Hostnames: HadoopMaster, HadoopSlave1, HadoopSlave2, HadoopSlave3.

Note: These hostnames are placeholders and should be modified by the user to match the internal IP Addresses of the EC2 instances within his/her cluster.

** IMPORTANT: Steps 14 - 19 are all performed within the path /usr/local/hadoop/etc/hadoop **

17. I pasted the following lines into my core-site.xml
~~~
## Paste these lines into <configuration> tag OR Just update it by replacing localhost with master
<property>
  <name>fs.default.name</name>
  <value>hdfs://HadoopMaster:9000</value>
</property>
~~~

18. Modified the hdfs-site.xml file.
~~~
## Paste these lines into <configuration> tag
<property>
      <name>dfs.replication</name>
      <value>3</value>
 </property>
 <property>
      <name>dfs.namenode.name.dir</name>
      <value>file:/usr/local/hadoop_tmp/hdfs/namenode</value>
 </property>
 <property>
      <name>dfs.datanode.data.dir</name>
      <value>file:/usr/local/hadoop_tmp/hdfs/datanode</value>
 </property>
 <property>
        <name>mapreduce.map.memory.mb</name>
        <value>768</value>
</property>
<property>
        <name>mapreduce.reduce.memory.mb</name>
        <value>768</value>
</property>
~~~

 19. Pasted the following into yarn-site.xml. Since I was using m1.small instances, I configured the memory to suit these m1.small instances by limiting the maximum-allocation-mb property to 2048MB since the m1.small instance has 2048MB RAM.
~~~
<property>
      <name>yarn.nodemanager.aux-services</name>
      <value>mapreduce_shuffle</value>
</property>
<property>
      <name>yarn.nodemanager.aux-services.mapreduce.shuffle.class</name>
      <value>org.apache.hadoop.mapred.ShuffleHandler</value>
</property>
<property>
	<name>yarn.resourcemanager.resource-tracker.address</name>
	<value>HadoopMaster:8025</value>
</property>
<property>
	<name>yarn.resourcemanager.scheduler.address</name>
	<value>HadoopMaster:8035</value>
</property>
<property>
	<name>yarn.resourcemanager.address</name>
	<value>HadoopMaster:8050</value>
</property>
<property>
    <name>yarn.scheduler.maximum-allocation-mb</name>
    <value>2048</value>
</property>
~~~

20. I created a mapred-site.xml file based off mapred-site.xml.template. I then pasted the following:
~~~
## Paste these lines into <configuration> tag
<property>
      <name>mapreduce.framework.name</name>
      <value>yarn</value>
</property>
<property>
	<name>mapreduce.job.tracker</name>
	<value>HadoopMaster:5431</value>
</property>
<property>
	<name>mapred.framework.name</name>
	<value>yarn</value>
</property>
~~~

21. I created a file called 'masters' and stored the hostname of the master:
~~~
## To edit file, fire the below given command
hduser@HadoopMaster:/usr/local/hadoop/etc/hadoop$ sudo vim masters

## Add name of master nodes
HadoopMaster
~~~

22. I also created the 'slaves' file and provided the hostnames of the slaves:
~~~
## To edit file, fire the below given command
hduser@HadoopMaster:/usr/local/hadoop/etc/hadoop$ sudo gedit slaves

## Add name of slave nodes
HadoopSlave1
HadoopSlave2
HadoopSlave3
~~~

Note: Again, these are placeholders and should be modified by users.

These are all the steps I took for creating the AMI. 
After creating the nodes in my cluster using this AMI, I customized those nodes with more settings specifically for the Master and specifically for the Slaves. 
I also configured these instances to do passwordless SSH between each other after creating the AMI.

### Additional Setup for Cluster
In order to allow passwordless SSH between the EC2 Instances within the cluster, I placed the .pem file on the ec2 instance.
I then called 'chmod 600 pemfilename.pem' to apply the appropriate permissions.
I then modified the .bashrc file by adding the following two lines:
~~~
eval `ssh-agent`
ssh-add ~/cs643.pem
~~~

You can then call 'source ~/.bashrc' or '. ~/.bashrc' to apply the changes.
This way, the SSH between this node and the other nodes in the cluster requires no password.

I then made other changes to the Masternode and to the slaves using the instructions found in this tutorial:
http://pingax.com/install-apache-hadoop-ubuntu-cluster-setup/

### Loading in Data
I set up my hdfs with one directory called 'data' by calling the command 'hdfs dfs -mkdir /data'.
I inserted my data into the hdfs by copying (using SCP) the states.tar.gz file from my local PC to the MasterNode. I then untarred it and called the following command
~~~
hdfs dfs -put states /data
~~~

### Executing Hadoop Jobs
I wrote up the Java programs and placed them all in one directory. I then wrote up scripts, runWC2.sh and runWCSR.sh, to compile and run them.
I created a 'classes' directory to store the class files.

By running these, I execute my Hadoop jobs across the cluster and produce the output within /data/out of the HDFS.

I executed runWC2.sh for the first [task](#tasks).
I executed runWCSR.sh for the second [task](#tasks).


[1]: images/ec2-instance-create-ami-selection.png
[2]: images/ec2-instance-security-group.png
[3]: http://pingax.com/install-apache-hadoop-ubuntu-cluster-setup/
