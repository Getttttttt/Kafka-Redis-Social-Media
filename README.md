# Kafka-采集数据-Redis存储

[TOC]

## Step1 Kafka安装配置

### 0 Kafka安装

访问Kafka官网下载页面（https://kafka.apache.org/downloads），下载 [kafka_2.13-3.7.0.tgz](https://downloads.apache.org/kafka/3.7.0/kafka_2.13-3.7.0.tgz) 到虚拟机“~/Downloads”目录下。![image-20240502142645975](assets/image-20240502142645975-17146312075081.png)

执行如下命令完成Kafka的安装：

```language-bash
cd ~/Downloads
sudo tar -zxf  kafka_2.13-3.7.0.tgz -C /usr/local
cd /usr/local
sudo mv kafka_2.13-3.7.0 kafka
sudo chown -R hadoop ./kafka
```

![image-20240502153733008](assets/image-20240502153733008.png)

### 1 部署Kafka伪分布式集群

进入kafka目录，在此目录下建立一个目录etc，将config文件夹中的zookeeper.properties复制到etc文件目录中。

```language-bash
cd /usr/local/kafka
sudo mkdir etc

cd /usr/local/kafka/config	#先进入kafka的config目录中
ls          #查看config目录下的文件有没有zookeeper.properties
sudo mv zookeeper.properties /usr/local/kafka/etc  #将config文件夹中的zookeeper.properties复制到etc文件目录中
cd /usr/local/kafka/etc      #进入etc目录查看此目录有没有zookeeper.properties文件
ls

```

![image-20240502163036333](assets/image-20240502163036333.png)

将config文件夹中的server.properties复制三份至etc文件目录中，分别命名为server-0.properties、server-1.properties、server-2.properties

```language-bash
cd /usr/local/kafka/config    #进入config目录
sudo cp server.properties /usr/local/kafka/etc/server-0.properties 	 #将config文件夹中的server.properties复制第一份到etc并命名server-0.properties
sudo cp server.properties /usr/local/kafka/etc/server-1.properties  #第二份复制命名
sudo cp server.properties /usr/local/kafka/etc/server-2.properties  #第三份复制命名
cd /usr/local/kafka/etc    #进入etc目录查看是否复制成功
ls                 
```

![image-20240502163223307](assets/image-20240502163223307.png)

### 2 配置三个server-X.properties文件

分别编辑三个broker配置server-X.properties文件中的以下信息：

```language-bash
broker.id = X  
listeners = PLAINTEXT://:9092(9093,9094)    
log.dirs.=/tmp/kafka-logsX 
```

具体操作命令：

```language-bash
cd /usr/local/kafka/etc
ls    #进入ect界面后查看是否存在server-X.properties文件
sudo vim ../etc/server-0.properties      #编辑server-0.properties文件
sudo vim ../etc/server-1.properties      #编辑server-1.properties文件
sudo vim ../etc/server-2.properties      #编辑server-2.properties文件
```

![image-20240502170357685](assets/image-20240502170357685.png)

对于3个伪分布式集群分别进行修改：

![image-20240502170651831](assets/image-20240502170651831.png)

### 3 启动zookeeper服务器和kafka集群

首先启动zookeeper：

```language-bash
cd /usr/local/kafka 
./bin/zookeeper-server-start.sh  etc/zookeeper.properties
```

![image-20240503104558476](assets/image-20240503104558476.png)

启动kafka集群

```language-bash
cd /usr/local/kafka
./bin/kafka-server-start.sh  etc/server-0.properties   
./bin/kafka-server-start.sh  etc/server-1.properties   #相同路径下，启动新端口操作
./bin/kafka-server-start.sh  etc/server-2.properties   #相同路径下，启动新端口操作
```

![image-20240503104807791](assets/image-20240503104807791.png)

启动集群看是否成功

```language-bash
jps
```

![image-20240503104849021](assets/image-20240503104849021.png)

启动成功！

注意，启动的zookeeper和3个server都需要保持在启动状态，不能关闭终端。

## Step2 Java实现数据统计

### 0 配置Topic

创建3个Topic以完成任务，分别是`comments`, `likes`, 和 `shares`，

```language-bash
cd /usr/local/kafka
./bin/kafka-topics.sh --create --topic comments --partitions 3 --replication-factor 2 --bootstrap-server localhost:9092,localhost:9093,localhost:9094

./bin/kafka-topics.sh --create --topic likes --partitions 3 --replication-factor 2 --bootstrap-server localhost:9092,localhost:9093,localhost:9094

./bin/kafka-topics.sh --create --topic shares --partitions 3 --replication-factor 2 --bootstrap-server localhost:9092,localhost:9093,localhost:9094
```

![image-20240503195025676](assets/image-20240503195025676.png)

创建成功！

查看创建的Topics

```language-bash
cd /usr/local/kafka
./bin/kafka-topics.sh --describe --topic comments --bootstrap-server localhost:9092,localhost:9093,localhost:9094
./bin/kafka-topics.sh --describe --topic likes,shares --bootstrap-server localhost:9092,localhost:9093,localhost:9094
```

![image-20240503195530755](assets/image-20240503195530755.png)

### 1 Kafka 生产者

首先创建一个maven框架：



```java
import org.apache.kafka.clients.producer.*;
import java.io.*;
import java.util.Properties;

public class SocialMediaProducer {
    public static void main(String[] args) {
        String inputFile = "path/to/student_dataset.txt"; // Change this to your file path
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        Producer<String, String> producer = new KafkaProducer<>(props);

        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" ");
                String type = parts[0];
                String message = line.substring(type.length()).trim();

                switch (type) {
                    case "like":
                        producer.send(new ProducerRecord<>("likes", null, message));
                        break;
                    case "comment":
                        producer.send(new ProducerRecord<>("comments", null, message));
                        break;
                    case "share":
                        producer.send(new ProducerRecord<>("shares", null, message));
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            producer.close();
        }
    }
}

```

### 2 Kafka 消费者



