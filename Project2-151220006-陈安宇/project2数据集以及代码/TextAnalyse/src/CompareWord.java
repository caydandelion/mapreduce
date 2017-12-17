import java.io.IOException;  
import java.util.Random;  
  
import org.apache.hadoop.conf.Configuration;  
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;  
import org.apache.hadoop.io.IntWritable;  
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;  
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.Job;  
import org.apache.hadoop.mapreduce.Mapper;  
import org.apache.hadoop.mapreduce.Reducer;  
import org.apache.hadoop.mapreduce.Reducer.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;  
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.map.InverseMapper;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;  
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;  






  
public class CompareWord {  
    public static class MyMapper extends  
            Mapper<Object, Text, Text, IntWritable> {
    	
    	private IntWritable result = new IntWritable();
        public void map(Object key, Text value, Context context)  
                throws IOException, InterruptedException { 
        	String val = value.toString().replaceAll("\t", " ");
        	
        	String[] str = val.split(" ");
            //IntWritable data = new IntWritable(Integer.parseInt(value.toString()));  
            //IntWritable random = new IntWritable(new Random().nextInt()); 
        	System.out.println("*");
        	float f_end = Float.parseFloat(str[1]);
        	System.out.println(f_end);
        	int sum = (int)(f_end*1000);
        	System.out.println(sum);
        	result.set(sum);
            context.write(new Text(str[0]),result);  
        }  
    }  
  
    public static class IntSumReducer
    extends Reducer<Text,IntWritable,Text,IntWritable> {
    	private IntWritable result = new IntWritable();
       public void reduce(Text key, Iterable<IntWritable> values,
               Context context
                    ) throws IOException, InterruptedException {
    	   int sum = 0;
    	      for (IntWritable val : values) {
    	        sum += val.get();
    	      }
    	      result.set(sum);
           context.write(key,result);
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
    	Job job = Job.getInstance(conf, "word count");
        job.setJarByClass(CompareWord.class);
        job.setMapperClass(MyMapper.class);
        job.setCombinerClass(IntSumReducer.class);
        job.setReducerClass(IntSumReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        job.setOutputFormatClass(SequenceFileOutputFormat.class);
        Path tempDir = new Path("Compare-temp-" + Integer.toString(new Random().nextInt(Integer.MAX_VALUE))); //定义一个临时目录
    	
        FileOutputFormat.setOutputPath(job,tempDir);
        if(job.waitForCompletion(true))
        {
        	Configuration conf2 = new Configuration();
        	Job sortjob = Job.getInstance(conf2);
            sortjob.setJobName("sort word");  
        FileInputFormat.addInputPath(sortjob, tempDir);
        sortjob.setInputFormatClass(SequenceFileInputFormat.class);
        sortjob.setMapperClass(InverseMapper.class);
        sortjob.setNumReduceTasks(1);
        FileOutputFormat.setOutputPath(sortjob, new Path(args[1]));
        sortjob.setOutputKeyClass(IntWritable.class);
        sortjob.setOutputValueClass(Text.class);
        sortjob.setSortComparatorClass(IntWritableDecreasingComparator.class);

        sortjob.waitForCompletion(true);
        System.exit(sortjob.waitForCompletion(true) ? 0 : 1);
        }
        	FileSystem.get(conf).deleteOnExit(tempDir);
        
    }
  
}  