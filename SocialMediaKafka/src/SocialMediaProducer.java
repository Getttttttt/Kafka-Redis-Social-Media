import org.apache.kafka.clients.producer.*;
import java.io.*;
import java.util.Properties;

public class SocialMediaProducer {
    public static void main(String[] args) {
        String inputFile = "/home/hadoop/Downloads/dataset/student_dataset.txt";
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092,localhost:9093,localhost:9094");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        Producer<String, String> producer = new KafkaProducer<>(props);

        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" ", 2); 
                if (parts.length < 2) {
                    System.err.println("Skipping malformed line: " + line);
                    continue;
                }
                String type = parts[0].toLowerCase();  
                String message = parts[1];

                String topic;
                switch (type) {
                    case "like":
                        topic = "likes";
                        break;
                    case "comment":
                        topic = "comments";
                        break;
                    case "share":
                        topic = "shares";
                        break;
                    default:
                        System.err.println("Unknown type: " + type + " in line: " + line);
                        continue;
                }

                producer.send(new ProducerRecord<>(topic, null, message), new Callback() {
                    @Override
                    public void onCompletion(RecordMetadata metadata, Exception exception) {
                        if (exception != null) {
                            System.err.println("Failed to send message: " + message + " to topic: " + topic);
                            exception.printStackTrace();
                        } else {
                            System.out.println("Sent message: " + message + " to topic: " + topic);
                        }
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            producer.close();
        }
    }
}
