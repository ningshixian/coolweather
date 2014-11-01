package activity;


import java.io.IOException;
import java.net.URL;

import service.AutoUpdateService;
import util.HttpCallbackListener;
import util.HttpUtil;
import util.Utility;

import com.coolweather.app.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class WeatherActivity extends BaseActivity implements OnClickListener {
	final Handler handler2=new Handler(){
        @Override
        public void handleMessage(Message msg) {
           ((ImageView) WeatherActivity.this.findViewById(msg.arg1)).setImageDrawable((Drawable)msg.obj);
        }
    };
	private LinearLayout weatherInfoLayout;
	/*
	 * 用于显示城市名
	 */
	private TextView cityNameText;
	/*
	 * 用于显示城市Id
	 */
	private TextView cityId;
	/*
	 * 用于显示发布时间
	 */
	private TextView publishText;
	/*
	 * 用于显示天气图片
	 */
	private ImageView weatherImg1;
	private ImageView weatherImg2;
	/*
	 * 用于显示天气描述信息
	 */
	private TextView weatherDespText;
	/*
	 * 用于显示气温1
	 */
	private TextView temp1Text;
	/*
	 * 用于显示气温2
	 */
	private TextView temp2Text;
	/*
	 * 用于显示当前日期
	 */
	//private TextView currentDateText;
	/*
	 * 切换城市按钮
	 */
	private Button switchCity;
	/*
	 * 更新天气按钮
	 */
	private Button refreshWeather;
	/*
	 * 退出按钮
	 */
	private Button exitButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.weather_layout);
		
		//初始化各控件
		weatherInfoLayout = (LinearLayout) findViewById(R.id.weather_info_layout);
		cityNameText = (TextView) findViewById(R.id.city_name);
		cityId = (TextView) findViewById(R.id.city_id);
		publishText = (TextView) findViewById(R.id.publish_text);
		weatherImg1 = (ImageView) findViewById(R.id.weather_img1);
		weatherImg2 = (ImageView) findViewById(R.id.weather_img2);
		weatherDespText = (TextView) findViewById(R.id.weather_desp);
		temp1Text = (TextView) findViewById(R.id.temp1);
		temp2Text = (TextView) findViewById(R.id.temp2);
		//currentDateText = (TextView) findViewById(R.id.current_date);
		switchCity = (Button) findViewById(R.id.switch_city);
		refreshWeather = (Button) findViewById(R.id.refresh_weather);
		exitButton = (Button) findViewById(R.id.exit);
		
		//获取ChooseAreaActivity中的intent对象，尝试从intent中取出县级代号
		String countyCode = getIntent().getStringExtra("county_code");
		if (!TextUtils.isEmpty(countyCode)) {
			//取到县级代号时就去查询天气
			publishText.setText("同步中...");
			weatherInfoLayout.setVisibility(View.INVISIBLE);
			cityNameText.setVisibility(View.INVISIBLE);
			queryweatherCode(countyCode);//查询天气代号，传入县级代号
		}else {
			//没有县级代号时就直接显示本地存储的天气信息
			showWeather();
		}
		switchCity.setOnClickListener(this);
		refreshWeather.setOnClickListener(this);
		
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.switch_city:
			//切换城市，回到选择城市的页面ChooseAreaActivity
			Intent intent = new Intent(this,ChooseAreaActivity.class);
			intent.putExtra("from_weather_activity", true);
			startActivity(intent);
			finish();
			break;
			
		case R.id.refresh_weather:
			publishText.setText("同步中...");
			SharedPreferences prefs = PreferenceManager.
					getDefaultSharedPreferences(this);
			String weatherCode = prefs.getString("weather_code", "");//获取cityId
			if (!TextUtils.isEmpty(weatherCode)) {
				queryweatherInfo(weatherCode);
			}
			break;
		
		case R.id.exit:
			ActivityCollector.finishAll();
			finish();
			break;

		default:
			break;
		}
	}

	/*
	 * 查询县级代号所对应的天气代号
	 */
	private void queryweatherCode(String countyCode) {
		String address = "http://www.weather.com.cn/data/list3/city"+countyCode+".xml";
		queryFromServer(address,"countyCode");
	}

	/*
	 * 查询天气代号所对应的天气
	 */
	public void queryweatherInfo(String weatherCode)
	{		
		String address = "http://www.weather.com.cn/data/cityinfo/"+weatherCode+".html";
		queryFromServer(address, "weatherCode");
	}
	/*
	 * 查询天气代号所对应的天气的图片
	 */
	public void queryweatherImg(String weatherImg1,String weatherImg2)
	{		
		if (weatherImg1!=null && weatherImg2!=null) {
			final String address1 = "http://www.weather.com.cn/m2/i/icon_weather/29x20/"+weatherImg1;
			loadImage2(address1,R.id.weather_img1);
			final String address2 = "http://www.weather.com.cn/m2/i/icon_weather/29x20/"+weatherImg2;
			loadImage2(address2,R.id.weather_img2);
		}
		else if (weatherImg1!=null) {
			final String address1 = "http://www.weather.com.cn/m2/i/icon_weather/29x20/"+weatherImg1;
			loadImage2(address1,R.id.weather_img1);
		}		
				
	}
	
	//采用handler+Thread模式实现多线程异步加载图片
    private void loadImage2(final String url, final int id) {
        Thread thread = new Thread(){
            @Override
            public void run() {
              Drawable drawable = null;
                   try {
                       drawable = Drawable.createFromStream(new URL(url).openStream(), "image.png");
                   } catch (IOException e) {
                   }

               Message message= handler2.obtainMessage() ;
                message.arg1 = id;
                message.obj = drawable;
                handler2.sendMessage(message);
            }
        };
        thread.start();
        thread = null;
   }
	
	/*
	 * 根据传入的地址和类型去向服务器查询天气代号或者天气信息。
	 */
	private void queryFromServer(final String address,final String type) {
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
			
			//服务器返回的数据仍然会回调到onFinish()方法中 
			@Override
			public void onFinish(final String response) {
				if ("countyCode".equals(type)) {
					if (!TextUtils.isEmpty(response)) {
						//从服务器返回的数据中解析出天气代码
						String[] array = response.split("\\|");
						if (array != null && array.length == 2) {
							String weatherCode = array[1];
							//Toast.makeText(WeatherActivity.this, "weatherCode is "+weatherCode, Toast.LENGTH_SHORT).show();
							queryweatherInfo(weatherCode);
						}
					}
				}
				//根据天气代号查询天气信息，天气信息是以JSON格式返回
				else if("weatherCode".equals(type))
				{
					//使用JSONobject将数据全部解析出来，存储到SharedPreferences文件中。
					Utility.handleWeatherResponse(WeatherActivity.this, response);
					runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
							showWeather();//从SharedPreferences中取出数据，显示在界面。
						}
					});
				}
				
			}
				
			
			@Override
			public void onError(Exception e) {
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						publishText.setText("同步失败");
					}
				});
			}
		});
	}

	/*
	 * 从SharedPreferences文件中读取存储的天气信息，并显示到界面上。
	 */
	private void showWeather() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		cityNameText.setText(prefs.getString("city_name", ""));
		temp1Text.setText(prefs.getString("temp1", ""));
		temp2Text.setText(prefs.getString("temp2", ""));
		weatherDespText.setText(prefs.getString("weather_desp", ""));
		cityId.setText("天气代码 "+prefs.getString("weather_code", ""));
		publishText.setText("今天"+prefs.getString("publish_time", "")+"发布");
		//currentDateText.setText(prefs.getString("current_data", ""));
		queryweatherImg(prefs.getString("weather_img1", ""),prefs.getString("weather_img2", ""));
		weatherInfoLayout.setVisibility(View.VISIBLE);
		cityNameText.setVisibility(View.VISIBLE);
		
		//将自动更新服务加到showWeather()最后
		//一旦选中城市成功更新天气后，将会一直在后台运行。
		Intent intent = new Intent(this,AutoUpdateService.class);
		startService(intent);
	}

	@Override
	public void onBackPressed() {
		Intent intent = new Intent(WeatherActivity.this,ChooseAreaActivity.class);
		startActivity(intent);
		finish();
	}
	
	@Override
	protected void onDestroy() {		
		super.onDestroy();		
		}	 
	
}
	

