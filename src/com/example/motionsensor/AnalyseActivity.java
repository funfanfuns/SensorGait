package com.example.motionsensor;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class AnalyseActivity extends Activity {
	private Timer timer = new Timer();
	private TimerTask task;
	private Handler handler;
	private String title = "Conclusion";
	private XYSeries Xseries;
	private XYSeries Yseries;
	private XYSeries Zseries;
	private XYSeries Sseries;

	private XYMultipleSeriesDataset mDataset;
	private GraphicalView chart;
	private XYMultipleSeriesRenderer renderer;
	private Context context;
	private int addX = -1;
	private String result="result: ";
	private ArrayList<Float> Xacc = new ArrayList<Float>();
	private ArrayList<Float> Yacc = new ArrayList<Float>();
	private ArrayList<Float> Zacc = new ArrayList<Float>();
	private ArrayList<Float> Sacc = new ArrayList<Float>();
	private float sumStepTime;
	private float sumLeftD=0,sumRightD=0;
	private ArrayList<Float> disLeft = new ArrayList<Float>();
	private ArrayList<Float> disRight = new ArrayList<Float>();
	float sumLeft=0f,sumRight=0f;
	private static final int BUFFER_LENGTH = 1026; // 缓存长度
	// 窗口长度
	private static final int WINDOW_LENGTH = 80;
	private static final int SAMPLE_RATING = 20;
	int[] xv = new int[BUFFER_LENGTH];// 这是X轴的点
	float[] Xyv = new float[BUFFER_LENGTH];// 这是对应X加速度的Y轴暂存点
	float[] Yyv = new float[BUFFER_LENGTH];// 这是对应Y加速度的Y轴暂存点
	float[] Zyv = new float[BUFFER_LENGTH];// 这是对应Z加速度的Y轴暂存点]
	float[] Syv = new float[BUFFER_LENGTH];// 这是对应SVM的Y轴暂存点

	public NotificationManager mNotificationManager;

	private Button btn;
	private TextView status;
	
	private float sumSymAngle=0,sumSymTime=0,sumSymLength=0;
	private ArrayList<ArrayList<Float>> xSteps = new ArrayList<ArrayList<Float>>();
	private ArrayList<ArrayList<Float>> ySteps = new ArrayList<ArrayList<Float>>();
	private ArrayList<ArrayList<Float>> zSteps = new ArrayList<ArrayList<Float>>();
	
	/***
	 * 这三个数组对应的是每一步的开始、中间（左右步划分）、和结束的加速度的索引
	 *如stepBeginIndex.get(0)、stepMidIndex.get(0)、stepEndIndex.get(0)，就是第一步的开始、中间和结束的索引
	 */

	private ArrayList<Integer> stepMidIndex=new ArrayList<Integer>();
	private ArrayList<Integer> stepBeginIndex=new ArrayList<Integer>();
	private ArrayList<Integer> stepEndIndex=new ArrayList<Integer>();
	private int lastPoint;
	public float meanArray(ArrayList<Float>data)
	{
		float sum=0;
		for(int i=0;i<data.size();i++)
			sum+=data.get(i);
		return sum/data.size();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_analyse);

		context = getApplicationContext();
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Intent intent = getIntent();
		Sacc = (ArrayList<Float>) intent.getSerializableExtra("SSet");
		Xacc = (ArrayList<Float>) intent.getSerializableExtra("XSet");
		Yacc = (ArrayList<Float>) intent.getSerializableExtra("YSet");
		Zacc = (ArrayList<Float>) intent.getSerializableExtra("ZSet");
		
		float meanX=meanArray(Xacc);
		float meanY=meanArray(Yacc);
		float meanZ=meanArray(Zacc);
	
		for(int i=0;i<Xacc.size();i++)
		{
			Xacc.set(i, Xacc.get(i)-meanX);
		}
		for(int i=0;i<Yacc.size();i++)
		{
			Yacc.set(i, Yacc.get(i)-meanY);
		}
		for(int i=0;i<Zacc.size();i++)
		{
			Zacc.set(i, Zacc.get(i)-meanZ);
		}
		
		long t=(long)intent.getLongExtra("key", 0);
		btn = (Button) findViewById(R.id.btn);
		status = (TextView) findViewById(R.id.status);
		
		//InitFrame();
//		handler = new Handler() {
//			@Override
//			public void handleMessage(Message msg) {
//				// 刷新图表
//				updateChart();
//				super.handleMessage(msg);
//			}
//		};

//		task = new TimerTask() {
//			@Override
//			public void run() {
//				Message message = new Message();
//				message.what = 1;
//				handler.sendMessage(message);
//			}
//		};
		System.out.println("test 1");
		//timer.schedule(task, 50, 50); // 设置绘制时间
		btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				//judgeFall();
				System.out.println("test 2");
				//分别使用Y轴的加速度进行分割。X
				//stepSegmentation(Xacc);
				stepSegmentation(Yacc);
				//stepSegmentation(Zacc);
				System.out.println("SegEnd");
				
				trapz(Xacc);
				result+="\n左偏："+sumRight/stepBeginIndex.size()+"\t右偏"+sumLeft/stepBeginIndex.size();
				result+="\n平均步时："+(lastPoint-50)/(float)SAMPLE_RATING/stepBeginIndex.size();
				result+="\n平均步长："+(sumLeftD/stepBeginIndex.size()+sumRightD/stepBeginIndex.size());
				result+="\n对称度: "+sumSymAngle/stepBeginIndex.size()+" "+sumSymLength/stepBeginIndex.size()+" "+sumSymTime/stepBeginIndex.size();
				status.setText(result);
			}
		});
		
	}

	
	public void InitFrame() {
		context = getApplicationContext();
		// 这里获得main界面上的布局，下面会把图表画在这个布局里面
		LinearLayout layout = (LinearLayout) findViewById(R.id.linearLayout1);
		// 这个类用来放置曲线上的所有点，是一个点的集合，根据这些点画出曲线
		Xseries = new XYSeries(title);
		Yseries = new XYSeries(title);
		Zseries = new XYSeries(title);
		Sseries = new XYSeries(title);
		// 创建一个数据集的实例，这个数据集将被用来创建图表
		mDataset = new XYMultipleSeriesDataset();
		// 将点集添加到这个数据集中
		mDataset.addSeries(0, Xseries);
		mDataset.addSeries(1, Yseries);
		mDataset.addSeries(2, Zseries);
		mDataset.addSeries(3, Sseries);
		// 以下都是曲线的样式和属性等等的设置，renderer相当于一个用来给图表做渲染的句柄
		int color = Color.BLACK;
		PointStyle style = PointStyle.POINT;
		renderer = buildRenderer(color, style, true);
		renderer.setPanLimits(new double[] { 0, 10000, -30, 30 });
		renderer.setZoomEnabled(false, true);
		renderer.setZoomInLimitX(1);
		renderer.setZoomInLimitY(5);
		// 设置好图表的样式
		setChartSettings(renderer, "X", "Y", 0, 100, -10, 10, Color.WHITE, Color.WHITE);
		// 生成图表
		chart = ChartFactory.getCubeLineChartView(context, mDataset, renderer, 0);
		// 将图表添加到布局中去
		layout.addView(chart, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
	}

	protected XYMultipleSeriesRenderer buildRenderer(int color, PointStyle style, boolean fill) {
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
		// 设置图表中曲线本身的样式，包括颜色、点的大小以及线的粗细等
		XYSeriesRenderer Xrenderer = new XYSeriesRenderer();
		Xrenderer.setColor(Color.GREEN);
		Xrenderer.setPointStyle(style);
		Xrenderer.setFillPoints(fill);
		Xrenderer.setLineWidth(1);
		XYSeriesRenderer Yrenderer = new XYSeriesRenderer();
		Yrenderer.setColor(Color.RED);
		Yrenderer.setPointStyle(style);
		Yrenderer.setFillPoints(fill);
		Yrenderer.setLineWidth(1);
		XYSeriesRenderer Zrenderer = new XYSeriesRenderer();
		Zrenderer.setColor(Color.BLUE);
		Zrenderer.setPointStyle(style);
		Zrenderer.setFillPoints(fill);
		Zrenderer.setLineWidth(1);
		XYSeriesRenderer Srenderer = new XYSeriesRenderer();
		Srenderer.setColor(color);
		Srenderer.setPointStyle(style);
		Srenderer.setFillPoints(fill);
		Srenderer.setLineWidth(1);
		renderer.addSeriesRenderer(Xrenderer);
		renderer.addSeriesRenderer(Yrenderer);
		renderer.addSeriesRenderer(Zrenderer);
		renderer.addSeriesRenderer(Srenderer);
		return renderer;
	}

	protected void setChartSettings(XYMultipleSeriesRenderer renderer, String xTitle, String yTitle, double xMin,
			double xMax, double yMin, double yMax, int axesColor, int labelsColor) {
		// 有关对图表的渲染可参看api文档
		renderer.setChartTitle(title);
		renderer.setXTitle(xTitle);
		renderer.setYTitle(yTitle);
		renderer.setXAxisMin(xMin);
		renderer.setXAxisMax(xMax);
		renderer.setYAxisMin(yMin);
		renderer.setYAxisMax(yMax);
		renderer.setAxesColor(axesColor);
		renderer.setLabelsColor(labelsColor);
		renderer.setShowGrid(false);
		renderer.setXLabels(20);
		renderer.setYLabels(10);
		renderer.setXTitle("Time");
		renderer.setYTitle("num");
		renderer.setYLabelsAlign(Align.RIGHT);
		renderer.setPointSize((float) 2);
		renderer.setShowLegend(false);
	}

	private void updateChart() {

		// 设置好下一个需要增加的节点
		addX = 0;
		// 移除数据集中旧的点集
		mDataset.removeSeries(Xseries);
		mDataset.removeSeries(Yseries);
		mDataset.removeSeries(Zseries);
		mDataset.removeSeries(Sseries);
		// 判断当前点集中到底有多少点，因为屏幕总共只能容纳100个，所以当点数超过100时，长度永远是100
		int len = Xseries.getItemCount();
		if (len > BUFFER_LENGTH) {
			len = BUFFER_LENGTH;
		}
		// 将旧的点集中x和y的数值取出来放入backup中，并且将x的值加1，造成曲线向右平移的效果
		for (int i = 0; i < len; i++) {
			xv[i] = (int) Xseries.getX(i) + 1;
		}
		// 点集先清空，为了做成新的点集而准备
		Xseries.clear();
		Yseries.clear();
		Zseries.clear();
		Sseries.clear();
		/*
		 * 将新产生的点首先加入到点集中，然后在循环体中将坐标变换后的一系列点都重新加入到点集中 //
		 * 这里可以试验一下把顺序颠倒过来是什么效果，即先运行循环体，再添加新产生的点 series.add(addX, addY); for
		 * (int k = 0; k < length; k++) { series.add(xv[k], yv[k]); }
		 * mDataset.addSeries(series); 在数据集中添加新的点集
		 */
		// 将新产生的点首先加入到点集中，然后在循环体中将坐标变换后的一系列点都重新加入到点集中
		// 这里可以试验一下把顺序颠倒过来是什么效果，即先运行循环体，再添加新产生的点

		if (Xacc.size() != 0) {
			Xseries.add(addX, (float) Xacc.get(0));
			for (int k = 0; k < len; k++) {
				if (k == Xacc.size()) {
					break;
				}
				Xseries.add(xv[k], Xacc.get(k));
			}
			// 在数据集中添加新的点集
			mDataset.addSeries(0, Xseries);
		}
		if (Yacc.size() != 0) {
			Yseries.add(addX, (float) Yacc.get(0));
			for (int k = 0; k < len; k++) {
				if (k == Yacc.size()) {
					break;
				}
				Yseries.add(xv[k], Yacc.get(k));
			}
			// 在数据集中添加新的点集
			mDataset.addSeries(1, Yseries);
		}

		if (Zacc.size() != 0) {
			Zseries.add(addX, (float) Zacc.get(0));
			for (int k = 0; k < len; k++) {
				if (k == Zacc.size()) {
					break;
				}
				Zseries.add(xv[k], Zacc.get(k));
			}
			// 在数据集中添加新的点集
			mDataset.addSeries(2, Zseries);
		}
		if (Sacc.size() != 0) {
			Sseries.add(addX, (float) Sacc.get(0));
			for (int k = 0; k < len; k++) {
				if (k == Sacc.size()) {
					break;
				}
				Sseries.add(xv[k], Sacc.get(k));
			}
			// 在数据集中添加新的点集
			mDataset.addSeries(3, Sseries);
		}
		// 视图更新，没有这一步，曲线不会呈现动态
		// 如果在非UI主线程中，需要调用postInvalidate()，具体参考api
		chart.invalidate();
		// checkfall
	}

	public void judgeFall() {
		int tem = -1;
		float svm = 0;
		ArrayList<Float> XaccWindow = new ArrayList<Float>();
		ArrayList<Float> YaccWindow = new ArrayList<Float>();
		ArrayList<Float> ZaccWindow = new ArrayList<Float>();
		ArrayList<Float> SaccWindow = new ArrayList<Float>();
		for (int i = 0; i < Sacc.size(); i++) {
			if (Sacc.get(i) > 20 && Sacc.get(i) > svm) {
				tem = i;
				svm = Sacc.get(i);
				//showMessage("疑似跌倒发生");
			}
		}
		
		
		if (tem > 0) {
			if (tem - WINDOW_LENGTH / 2 > 0 && tem + WINDOW_LENGTH / 2 < Sacc.size()) {
				for (int i = 0; i < WINDOW_LENGTH; i++) {
					SaccWindow.add(Sacc.get(tem - WINDOW_LENGTH / 2 + i));
				}
				for (int i = 0; i < WINDOW_LENGTH; i++) {
					XaccWindow.add(Xacc.get(tem - WINDOW_LENGTH / 2 + i));
				}
				for (int i = 0; i < WINDOW_LENGTH; i++) {
					YaccWindow.add(Yacc.get(tem - WINDOW_LENGTH / 2 + i));
				}
				for (int i = 0; i < WINDOW_LENGTH; i++) {
					ZaccWindow.add(Zacc.get(tem - WINDOW_LENGTH / 2 + i));
				}
			} else if(tem - WINDOW_LENGTH / 2 > 0){
				for (int i = 0; i < WINDOW_LENGTH; i++) {
					SaccWindow.add(Sacc.get(i));
				}
				for (int i = 0; i < WINDOW_LENGTH; i++) {
					XaccWindow.add(Xacc.get(i));
				}
				for (int i = 0; i < WINDOW_LENGTH; i++) {
					YaccWindow.add(Yacc.get(i));
				}
				for (int i = 0; i < WINDOW_LENGTH; i++) {
					ZaccWindow.add(Zacc.get(i));
				}
			}else if(tem + WINDOW_LENGTH / 2 < Sacc.size()){
				for(int i = Sacc.size() - WINDOW_LENGTH;i < Sacc.size();i++){
					SaccWindow.add(Sacc.get(i));
				}
				for (int i = Sacc.size() - WINDOW_LENGTH;i < Sacc.size();i++) {
					XaccWindow.add(Xacc.get(i));
				}
				for (int i = Sacc.size() - WINDOW_LENGTH;i < Sacc.size();i++) {
					YaccWindow.add(Yacc.get(i));
				}
				for (int i = Sacc.size() - WINDOW_LENGTH;i < Sacc.size();i++) {
					ZaccWindow.add(Zacc.get(i));
				}
			}
			else
			{
				status.setText("数据不规范");
			}
			float min = 100;
			for(int i = 0;i < SaccWindow.size(); i++){
				if(SaccWindow.get(i)<min){
					min = SaccWindow.get(i);
				}
			}
			float avgXBefore = getAverage(WINDOW_LENGTH / 2 + 10, WINDOW_LENGTH / 2 + 20, XaccWindow);
			float avgYBefore = getAverage(WINDOW_LENGTH / 2 + 10, WINDOW_LENGTH / 2 + 20, YaccWindow);
			float avgZBefore = getAverage(WINDOW_LENGTH / 2 + 10, WINDOW_LENGTH / 2 + 20, ZaccWindow);
			float avgXAfter = getAverage(WINDOW_LENGTH / 2 - 20, WINDOW_LENGTH / 2 - 10, XaccWindow);
			float avgYAfter = getAverage(WINDOW_LENGTH / 2 - 20, WINDOW_LENGTH / 2 - 10, YaccWindow);
			float avgZAfter = getAverage(WINDOW_LENGTH / 2 - 20, WINDOW_LENGTH / 2 - 10, ZaccWindow);
			String statusBefore = showStatus(avgXBefore, avgYBefore, avgZBefore);
			String statusAfter = showStatus(avgXAfter, avgYAfter, avgZAfter);
			
			if (statusBefore != statusAfter && tem != 0  && min < 8) {
				showMessage("跌倒发生");
				if(statusBefore=="turn down"&&statusAfter == "screen up"||statusBefore=="turn up"&&statusAfter == "screen down"){
					if(svm<=20){
						status.setText("avgXBefore: " + avgXBefore + "\n" + "avgYBefore: " + avgYBefore + "\n" + "avgZBefore: "
								+ avgZBefore + "\n" + "avgXAfter: " + avgXAfter + "\n" + "avgYAfter: " + avgYAfter + "\n"
								+ "avgZAfter: " + avgZAfter + "\n" + "statusBefore: " + statusBefore + "\n" + "statusAfter: "
								+ statusAfter + "\n" +"向前跌倒 膝盖着地");
					}
					else{
						status.setText("avgXBefore: " + avgXBefore + "\n" + "avgYBefore: " + avgYBefore + "\n" + "avgZBefore: "
								+ avgZBefore + "\n" + "avgXAfter: " + avgXAfter + "\n" + "avgYAfter: " + avgYAfter + "\n"
								+ "avgZAfter: " + avgZAfter + "\n" + "statusBefore: " + statusBefore + "\n" + "statusAfter: "
								+ statusAfter + "\n" +"向前跌倒 头部着地");
					}
					
				}
				else if(statusBefore=="turn down"&&(statusAfter == "turn left"||statusAfter == "turn right")){
					status.setText("avgXBefore: " + avgXBefore + "\n" + "avgYBefore: " + avgYBefore + "\n" + "avgZBefore: "
							+ avgZBefore + "\n" + "avgXAfter: " + avgXAfter + "\n" + "avgYAfter: " + avgYAfter + "\n"
							+ "avgZAfter: " + avgZAfter + "\n" + "statusBefore: " + statusBefore + "\n" + "statusAfter: "
							+ statusAfter + "\n" +"左右跌倒 可能危及四肢以及躯干");
				}
				else if(statusBefore=="turn down"&&statusAfter == "screen down"){
					status.setText("avgXBefore: " + avgXBefore + "\n" + "avgYBefore: " + avgYBefore + "\n" + "avgZBefore: "
							+ avgZBefore + "\n" + "avgXAfter: " + avgXAfter + "\n" + "avgYAfter: " + avgYAfter + "\n"
							+ "avgZAfter: " + avgZAfter + "\n" + "statusBefore: " + statusBefore + "\n" + "statusAfter: "
							+ statusAfter + "\n" +"向后跌倒 臀部着地");
				}
				else if(statusBefore=="turn up"&&statusAfter == "screen up"){
					status.setText("avgXBefore: " + avgXBefore + "\n" + "avgYBefore: " + avgYBefore + "\n" + "avgZBefore: "
							+ avgZBefore + "\n" + "avgXAfter: " + avgXAfter + "\n" + "avgYAfter: " + avgYAfter + "\n"
							+ "avgZAfter: " + avgZAfter + "\n" + "statusBefore: " + statusBefore + "\n" + "statusAfter: "
							+ statusAfter + "\n" +"向后跌倒 臀部着地");
				}
				else{
					status.setText("avgXBefore: " + avgXBefore + "\n" + "avgYBefore: " + avgYBefore + "\n" + "avgZBefore: "
							+ avgZBefore + "\n" + "avgXAfter: " + avgXAfter + "\n" + "avgYAfter: " + avgYAfter + "\n"
							+ "avgZAfter: " + avgZAfter + "\n" + "statusBefore: " + statusBefore + "\n" + "statusAfter: "
							+ statusAfter + "\n" +"跌倒");
				}
			}
			
			
			if(statusBefore == "screen up" && Sacc.get(tem)>28 && min < 6){
				showMessage("跌倒发生");
				status.setText("avgXBefore: " + avgXBefore + "\n" + "avgYBefore: " + avgYBefore + "\n" + "avgZBefore: "
						+ avgZBefore + "\n" + "avgXAfter: " + avgXAfter + "\n" + "avgYAfter: " + avgYAfter + "\n"
						+ "avgZAfter: " + avgZAfter + "\n" + "statusBefore: " + statusBefore + "\n" + "statusAfter: "
						+ statusAfter + "\n" +"向前跌倒 拿在手中");
			}
		}
		else{
			status.setText("没有疑似跌倒状况");
		}
	}

	private String showStatus(float X, float Y, float Z) {
		float tMax = 1.0f;
		float absx = Math.abs(X);
		float absy = Math.abs(Y);
		float absz = Math.abs(Z);

		if (absx > absy && absx > absz) {

			if (X > tMax) {
				// v.setText("turn left");
				return "turn left";
			} else if (X < -tMax) {
				// v.setText("turn right");
				return "turn right";
			}

		} else if (absy > absx && absy > absz) {

			if (Y > tMax) {
				// v.setText("turn up");
				return "turn up";
			} else if (Y < -tMax) {
				// v.setText("turn down");
				return "turn down";
			}
		}

		else if (absz > absx && absz > absy) {
			if (Z > 0) {
				// v.setText("screen up");
				return "screen up";
			} else {
				// v.setText("screen down");
				return "screen down";
			}
		} else {
			// v.setText("unknow action");
			return "unknow action";
		}
		return "error";
	}

	public float getAverage(int start, int end, ArrayList<Float> array) {
		float sum = 0;
		for (int i = start; i < end; i++) {
			sum += array.get(i);
		}
		return sum / (end - start);
	}

	// 獲取標準差
	public double getStandardDevition(int num, ArrayList<Float> array, double average) {
		double sum = 0;
		for (int i = 0; i < num; i++) {
			sum += Math.sqrt(((double) array.get(i) - average) * (array.get(i) - average));
		}
		return (sum / (num - 1));
	}

	void OnStop() {
		finish();
		super.onDestroy();
	}

	public void onBackPressed() {
		timer.cancel();
		finish();
		super.onBackPressed();
	}

	// 显示信息
	public void showMessage(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
	}
	/**************************************
	 * 
	 * 
	 * 
	 * 
	 *************************************/
	/*
	 * 步周期分割
	 *取Y轴方向加速度最大的时候作为分割标志。
	 *即一大步踩三次地，每次踩地的时候加由于身体受到向上的反作用力，所以认为此时的Y轴加速度最大
	 *
	 *
	 *
	 *
	 */
	public void stepSegmentation(ArrayList<Float> Yacc)
	{
		//
		int length = Yacc.size(),firstLocalMinIndex=0,secondLocalMinIndex=0,midLocalMinIndex =0;
		float firstLocalMinValue=0, secondLocalMinValue=0,midLocalMinValue=0;
		int begin=50;
		int maxM=27,normalM=26,d=5;
		length = Yacc.size();
		System.out.println("length = " + length);
		System.out.println("begin = " + begin);
		int c=0;
		while(begin+50<length){
			c++;
			System.out.println("begin in while = " + begin);
			ArrayList<Float> currentStep = new ArrayList<Float>();
			if(c==1){
				firstLocalMinValue=Yacc.get(begin);
				firstLocalMinIndex=begin;
				for(int i=begin;i<=begin+maxM;i++)
				{
					if(firstLocalMinValue<=Yacc.get(i))
					{
						firstLocalMinValue=Yacc.get(i);
						firstLocalMinIndex=i;
					}
				}
				
			}
			else
			{
				firstLocalMinValue=Yacc.get(begin);
				firstLocalMinIndex=begin;
			}
			stepBeginIndex.add(firstLocalMinIndex);
			secondLocalMinValue=Yacc.get(firstLocalMinIndex+normalM-d);
			secondLocalMinIndex=firstLocalMinIndex+normalM-d;
			for(int i=firstLocalMinIndex+normalM-d;i<=firstLocalMinIndex+normalM+d;i++)
			{
				if(secondLocalMinValue<=Yacc.get(i))
				{
					secondLocalMinValue=Yacc.get(i);
					secondLocalMinIndex=i;
					begin=i;
				}
			}
			stepEndIndex.add(secondLocalMinIndex);
			
			
			int mBegin=firstLocalMinIndex*2/3+secondLocalMinIndex/3;
			int mEnd=firstLocalMinIndex/3+secondLocalMinIndex*2/3;
			midLocalMinIndex=mBegin;
			midLocalMinValue=Yacc.get(midLocalMinIndex);
			for(int i=mBegin;i<mEnd;i++)
			{
				if(midLocalMinValue<=Yacc.get(i))
				{
					midLocalMinValue=Yacc.get(i);
					midLocalMinIndex=i;
					
				}
			}
			stepMidIndex.add(midLocalMinIndex);
			for(int i=firstLocalMinIndex;i<secondLocalMinValue;i++)
				currentStep.add(Yacc.get(i));
			System.out.println("start: "+firstLocalMinIndex+"\t mid: "+midLocalMinIndex+"\t end: "+secondLocalMinIndex);
			System.out.println("\n start: "+firstLocalMinValue+"\t mid: "+midLocalMinValue+"\t end: "+secondLocalMinValue);
			
			//ySteps.add(currentStep);
			//status.setText("care"+String.valueOf(stepEndIndex));
			
		}
		lastPoint=secondLocalMinIndex;
		//status.setText("time: "+length/50+"s, step: "+c);
		result+="\n步数: "+c;
	}
	
	//提取步周期
	public void calStepInterval()
	{
		
	}
	
	//提取步长
	public void calStepLength()
	{
	}
	
	//提取倾角（未完成）
	public void calStepAngel(ArrayList<Float>  Yacc)
	{
		ArrayList<Float> v=new ArrayList<Float>();
		ArrayList<Float> d=new ArrayList<Float>();
		v.add((float) 0);
		for(int i=1;i<Yacc.size();i++)
		{
			
			v.add(v.get(i-1)+Yacc.get(i));
		}
		
		d.add((float) 0);
		for(int i=1;i<v.size();i++)
		{
			d.add(d.get(i-1)+v.get(i));
		}
		System.out.println("V: "+v.get(v.size()-1)+"  D: "+d.get(d.size()-1));
		result+="\nV: "+v.get(v.size()-1)+"  D: "+d.get(d.size()-1);
	}
	public float cumtrapz2(ArrayList<Float> data)
	{
		;
		ArrayList<Float> trap1= new ArrayList();
		trap1.add((float)0);
		
		ArrayList<Float> trap2= new ArrayList();
		trap2.add((float)0);
		
		
		for(int i=1;i<data.size();i++)
		{
			trap1.add( ( (data.get(i-1)+data.get(i))/2f/(float)SAMPLE_RATING) + trap1.get(i-1) );
			
		}
		
		for(int i=1;i<trap1.size();i++)
		{
			trap2.add( ( (trap1.get(i-1)+trap1.get(i))/2f/(float)SAMPLE_RATING ) +trap2.get(i-1) );
			
		}
		
		return Math.abs( trap2.get(trap2.size()-1));
	}
	public void trapz(ArrayList<Float> data)
	{
		for(int i=0;i<stepBeginIndex.size();i++)
		{
			ArrayList<Float> temp1= new ArrayList();
			ArrayList<Float> temp2= new ArrayList();
			
			ArrayList<Float> temp3= new ArrayList();
			ArrayList<Float> temp4= new ArrayList();
			
			for(int j=stepBeginIndex.get(i);j<=stepMidIndex.get(i);j++)
			{
				temp1.add(data.get(j));
				temp3.add(Zacc.get(j));
			}
			float step1=cumtrapz2(temp1);
			//float step1=calStepLength(temp1);
			float d1=calStepLength(temp3);
			disRight.add(step1);
			
			sumLeftD+=d1;
			for(int j=stepMidIndex.get(i);j<=stepEndIndex.get(i);j++)
			{
				temp2.add(data.get(j));
				temp4.add(Zacc.get(j));
			}
			float step2=cumtrapz2(temp2);
			//float step2=calStepLength(temp2);
			float d2=calStepLength(temp4);
		
			disLeft.add(step2);
			sumRight+=step1;
			sumLeft+=step2;
			sumRightD+=d2;
			//result+="\nright: "+ step1 + "  Left: " + step2;
			sumSymAngle+=Math.min(Math.abs(step1), Math.abs(step2))/Math.max(Math.abs(step1), Math.abs(step2));
			sumSymLength+=Math.min(Math.abs(d1), Math.abs(d2))/Math.max(Math.abs(d1), Math.abs(d2));
			sumSymTime+=((float)Math.min(stepEndIndex.get(i)-stepMidIndex.get(i), stepMidIndex.get(i)-stepBeginIndex.get(i)))/((float)Math.max(stepEndIndex.get(i)-stepMidIndex.get(i), stepMidIndex.get(i)-stepBeginIndex.get(i)));
		}
		
	}
	
	public float calStepLength(ArrayList<Float> data )
	{
		ArrayList<Float> vl= new ArrayList();
		ArrayList<Float> dl= new ArrayList();
		
		vl.add(0f);
		dl.add(0f);
		
			for(int j = 1;j < data.size();j++){//(stepMidIndex.get(i)-stepBeginIndex.get(i))为左脚一步
				vl.add ( vl.get(j-1) + data.get(j)/20 + (data.get(j)-data.get(j-1))/40 );//(Zacc.get(j)-Zacc.get(j-1)除以100是因为/50/2，减小误差
				dl.add ( dl.get(j-1) + vl.get(j)/20 + (vl.get(j) - vl.get(j-1))/40 );//理由同上
		
		}
	
		return Math.abs( dl.get(data.size()-1) );
	}
}