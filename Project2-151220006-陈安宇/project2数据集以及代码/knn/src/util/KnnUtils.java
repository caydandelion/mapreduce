package util;


import org.apache.hadoop.conf.Configuration;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class KnnUtils {
//	private static Logger log = LoggerFactory.getLogger(KnnUtils.class);
	// 训练数据
	public static String TRAINFILE=null;
	// 测试数据
	public static String TESTFILE=null;
	// 输出数据
	public static String OUTPUT=null;
	// namenode:port
	//public static String FS=null;
	// resourcemanager:port
	//public static String RM=null;
	// reducer num
	public static int NUMREDUCER=1;  // 默认1个
	
	// 默认K的个数，5
	public static int KNN_K= 5;
	// 默认输入训练数据 的维度
	public static int TRAIN_COLUMN=6;
	// 默认数据分隔符
	public static String DELIMITER=",";
	
	
	private static Configuration conf=null;
	// 获得Configuration的方式
	public static Configuration getConf(){
		
		if(conf==null){
			conf= new Configuration();
		}
		
		/*if(FS!=null&&RM!=null){
			conf.set("fs.defaultFS", "hdfs://"+FS);
			conf.set("mapreduce.framework.name", "yarn");
			conf.set("yarn.resourcemanager.address", RM);
		}*/
		return conf;
	}
	
	
	public static String getStr(float[][] data,float[] distance){
		StringBuffer buff = new StringBuffer();
		for(int i=0;i<data.length;i++){
			for(int j=0;j<data[i].length-1;j++){
				buff.append(data[i][j]).append(",");
			}
			buff.append(data[i][data[i].length-1]).append(":");
			
			buff.append(distance[i]).append("\n");
		}
		
		return buff.toString();
	}

}
