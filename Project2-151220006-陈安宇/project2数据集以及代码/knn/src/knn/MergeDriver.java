package knn;

import java.io.IOException;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import util.KnnUtils;

/**
 * 如果KnnDriver的Reducer个数有多个，那么需要把结果合并,按照id顺序排序
 * 
 * @author fansy
 * @date 2015-7-28
 */
public class MergeDriver extends Configured implements Tool{

	public static void main(String[] args) throws Exception{
		int res = ToolRunner.run(new Configuration(), new MergeDriver(), args);
        if(res!=0){
        	System.err.println("Job failed...");
        	System.exit(-1);
        }
	}
	public static class MergeMapper extends Mapper<LongWritable,Text,IntWritable,IntWritable> {
		private IntWritable id = new IntWritable();
		private IntWritable type = new IntWritable();
		public void map(LongWritable key,Text value ,Context cxt) throws IOException,InterruptedException{
			String[] id_type = value.toString().split("\t");
			id.set(Integer.parseInt(id_type[0]));
			type.set(Integer.parseInt(id_type[1]));
			cxt.write(id, type);
		}
	}
	
	public static class MergeReducer extends Reducer<IntWritable ,IntWritable ,IntWritable,IntWritable>{
		private IntWritable id=new IntWritable();
		public void reduce(IntWritable key,Iterable<IntWritable> values ,Context cxt)throws 
			IOException,InterruptedException{
			id.set(key.get()+1);
			for(IntWritable v:values){
				
				cxt.write(id, v);
			}
		}
	}
	public int run(String[] args) throws Exception {

		configureArgs(args);
    	// 检查参数设置
    	checkArgs();
    	// config a job and start it
        Configuration conf = KnnUtils.getConf();
     
		Job job = Job.getInstance(conf,"Knn-T Model Job");
        job.setJarByClass(KnnDriver.class);

        job.setMapperClass(MergeMapper.class);
//        job.setCombinerClass(KnnCombiner.class);
        job.setReducerClass(MergeReducer.class);

        job.setMapOutputKeyClass(IntWritable.class);
        job.setMapOutputValueClass(IntWritable.class);
        
        job.setOutputKeyClass(IntWritable.class);
        job.setOutputValueClass(IntWritable.class);
        
        job.setNumReduceTasks(1);
        Path out =new Path(KnnUtils.OUTPUT);
        out.getFileSystem(conf).delete(out, true);
        
        FileInputFormat.addInputPath(job, new Path(KnnUtils.TRAINFILE));
        FileOutputFormat.setOutputPath(job, out);
        
        int res = job.waitForCompletion(true) ? 0 : 1;
        return res;
	}

	private void checkArgs() {
    	if("".equals(KnnUtils.TRAINFILE)|| KnnUtils.TRAINFILE==null){
    		System.err.println(" missing input path or file");
    		printUsage();
    		System.exit(-1);
    	}
    	
    	if("".equals(KnnUtils.OUTPUT)|| KnnUtils.OUTPUT==null){
    		System.err.println(" missing output path ");
    		printUsage();
    		System.exit(-1);
    	}
    	
	}

	private void configureArgs(String[] args) {
    	for(int i=0;i<args.length;i++){
    		if("-i".equals(args[i])){
    			KnnUtils.TRAINFILE=args[++i];
    		}
    		if("-o".equals(args[i])){
    			KnnUtils.OUTPUT=args[++i];
    		}
    		
    		/*if("-fs".equals(args[i])){
    			KnnUtils.FS=args[++i];
    		}

    		if("-rm".equals(args[i])){
    			KnnUtils.RM=args[++i];
    		}*/

    	}
	}

	public static void printUsage(){
    	System.err.println("Usage:");
    	System.err.println("-i input \t input train data path.");
    	System.err.println("-o output \t output data path.");
    	/*System.err.println("-fs fs \t namenode and port <namenode:port>");
    	System.err.println("-rm rm \t resourcemanager and port <resourcemanager:port>");*/
    }
	
}