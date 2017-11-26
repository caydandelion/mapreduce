import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.Random;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.map.InverseMapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.StringUtils;

public class WordCount3 {

  public static class TokenizerMapper
       extends Mapper<Object, Text, Text, IntWritable>{

    static enum CountersEnum { INPUT_WORDS }

    private final static IntWritable one = new IntWritable(1);
    private Text word = new Text();

    private boolean caseSensitive;
    private Set<String> patternsToSkip = new HashSet<String>();

    private Configuration conf;
    private BufferedReader fis;

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

    @Override
    public void map(Object key, Text value, Context context
                    ) throws IOException, InterruptedException {
    	String line = (caseSensitive) ?
    	          value.toString() : value.toString().toLowerCase();
      /*for (String pattern : patternsToSkip) {
    	  line = line.replaceAll(pattern, "");
      }*/
      WordSeg wd = new WordSeg();
  	  List<String> lis = null;
      //StringTokenizer itr = new StringTokenizer(line);
  	  String[] itr = line.split("\t");
  	  int t = itr.length;
  	  if(t > 5)
  	 {
  	  int m = 4;
      while(m < t-1)//while (itr.hasMoreTokens()) 
      {
    	 for (String pattern : patternsToSkip) {
          	  itr[m] = itr[m].replaceAll(pattern, "");
        }
    	lis = wd.seg(itr[m]);
      	for(int i = 0 ; i < lis.size() ; i++){
      	  if((!(lis.get(i).equals(" ")))&&(!(lis.get(i).equals("")))&&(lis.get(i).length()!= 1)){
            word.set(lis.get(i));
            context.write(word, one);
            Counter counter = context.getCounter(CountersEnum.class.getName(),
            CountersEnum.INPUT_WORDS.toString());
            counter.increment(1);}
        
      	}
      	m = m+1;
      }
  	 }
    }
  }

  public static class IntSumReducer
       extends Reducer<Text,IntWritable,Text,IntWritable> {
    private IntWritable result = new IntWritable();
    private int k = 0;
    public void reduce(Text key, Iterable<IntWritable> values,
                       Context context
                       ) throws IOException, InterruptedException {
      Configuration conf = context.getConfiguration();
      k = context.getConfiguration().getInt("k", 0); 
      int sum = 0;
      for (IntWritable val : values) {
        sum += val.get();
      }
      result.set(sum);
      if(sum >= k){
      context.write(key, result);}
    }
  }
  
  private static class IntWritableDecreasingComparator extends IntWritable.Comparator {

	     public int compare(WritableComparable a, WritableComparable b) {
	         return -super.compare(a, b);
	      }
	     public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2) {
	                return -super.compare(b1, s1, l1, b2, s2, l2);
	       }
	}

  public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();
    GenericOptionsParser optionParser = new GenericOptionsParser(conf, args);
    String[] remainingArgs = optionParser.getRemainingArgs();
    if (!(remainingArgs.length != 2 ||remainingArgs.length != 4)) {
      System.err.println("Usage: wordcount <in> <out> [-skip skipPatternFile]");
      System.exit(2);
    }
    Job job = Job.getInstance(conf, "word count");
    job.setJarByClass(WordCount3.class);
    job.setMapperClass(TokenizerMapper.class);
    job.setCombinerClass(IntSumReducer.class);
    job.setReducerClass(IntSumReducer.class);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(IntWritable.class);

    List<String> otherArgs = new ArrayList<String>();
    for (int i=0; i < remainingArgs.length; ++i) {
      if ("-skip".equals(remainingArgs[i])) {
        job.addCacheFile(new Path(remainingArgs[++i]).toUri());
        job.getConfiguration().setBoolean("wordcount.skip.patterns", true);
      } else {
        otherArgs.add(remainingArgs[i]);
      }
    }
    job.getConfiguration().setInt("k", Integer.parseInt(otherArgs.get(2)));
    FileInputFormat.addInputPath(job, new Path(otherArgs.get(0)));
    
    job.setOutputFormatClass(SequenceFileOutputFormat.class);
    //FileOutputFormat.setOutputPath(job, new Path(otherArgs.get(1)));

    //System.exit(job.waitForCompletion(true) ? 0 : 1);
    
    Path tempDir = new Path("wordcount-temp-" + Integer.toString(new Random().nextInt(Integer.MAX_VALUE))); //定义一个临时目录
	
    FileOutputFormat.setOutputPath(job,tempDir);
    if(job.waitForCompletion(true))
    {
    	Job sortjob = Job.getInstance(conf);
        job.setJobName("sort word");  
    FileInputFormat.addInputPath(sortjob, tempDir);
    sortjob.setInputFormatClass(SequenceFileInputFormat.class);
    sortjob.setMapperClass(InverseMapper.class);
    sortjob.setNumReduceTasks(1);
    FileOutputFormat.setOutputPath(sortjob, new Path(otherArgs.get(1)));
    sortjob.setOutputKeyClass(IntWritable.class);
    sortjob.setOutputValueClass(Text.class);
    sortjob.setSortComparatorClass(IntWritableDecreasingComparator.class);

    sortjob.waitForCompletion(true);
    System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
    	FileSystem.get(conf).deleteOnExit(tempDir);
    }
  
}