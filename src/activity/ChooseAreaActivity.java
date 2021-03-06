package activity;

import java.util.ArrayList;
import java.util.List;

import service.AutoUpdateService;
import util.HttpCallbackListener;
import util.HttpUtil;
import util.Utility;

import com.coolweather.app.R;

import model.City;
import model.County;
import model.Province;

import db.CoolWeatherDB;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
/*
 * 遍历省市县数据
 */
public class ChooseAreaActivity extends BaseActivity {

	public static final int LEVEL_PROVINCE = 0;
	public static final int LEVEL_CITY = 1;
	public static final int LEVEL_COUNTY = 2;
	
	private ProgressDialog progressDialog;
	private TextView titleText;
	private ListView listView;
	private ArrayAdapter<String> adapter;
	private CoolWeatherDB coolWeatherDB;
	private List<String> dataList = new ArrayList<String>();
	
	/*
	 * 省列表
	 */
	private List<Province> provinceList;
	/*
	 * 市列表
	 */
	private List<City> cityList;
	/*
	 * 县列表
	 */
	private List<County> countyList;
	/*
	 * 选中的省份
	 */
	private Province selectedProvince;
	/*
	 * 选中的城市
	 */
	private City selectedCity;
	/*
	 * 当前选中的级别
	 */
	private int currentLevel;
	/*
	 * 是否从WeatherActivity中跳转过来
	 */
	private boolean isFromWeatherActivity;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
//		isFromWeatherActivity = getIntent().
//				getBooleanExtra("from_weather_activity", false);
//		
//		//1、从SharedPreferences中读取city_selected标志位
//		//2、如果为true，说明当前已经选择过城市了，直接跳转。
//		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
//		
//		//已经选择了城市且不是从WeatherActivity跳转过来的，才回直接跳转到WeatherActivity
//		if (prefs.getBoolean("city_selected", false)&& !isFromWeatherActivity) {
//			Intent intent = new Intent(this,WeatherActivity.class);
//			startActivity(intent);
//			finish();
//			return;
//		}
		
		//提示通知
		NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Notification notification = new Notification(
				R.drawable.logo,"一个天气已经启动啦！",System.currentTimeMillis());
		notification.setLatestEventInfo(this, "一个天气", "APP处女作，请多支持", null);
		notification.defaults = Notification.DEFAULT_ALL;
		manager.notify(1,notification);
		
		setContentView(R.layout.choose_area);
		listView = (ListView) findViewById(R.id.list_view);
		titleText = (TextView) findViewById(R.id.title_text);
		
		//初始化ArrayAdapter，将它设置为ListView的适配器。
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,dataList);
		listView.setAdapter(adapter);
		coolWeatherDB = CoolWeatherDB.getInstance(this);
		listView.setOnItemClickListener(new OnItemClickListener() {
			//给ListView设置点击事件
			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int index,
					long arg3) {
				// TODO Auto-generated method stub
				if (currentLevel == LEVEL_PROVINCE) {
					selectedProvince = provinceList.get(index);
					queryCities();
				}
				else if (currentLevel == LEVEL_CITY) {
					selectedCity = cityList.get(index);
					queryCounties();
				}
				//如果选择了县级选项，就启动WeatherActivity，把选中县的代号传下去。
				else if (currentLevel == LEVEL_COUNTY) {
					String countyCode = countyList.get(index).getCountyCode();
					Intent intent = new Intent(ChooseAreaActivity.this,WeatherActivity.class);
					intent.putExtra("county_code", countyCode);
					startActivity(intent);
					finish();
				}
			}

			
		});
		queryProvinces();//加载省级数据
	}
	/*
	 * 查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器上查询。
	 */
	private void queryProvinces() {
		provinceList = coolWeatherDB.loadProvinces();
		if (provinceList.size()>0) {
			dataList.clear();
			for(Province province : provinceList)
			{
				dataList.add(province.getProvinceName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText("中国");
			currentLevel = LEVEL_PROVINCE;
		}else {
			queryFromServer(null,"province");
		}
	}
	
	/*
	 * 查询选中省内所有的市，优先从数据库查询，如果没有查询到再去服务器上查询。
	 */
	private void queryCities() {
		cityList = coolWeatherDB.loadCities(selectedProvince.getId());
		if (cityList.size()>0) {
			dataList.clear();
			for(City city : cityList)
			{
				dataList.add(city.getCityName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedProvince.getProvinceName());
			currentLevel = LEVEL_CITY;
		}else {
			queryFromServer(selectedProvince.getProvinceCode(),"city");
		}
	}

	/*
	 * 查询选中市内所有的县，优先从数据库查询，如果没有查询到再去服务器上查询。
	 */
	private void queryCounties() {
		countyList = coolWeatherDB.loadCounties(selectedCity.getId());
		if (countyList.size()>0) {
			dataList.clear();
			for(County county : countyList)
			{
				dataList.add(county.getCountyName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedCity.getCityName());
			currentLevel = LEVEL_COUNTY;
		}else {
			queryFromServer(selectedCity.getCityCode(),"county");
		}
		
	}

	/*
	 * 根据传入的代号和类型从服务器上查询省市县的数据
	 */
	private void queryFromServer(final String code,final String type) {
		String address;
		if (!TextUtils.isEmpty(code)) {
			address = "http://www.weather.com.cn/data/list3/city"+code+".xml";
		}
		else {
			address = "http://www.weather.com.cn/data/list3/city.xml";
		}
		showProgressDialog();
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
			
			@Override
			public void onFinish(String response) {
				boolean result = false;
				if ("province".equals(type)) {
					result = Utility.handleProvincesResponse(coolWeatherDB, 
							response);
				}
				else if ("city".equals(type)) {
					result = Utility.handleCitiesResponse(coolWeatherDB, 
							response, selectedProvince.getId());
				}
				else if ("county".equals(type)) {
					result = Utility.handleCountiesResponse(coolWeatherDB, 
							response, selectedCity.getId());
				}
				
				if (result) {
					//通过runOnUiThread()方法回到主线程处理逻辑
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							closeProgressDialog();
							if ("province".equals(type)) {
								queryProvinces();
							}
							else if ("city".equals(type)) {
								queryCities();
							}
							else if ("county".equals(type)){
								queryCounties();
							}
						}						
					});				
					
				}				
			}
		
			@Override
			public void onError(Exception e) {
				//通过runOnUiThread()方法回到主线程处理逻辑
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this, 
								"加载失败", Toast.LENGTH_SHORT).show();
					}
				});
			}
				
		});
	}
	
	/*
	 * 显示进度对话框
	 */
	private void showProgressDialog() {
		if (progressDialog == null) {
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("正在加载...");
			progressDialog.setCanceledOnTouchOutside(false);
		}
		progressDialog.show();
	}
	
	/*
	 * 关闭进度对话框
	 */
	private void closeProgressDialog() {
		if (progressDialog != null) {
			progressDialog.dismiss();
		}
	}
	
	
	/*
	 * 捕获Back按键，根据当前的级别来判断，此时应该返回市列表、省列表、还是直接退出。
	 */
	@Override
	public void onBackPressed() {
		if (currentLevel == LEVEL_COUNTY) {
			queryCities();
		}
		else if (currentLevel == LEVEL_CITY) {
			queryProvinces();
		}
//		else {
//			//当按下Back键时，如果是从WeatherActivity跳转过来的，则重新回到WeatherActivity
//			if (isFromWeatherActivity) {
//				Intent intent = new Intent(this,WeatherActivity.class);
//				startActivity(intent);
//			}
//			finish();
//		}	
		else if (currentLevel==LEVEL_PROVINCE){
			AlertDialog.Builder dialog = new AlertDialog.Builder(this);
	         //dialog.setIcon(android.R.drawable.ic_dialog_info);
	         dialog.setTitle("警告");
	         dialog.setMessage("你确定要退出当前程序？");
	         dialog.setPositiveButton("确定",
	                 new DialogInterface.OnClickListener() {
	                     @Override
	                     public void onClick(DialogInterface dialog, int which) {
	                         finish();
	                     }
	                 });
	         dialog.setNegativeButton("取消",
	                 new DialogInterface.OnClickListener() {
	                     @Override
	                     public void onClick(DialogInterface dialog, int which) {
	  
	                     }
	                 });
	         dialog.show();
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
	}
	
}
