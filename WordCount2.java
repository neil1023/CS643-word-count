import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class WordCount2 {

    private static final String OUTPUT_PATH = "/data/intermediate_output";

    public static class TokenizerMapper extends Mapper<Object, Text, Text, Text> {

        private Text word = new Text();
        private Text filename = new Text();

        private List<String> keyWords = new ArrayList<String>();

        {
            keyWords.add("education");
            keyWords.add("politics");
            keyWords.add("sports");
            keyWords.add("agriculture");
        }

        @Override
        protected void setup(Mapper.Context context) throws IOException, InterruptedException {
            filename.set(((FileSplit) context.getInputSplit()).getPath().toString());
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
            java.util.Map<String, Integer> countMap = new HashMap<String, Integer>();

            for (Text val : values) {
                String currWord = val.toString();
                if (!countMap.containsKey(currWord)) {
                    countMap.put(currWord, 1);
                } else {
                    countMap.put(currWord, countMap.get(currWord) + 1);
                }
            }

            // Gets the most frequent word
            int max = 0;
            String mostFreqKey = null;
            for (String keyWord : countMap.keySet()) {
                if (countMap.get(keyWord) > max) {
                    max = countMap.get(keyWord);
                    mostFreqKey = keyWord;
                }
            }
            context.write(key, new Text(mostFreqKey));
        }
    }

    public static class Mapper2 extends Mapper<LongWritable, Text, Text, IntWritable> {
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();
            StringTokenizer itr = new StringTokenizer(line);
            while (itr.hasMoreTokens()) {
                Text filename = new Text(itr.nextToken());
                Text word = new Text(itr.nextToken());
                context.write(word, new IntWritable(1));
            }
        }
    }

    public static class Reducer2 extends Reducer<Text, IntWritable, Text, IntWritable> {
        public void reduce(Text key, Iterable<IntWritable> values, Context context
            ) throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable val : values) {
                sum += val.get();
            }
            context.write(key, new IntWritable(sum));
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();

        // Job 1
        Job job = Job.getInstance(conf, "Job 1");
        job.setJarByClass(WordCount2.class);
        job.setJar("WordCount2.jar");
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
        job2.setJarByClass(WordCount2.class);
        job2.setJar("WordCount2.jar");
        job2.setMapperClass(Mapper2.class);
        job2.setCombinerClass(Reducer2.class);
        job2.setReducerClass(Reducer2.class);
        job2.setOutputKeyClass(Text.class);
        job2.setOutputValueClass(IntWritable.class);

        FileInputFormat.addInputPath(job2, new Path(OUTPUT_PATH));
        FileOutputFormat.setOutputPath(job2, new Path(args[1]));

        System.exit(job2.waitForCompletion(true) ? 0 : 1);
    }
}
