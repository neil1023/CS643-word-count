import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.util.*;

public class WordCountSameRanking {

    private static final String OUTPUT_PATH = "/data/intermediate_output";

    public static class TokenizerMapper extends Mapper<Object, Text, Text, Text> {

        private Text word = new Text();
        private Text filename = new Text();

        private List<String> keyWords = new ArrayList<>();

        {
            keyWords.add("education");
            keyWords.add("politics");
            keyWords.add("sports");
            keyWords.add("agriculture");
        }

        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            String filePath = ((FileSplit) context.getInputSplit()).getPath().toString();
            filename.set(filePath.substring(filePath.lastIndexOf('/') + 1));
        }

        public void map(Object key, Text value, Context context
        ) throws IOException, InterruptedException {
            // String line = value.toString().toLowerCase().replaceAll("[_|$#<>\\^=\\[\\]\\*/\\\\,;,.\\-:()?!\"']", " ");
            String line = value.toString();
            StringTokenizer itr = new StringTokenizer(line);
            while (itr.hasMoreTokens()) {
                word.set(itr.nextToken());
                if (keyWords.contains(word.toString())) {
                    context.write(filename, word);
                }
            }
        }
    }

    public static class IntSumReducer extends Reducer<Text, Text, Text, Text> {

        public void reduce(Text key, Iterable<Text> values,
                           Context context
        ) throws IOException, InterruptedException {
            java.util.Map<String, Integer> countMap = new HashMap<>();

            for (Text val : values) {
                String currWord = val.toString();
                if (!countMap.containsKey(currWord)) {
                    countMap.put(currWord, 1);
                } else {
                    countMap.put(currWord, countMap.get(currWord) + 1);
                }
            }

            Map<String, Integer> sortedMap = new LinkedHashMap<>();

            countMap.entrySet().stream()
                    .sorted((Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2)
                            -> o2.getValue().compareTo(o1.getValue()))
                    .forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));

            // Constructs the ranking list delimited by -
            String wordRanking = "";
            for (String keyWord : sortedMap.keySet()) {
                wordRanking += keyWord + "-";
            }
            wordRanking = wordRanking.substring(0, wordRanking.length() - 1);
            context.write(key, new Text(wordRanking));
        }
    }

    public static class Mapper2 extends Mapper<LongWritable, Text, Text, Text> {
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();
            StringTokenizer itr = new StringTokenizer(line);
            while (itr.hasMoreTokens()) {
                Text state = new Text(itr.nextToken());
                Text ranking = new Text(itr.nextToken());
                context.write(ranking, state);
            }
        }
    }

    public static class Reducer2 extends Reducer<Text, Text, Text, Text> {
        public void reduce(Text key, Iterable<Text> values, Context context
            ) throws IOException, InterruptedException {
            String listOfStates = "";
            for (Text val : values) {
                listOfStates += val + ", ";
            }
            listOfStates = listOfStates.substring(0, listOfStates.length() - 2);
            context.write(key, new Text(listOfStates));
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();

        // Job 1
        Job job = Job.getInstance(conf, "Job 1");
        job.setJarByClass(WordCountSameRanking.class);
        job.setJar("WordCountSameRanking.jar");
        job.setMapperClass(TokenizerMapper.class);
        job.setCombinerClass(IntSumReducer.class);
        job.setReducerClass(IntSumReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(OUTPUT_PATH));
        job.waitForCompletion(true);

        // Job 2
        Job job2 = Job.getInstance(conf, "Job 2");
        job2.setJarByClass(WordCountSameRanking.class);
        job2.setJar("WordCountSameRanking.jar");
        job2.setMapperClass(Mapper2.class);
        job2.setCombinerClass(Reducer2.class);
        job2.setReducerClass(Reducer2.class);
        job2.setOutputKeyClass(Text.class);
        job2.setOutputValueClass(Text.class);

        FileInputFormat.addInputPath(job2, new Path(OUTPUT_PATH));
        FileOutputFormat.setOutputPath(job2, new Path(args[1]));

        System.exit(job2.waitForCompletion(true) ? 0 : 1);
    }
}
