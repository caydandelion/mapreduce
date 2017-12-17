package knn;

import kvtype.TypeDistanceWritable;
import mr.KnnCombiner;
import mr.KnnMapper;
import mr.KnnReducer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import util.KnnUtils;

/**
 * knn算法实现
 * 默认输入数据维度为5 ，即T=5
 * 
 * α、K可以由外部设置
 * 
 * 输入训练数据格式：
 * Lable,x1,x2,x3,...xn
 * 
 * 输入预测数据格式(只能有一个文件)：
 * x1,x2,x3,....xn
 * 
 * 输出数据格式:
 * id,type  //  注意,这里的type是类别预测值,id为行数的id（第一行id为0）；
 * 
 * @author fansy
 *
 */
public class KnnDriver extends Configured implements Tool{
	
	public static void main(String[] args) throws Exception {
		/*String[] arg= new String[]{
			"-o","hdfs://node33:9000/output/knn01",
			"-i","hdfs://node33:9000/input/knn-train.txt",
			"-t","hdfs://node33:9000/input/knn-test.txt",
			"-method","default"
		};*/
	//	printUsage();
        int res = ToolRunner.run(new Configuration(), new KnnDriver(), args);
        if(res!=0){
        	System.err.println("Job failed...");
        	System.exit(-1);
        }
	}

	public int run(String[] args) throws Exception {
        // 设置输入参数
    	configureArgs(args);
    	// 检查参数设置
    	checkArgs();
    	// config a job and start it
        Configuration conf = KnnUtils.getConf();
     
        conf.setInt("KNN_K", KnnUtils.KNN_K);
        conf.setInt("REDUCERNUMBER", KnnUtils.NUMREDUCER);
        conf.setInt("COLUMN", KnnUtils.TRAIN_COLUMN);
        conf.set("TEST", KnnUtils.TESTFILE);
        conf.set("DELIMITER", KnnUtils.DELIMITER);
        
		Job job = Job.getInstance(conf,"Knn-T Model Job");
        job.setJarByClass(KnnDriver.class);

        job.setMapperClass(KnnMapper.class);
        job.setCombinerClass(KnnCombiner.class);
        job.setReducerClass(KnnReducer.class);

        job.setMapOutputKeyClass(IntWritable.class);
        job.setMapOutputValueClass(TypeDistanceWritable.class);
        
        job.setOutputKeyClass(IntWritable.class);
        job.setOutputValueClass(IntWritable.class);
        
        job.setNumReduceTasks(KnnUtils.NUMREDUCER);
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
    	
    	if("".equals(KnnUtils.TESTFILE)|| KnnUtils.TESTFILE==null){
    		System.err.println(" missing test file path ");
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
    		if("-t".equals(args[i])){
    			KnnUtils.TESTFILE=args[++i];
    		}
    		
    		/*if("-fs".equals(args[i])){
    			KnnUtils.FS=args[++i];
    		}

    		if("-rm".equals(args[i])){
    			KnnUtils.RM=args[++i];
    		}*/
    		if("-knnk".equals(args[i])){
    			try {
					KnnUtils.KNN_K=Integer.parseInt(args[++i]);
				} catch (Exception e) {
					KnnUtils.KNN_K=5;
				}
    		}
    		if("-reducernum".equals(args[i])){
    			try {
					KnnUtils.NUMREDUCER=Integer.parseInt(args[++i]);
				} catch (Exception e) {
					KnnUtils.NUMREDUCER=1;
				}
    		}
    		if("-column".equals(args[i])){
    			try {
					KnnUtils.TRAIN_COLUMN=Integer.parseInt(args[++i]);
				} catch (Exception e) {
					KnnUtils.TRAIN_COLUMN=1;
				}
    		}
    		if("-delimter".equals(args[i])){
    			KnnUtils.DELIMITER=args[++i];
    		}

    	}
	}

	public static void printUsage(){
    	System.err.println("Usage:");
    	System.err.println("-i input \t input train data path.");
    	System.err.println("-t test \t test file data path");
    	System.err.println("-o output \t output data path.");
    	//System.err.println("-fs fs \t namenode and port <namenode:port>");
    	//System.err.println("-rm rm \t resourcemanager and port <resourcemanager:port>");
    	System.err.println("-knnk knn_k \t knn method k ,default is 5");
    	System.err.println("-reducernum reducer number,default is 1");
    	System.err.println("-column column train data column, default is 6 .");
    	System.err.println("-delimiter  data delimiter , default is comma  .");
    }
	
	
}