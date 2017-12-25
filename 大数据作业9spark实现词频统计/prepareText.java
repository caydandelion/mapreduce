import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
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
public class prepareText {
    // part1------------------------------------------------------------------------
    public static class Mapper_Part extends Mapper<LongWritable, Text, Text, Text> {
    	
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
	    
        
        static Text one = new Text("1");

        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String line = (caseSensitive) ?
                    value.toString() : value.toString().toLowerCase();
            
                for (String pattern : patternsToSkip) {
                  line = line.replaceAll(pattern, "");
                }
            String word= new String();    
            StringTokenizer itr = new StringTokenizer(line);
            WordSeg wd = new WordSeg();
        	List<String> lis = null;
            while (itr.hasMoreTokens()) {
            	lis = wd.seg(itr.nextToken());
              	for(int i = 0 ; i < lis.size() ; i++){
              	  if((!(lis.get(i).equals(" ")))&&(!(lis.get(i).equals("")))&&(lis.get(i).length()!= 1)){
                    word += lis.get(i); 
                    word += "\t";
              	  }
              
            }
            System.out.println(word);
        }
            int index = word.lastIndexOf("\t");
            if(index > 0){
            String s1 = word.substring(0, index);
            if(s1.split("\t").length>=1){
             context.write(new Text(s1), one);}
            }
    }
    }
    public static class Reduce_Part extends Reducer<Text, Text, Text, Text> {
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            for (Text val : values) {
                context.write(key, val);
            }
        }
    }
    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        GenericOptionsParser optionParser = new GenericOptionsParser(conf, args);
        String[] remainingArgs = optionParser.getRemainingArgs();
        Job job = Job.getInstance(conf, "word count");
        job.setJarByClass(prepareText.class);
        job.setMapperClass(Mapper_Part.class);
        job.setReducerClass(Reduce_Part.class);
        job.setNumReduceTasks(1);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        

        List<String> otherArgs = new ArrayList<String>();
        for (int i=0; i < remainingArgs.length; ++i) {
          if ("-skip".equals(remainingArgs[i])) {
            job.addCacheFile(new Path(remainingArgs[++i]).toUri());
            job.getConfiguration().setBoolean("wordcount.skip.patterns", true);
          } else {
            otherArgs.add(remainingArgs[i]);
          }
        }
        FileInputFormat.addInputPath(job, new Path(otherArgs.get(0)));
        FileOutputFormat.setOutputPath(job,new Path(otherArgs.get(1)));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
}
}