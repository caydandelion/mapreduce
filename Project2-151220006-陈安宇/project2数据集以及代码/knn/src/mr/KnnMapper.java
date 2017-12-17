package mr;

import java.io.IOException;
import java.util.ArrayList;

import kvtype.TypeDistance;
import kvtype.TypeDistanceWritable;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.util.LineReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KnnMapper extends Mapper<LongWritable, Text, IntWritable, TypeDistanceWritable> {
	private Logger log = LoggerFactory.getLogger(KnnMapper.class);
	private ArrayList<float[]> testdata= new ArrayList<float[]>();
	private TypeDistance[] typeDistance = null;
	
	private IntWritable id = new IntWritable();
	private TypeDistanceWritable idTypeDistance = new TypeDistanceWritable	();
	
	private String testPath= null;
	private String delimiter=null;
	private int column =0;// train data column
	private int knn_k;
	@Override // 读取test数据
	public void setup(Context cxt) throws IOException{
		Configuration conf= cxt.getConfiguration();
		testPath= conf.get("TEST");
		delimiter= conf.get("DELIMITER");
		column = conf.getInt("COLUMN", -1);// train data column
		knn_k = conf.getInt("KNN_K", -1);
		
		System.out.println("column:"+column+",knn_k:"+knn_k);
		
		Path testDataPath= new Path(testPath);
		FileSystem fs = FileSystem.get(testDataPath.toUri(), conf);
		if(fs.isDirectory(testDataPath)){
			FileStatus[] fileStatusArray = fs.listStatus(testDataPath);
			for(FileStatus file:fileStatusArray){
				readTestData(conf,fs,file.getPath(),cxt,column);// test data column is less than train data for 1
			}
		}else{
			readTestData(conf,fs,testDataPath,cxt,column);	
		}
		// 初始化
		typeDistance = new TypeDistance[testdata.size()];
		
	}
	@Override
	public void map(LongWritable key,Text value,Context cxt) throws NumberFormatException, IOException, InterruptedException{
		String[] s1= value.toString().split("\t");
		String[] s2= s1[1].split(delimiter);
		if(s2.length!=(column-1)){
			cxt.getCounter("Bad Records", "Bad Train Data").increment(1);
//			if(log.isDebugEnabled()){
				log.info(" Input Train Data size is not wright," +
						"with train data:{},column:{},delimiter:{}",
						s2.toString(),column,delimiter);
//			}
			return ; // ignore this record
		}
		// 遍历所有测试数据，计算每个训练数据和测试数据的距离
		for(int i=0;i<testdata.size();i++){
			 calDistanceAndInsert(testdata.get(i),s1[0],s2,i);
		}
	}
	
	@Override
	public void cleanup(Context cxt) throws IOException,InterruptedException{
		// 遍历输出
		for(int i=0;i<typeDistance.length;i++){
			id.set(i);
			idTypeDistance.setValues(typeDistance[i]);
			cxt.write(id, idTypeDistance);
		}
	}
	
	/**
	 * 计算距离并插入数据
	 * @param v
	 * @param line
	 * @param i 
	 */
	private void calDistanceAndInsert(float[] testData, String lab,String[] line, int i) {
		float[] inData = getAttributes(line);
		int oType = getLable(lab);
		float inDistance = calDistance(testData,inData);
		
		if(typeDistance[i]==null){// 没有存在
			typeDistance[i]= new TypeDistance(knn_k);
		}
		
		typeDistance[i].insertValues(oType, inDistance);
/*
		try{
			System.out.println("id:"+i+","+typeDistance[i]);
		}catch(Exception e){
			e.printStackTrace();
			System.out.println("id:"+i);
		}
		*/
	}
/**
	 * @param line
	 * @return
	 */
	private int getLable(String lab) {
		int label = Integer.parseInt(lab);
		return label;
	}
/**
 * 使用knn-t提供的算法计算距离
 * @param testData
 * @param inData
 * @return
 */
	private float calDistance(float[] testData, float[] inData) {
		
		return Outh(testData,inData);
	}
	/**
	 * 不包括第一列
	 * @param line
	 * @return
	 */
	private float[] getAttributes(String[] line) {
		float[] inData = new float[line.length];
		for(int i=0;i<line.length;i++){// first column is label
			inData[i] = Float.parseFloat(line[i]);
		}
		return inData;
	}

	//欧氏距离
	private float Outh(float[] testData, float[] inData) {
		float distance =0.0f;
		for(int i=0;i<testData.length;i++){
			distance += (testData[i]-inData[i])*(testData[i]-inData[i]);
		}
		return (float)Math.sqrt(distance);
	}
	/**
	 * 
	 * @param conf
	 * @param fs
	 * @param Path
	 * @param cxt
	 * @param column : train data column
	 * @throws IOException 
	 */
	private void readTestData(Configuration conf,FileSystem fs,Path Path, Context cxt, int column) throws IOException {
		FSDataInputStream dis = fs.open(Path);
		LineReader in = new LineReader(dis,conf);
		Text line = new Text();
		while(in.readLine(line)>0){
			String[] itr = line.toString().split("\t");
			String[] testData= itr[1].split(delimiter);
			if(testData.length!=(column-1)){ // column is the train data ,while testData.length is the test data column
				cxt.getCounter("Bad Records", "Bad Test Data").increment(1);
//				if(log.isDebugEnabled()){
					log.info("Test Input Data size is not suitable with the train data size," +
							"with test data:{},column:{},delimiter:{}",
							line.toString(),column,delimiter);
//				}
				continue;  // ignore the bad record
			}
			float[] vector= new float[testData.length];
			for(int i=0;i<testData.length;i++){
				vector[i]=Float.parseFloat(testData[i]);
			}
			testdata.add(vector);
		}
		in.close();
		dis.close();
	}

}