import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.StringUtils;


/**
 * <p>
 * 来源：http://www.tuicool.com/articles/VnuUzuz
 * </p>
 * Create Date: Jun 17, 2016
 * Last Modify: Jun 17, 2016
 * 
 * @author <a href="http://weibo.com/u/5131020927">Q-WHai</a>
 * @see <a href="http://blog.csdn.net/lemon_tree12138">http://blog.csdn.net/lemon_tree12138</a>
 * @version 0.0.1
 */
public class ChooseWord {
    // part1------------------------------------------------------------------------
    public static class Mapper_Part1 extends Mapper<LongWritable, Text, Text, Text> {
    	
    	private boolean caseSensitive;
	    private Set<String> patternsToSkip = new HashSet<String>();

	    private Configuration conf;
	    private BufferedReader fis;
	    //private Text word = new Text();

	    @Override
	    public void setup(Context context) throws IOException,
	        InterruptedException {
	      conf = context.getConfiguration();
	      caseSensitive = conf.getBoolean("wordcount.case.sensitive", false);
	      if (conf.getBoolean("wordcount.skip.patterns", false)) {
	        URI[] patternsURIs = Job.getInstance(conf).getCacheFiles();
	        for (URI patternsURI : patternsURIs) {
	          Path patternsPath = new Path(patternsURI.getPath());
	          String patternsFileName = patternsPath.getName().toString();
	          parseSkipFile(patternsFileName);
	        }
	      }
	    }

	    private void parseSkipFile(String fileName) {
	      try {
	        fis = new BufferedReader(new FileReader(fileName));
	        String pattern = null;
	        while ((pattern = fis.readLine()) != null) {
	          patternsToSkip.add(pattern);
	        }
	      } catch (IOException ioe) {
	        System.err.println("Caught exception while parsing the cached file '"
	            + StringUtils.stringifyException(ioe));
	      }
	    }
	    
        String File_name = ""; // 保存文件名，根据文件名区分所属文件
        int all = 0; // 单词总数统计
        static Text one = new Text("1");
        String word;

        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            InputSplit inputSplit = context.getInputSplit();
            String str = ((FileSplit) inputSplit).getPath().toString();
            File_name = str.substring(str.lastIndexOf("/") + 1); // 获取文件名
            /*String line = (caseSensitive) ?
                    value.toString() : value.toString().toLowerCase();*/
            byte[] valueBytes=value.getBytes();
            String line= new String(valueBytes,"GBK");
                for (String pattern : patternsToSkip) {
                  line = line.replaceAll(pattern, "");
                }
            StringTokenizer itr = new StringTokenizer(line);
            WordSeg wd = new WordSeg();
        	List<String> lis = null;
            while (itr.hasMoreTokens()) {
            	lis = wd.seg(itr.nextToken());
              	for(int i = 0 ; i < lis.size() ; i++){
              	  if((!(lis.get(i).equals(" ")))&&(!(lis.get(i).equals("")))&&(lis.get(i).length()!= 1)){
                    word = File_name;
                    word += " ";
                    word += lis.get(i); // 将文件名加单词作为key es: test1 hello 1
                    all++;
                    context.write(new Text(word), one);
              	  }
              	}
            }
        }

        public void cleanup(Context context) throws IOException, InterruptedException {
            // Map的最后，我们将单词的总数写入。下面需要用总单词数来计算。
            String str = "";
            str += all;
            context.write(new Text(File_name + " " + "!"), new Text(str));
            // 主要这里值使用的 "!"是特别构造的。 因为!的ascii比所有的字母都小。
        }
    }

    public static class Combiner_Part1 extends Reducer<Text, Text, Text, Text> {
        float all = 0;

        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            int index = key.toString().indexOf(" ");
            // 因为!的ascii最小，所以在map阶段的排序后，!会出现在第一个
            if (key.toString().substring(index + 1, index + 2).equals("!")) {
                for (Text val : values) {
                    // 获取总的单词数。
                    all = Integer.parseInt(val.toString());
                }
                // 这个key-value被抛弃
                return;
            }
            float sum = 0; // 统计某个单词出现的次数
            for (Text val : values) {
                sum += Integer.parseInt(val.toString());
            }
            // 跳出循环后，某个单词数出现的次数就统计完了，所有 TF(词频) = sum / all
            float tmp = sum / all;
            String value = "";
            value += tmp; // 记录词频
            // 将key中单词和文件名进行互换。es: test1 hello -> hello test1
            String p[] = key.toString().split(" ");
            String key_to = "";
            key_to += p[1];
            key_to += " ";
            key_to += p[0];
            context.write(new Text(key_to), new Text(value));
        }
    }

    public static class Reduce_Part1 extends Reducer<Text, Text, Text, Text> {
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            for (Text val : values) {
                context.write(key, val);
            }
        }
    }

    public static class MyPartitoner extends Partitioner<Text, Text> {
        // 实现自定义的Partitioner
        public int getPartition(Text key, Text value, int numPartitions) {
            // 我们将一个文件中计算的结果作为一个文件保存
            // es： test1 test2
            String ip1 = key.toString();
            ip1 = ip1.substring(0, ip1.indexOf(" "));
            Text p1 = new Text(ip1);
            return Math.abs((p1.hashCode() * 127) % numPartitions);
        }
    }

    // part2-----------------------------------------------------
    public static class Mapper_Part2 extends Mapper<LongWritable, Text, Text, Text> {
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String val = value.toString().replaceAll("\t", " "); // 将vlaue中的TAB分割符换成空格
            // es: Bank
            // test1
            // 0.11764706 ->
            // Bank test1
            // 0.11764706
            int index = val.indexOf(" ");
            String s1 = val.substring(0, index); // 获取单词 作为key es: hello
            String s2 = val.substring(index + 1); // 其余部分 作为value es: test1
            // 0.11764706
            s2 += " ";
            s2 += "1"; // 统计单词在所有文章中出现的次数, “1” 表示出现一次。 es: test1 0.11764706 1
            context.write(new Text(s1), new Text(s2));
        }
    }

    public static class Reduce_Part2 extends Reducer<Text, Text, Text, Text> {
        int file_count;

        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            // 同一个单词会被分成同一个group
            file_count = context.getNumReduceTasks(); // 获取总文件数
            float sum = 0;
            List<String> vals = new ArrayList<String>();
            for (Text str : values) {
                int index = str.toString().lastIndexOf(" ");
                sum += Integer.parseInt(str.toString().substring(index + 1)); // 统计此单词在所有文件中出现的次数
                vals.add(str.toString().substring(0, index)); // 保存
            }
            double tmp = Math.log10(file_count * 1.0 / (sum * 1.0)); // 单词在所有文件中出现的次数除以总文件数
                                                                     // = IDF
            for (int j = 0; j < vals.size(); j++) {
                String val = vals.get(j);
                String end = val.substring(val.lastIndexOf(" "));
                float f_end = Float.parseFloat(end); // 读取TF
                val += " ";
                val += f_end * tmp; // tf-idf值
                context.write(key, new Text(val));
            }
        }
    }

    // part3-----------------------------------------------------
    public static class Mapper_Part3 extends Mapper<LongWritable, Text, Text, Text> {
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String val = value.toString().replaceAll("\t", " "); // 将vlaue中的TAB分割符换成空格
            // es: Bank
            // test1
            // 0.11764706 
            // 0.34445 ->
            // Bank test1
            // 0.11764706
            //  0.34446
            //int index = val.indexOf(" ");
            //String s1 = val.substring(0, index); // 获取单词 作为key es: hello
            //String s2 = val.substring(index + 1); // 其余部分 作为value es: test1
            // 0.11764706
            String[] str = val.split(" ");
            //s2 += " ";
            //s2 += "1"; // 统计单词在所有文章中出现的次数, “1” 表示出现一次。 es: test1 0.11764706 1
            context.write(new Text(str[0]), new Text(str[3]));
        }
    }

    public static class Reduce_Part3 extends Reducer<Text, Text, Text, Text> {
        String val = new String();

        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            // 同一个单词会被分成同一个group
            List<String> vals = new ArrayList<String>();
            float f_end = 0;
            for (Text str : values) {
                vals.add(str.toString()); // 保存
            }
            for (int j = 0; j < vals.size(); j++) {
                val = vals.get(j);
                f_end = f_end+Float.parseFloat(val); // 读取TF

            }
            String s = "";
            s += f_end;
            context.write(key, new Text(s));
        }
    }
   
    public static void main(String[] args) throws Exception {
        // part1----------------------------------------------------
        Configuration conf1 = new Configuration();
        // 设置文件个数，在计算DF(文件频率)时会使用
        // 获取输入文件夹内文件的个数，然后来设置NumReduceTasks
        Job job1 = Job.getInstance(conf1, "My_tdif_part1");
        job1.setJarByClass(TFIDF.class);
        job1.setMapperClass(Mapper_Part1.class);
        job1.setCombinerClass(Combiner_Part1.class); // combiner在本地执行，效率要高点。
        job1.setReducerClass(Reduce_Part1.class);
        job1.setMapOutputKeyClass(Text.class);
        job1.setMapOutputValueClass(Text.class);
        job1.setOutputKeyClass(Text.class);
        job1.setOutputValueClass(Text.class);
        
        Configuration conf = new Configuration();
        GenericOptionsParser optionParser = new GenericOptionsParser(conf, args);
        String[] remainingArgs = optionParser.getRemainingArgs();
        List<String> otherArgs = new ArrayList<String>();
        for (int i=0; i < remainingArgs.length; ++i) {
          if ("-skip".equals(remainingArgs[i])) {
            job1.addCacheFile(new Path(remainingArgs[++i]).toUri());
            job1.getConfiguration().setBoolean("wordcount.skip.patterns", true);
          } else {
            otherArgs.add(remainingArgs[i]);
          }
        }
        FileSystem hdfs = FileSystem.get(conf1);
        FileStatus p[] = hdfs.listStatus(new Path(otherArgs.get(0)));
        job1.setNumReduceTasks(p.length);
        job1.setPartitionerClass(MyPartitoner.class); // 使用自定义MyPartitoner
        FileInputFormat.addInputPath(job1, new Path(otherArgs.get(0)));
        FileOutputFormat.setOutputPath(job1, new Path(otherArgs.get(1)));
        job1.waitForCompletion(true);
        // part2----------------------------------------
        Configuration conf2 = new Configuration();
        Job job2 = Job.getInstance(conf2, "My_tdif_part2");
        job2.setJarByClass(TFIDF.class);
        job2.setMapOutputKeyClass(Text.class);
        job2.setMapOutputValueClass(Text.class);
        job2.setOutputKeyClass(Text.class);
        job2.setOutputValueClass(Text.class);
        job2.setMapperClass(Mapper_Part2.class);
        job2.setReducerClass(Reduce_Part2.class);
        job2.setNumReduceTasks(p.length);
        FileInputFormat.setInputPaths(job2, new Path(otherArgs.get(1)));
        FileOutputFormat.setOutputPath(job2, new Path(otherArgs.get(2)));
        job2.waitForCompletion(true);
     // part3----------------------------------------
        Configuration conf3 = new Configuration();
        Job job3 = Job.getInstance(conf3, "My_tdif_part3");
        job3.setJarByClass(TextVector.class);
        job3.setMapOutputKeyClass(Text.class);
        job3.setMapOutputValueClass(Text.class);
        job3.setOutputKeyClass(Text.class);
        job3.setOutputValueClass(Text.class);
        job3.setMapperClass(Mapper_Part3.class);
        job3.setReducerClass(Reduce_Part3.class);
        job3.setNumReduceTasks(p.length);
        FileInputFormat.setInputPaths(job3, new Path(otherArgs.get(2)));
        FileOutputFormat.setOutputPath(job3, new Path(otherArgs.get(3)));
        job3.waitForCompletion(true);
        // hdfs.delete(new Path(args[1]), true);
    }
}