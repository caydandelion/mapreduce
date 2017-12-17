package kvtype;

import java.util.Arrays;

/**
 * @author fansy
 * @date 2015-7-25
 */
public class TypeDistance {

//	private int k;
	private int[] type ;
	private float [] distances;
	
	public TypeDistance(int k){
//		this.k=k;
		type = new int[k];
		distances = new float[k];
		init(type);
		init(distances);
	}
	
	/**
	 * 根据type的众数进行返回
	 * @return
	 */
	public int getTypeMost(){
		Arrays.sort(this.type);
        int count = 1;
        int longest = 0;
        int most = 0;
        for (int i = 0; i < this.type.length - 1; i++) {
            if (this.type[i] == this.type[i + 1]) {
                count++;
            } else {
                count = 1;// 如果不等于，就换到了下一个数，那么计算下一个数的次数时，count的值应该重新赋值为一
                continue;
            }
            if (count > longest) {
                most = this.type[i];
                longest = count;
            }
        }
		
		return most;
	}
	
	/**
	 * 插入数据
	 * @param oType
	 * @param oDistance
	 */
	public void insertValues(int oType,float oDistance){
		int maxIndex = findMaxDistance();
		if(distances[maxIndex]>oDistance){//  需要把较大的替换掉
			distances[maxIndex]=oDistance;
			type[maxIndex]=oType;
		}
	}
	/**
	 * 插入值
	 * @param oTypes
	 * @param oDistances
	 */
	public void insertValues(int[] oTypes ,float[] oDistances){
		for(int i=0;i<oTypes.length;i++){
			this.insertValues(oTypes[i], oDistances[i]);
		}
	}
	
	/**
	 * 寻找最大值下标
	 * @return
	 */
	private int findMaxDistance() {
		int index = -1;
		float distance = - Float.MAX_VALUE;
		for(int i=0;i<distances.length;i++){
			if(distance<distances[i]){
				distance = distances[i];
				index =i;
			}
		}
		return index;
	}

	private void init(int[] type){
		for(int i=0;i<type.length;i++){
			type[i]=-1;
		}
	}
	
	private void init(float[]  distances){
		for(int i=0;i<distances.length;i++){
			distances[i]=Float.MAX_VALUE;
		}
	}

	public int[] getType() {
		return type;
	}

	public void setType(int[] type) {
		this.type = type;
	}

	public float[] getDistances() {
		return distances;
	}

	public void setDistances(float[] distances) {
		this.distances = distances;
	}
	
	public String toString(){
		StringBuffer buff = new StringBuffer();
		buff.append("length:"+distances.length).append(":[");
		for(int i=0;i<distances.length;i++){
			buff.append("type:").append(type[i])
			.append(",distance:").append(distances[i]).append(";");
		}
		return buff.substring(0, buff.length())+"]";
	}
}