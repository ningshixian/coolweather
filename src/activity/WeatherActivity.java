package activity;


import service.AutoUpdateService;
import util.HttpCallbackListener;
import util.HttpUtil;
import util.Utility;

import com.coolweather.app.R;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class WeatherActivity extends Activity implements OnClickListener {

	private LinearLayout weatherInfoLayout;
	/*
	 * ������ʾ������
	 */
	private TextView cityNameText;
	/*
	 * ������ʾ����ʱ��
	 */
	private TextView publishText;
	/*
	 * ������ʾ����������Ϣ
	 */
	private TextView weatherDespText;
	/*
	 * ������ʾ����1
	 */
	private TextView temp1Text;
	/*
	 * ������ʾ����2
	 */
	private TextView temp2Text;
	/*
	 * ������ʾ��ǰ����
	 */
	private TextView currentDateText;
	/*
	 * �л����а�ť
	 */
	private Button switchCity;
	/*
	 * ����������ť
	 */
	private Button refreshWeather;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.weather_layout);
		//��ʼ�����ؼ�
		weatherInfoLayout = (LinearLayout) findViewById(R.id.weather_info_layout);
		cityNameText = (TextView) findViewById(R.id.city_name);
		publishText = (TextView) findViewById(R.id.publish_text);
		weatherDespText = (TextView) findViewById(R.id.weather_desp);
		temp1Text = (TextView) findViewById(R.id.temp1);
		temp2Text = (TextView) findViewById(R.id.temp2);
		currentDateText = (TextView) findViewById(R.id.current_date);
		switchCity = (Button) findViewById(R.id.switch_city);
		refreshWeather = (Button) findViewById(R.id.refresh_weather);
		
		//��ȡChooseAreaActivity�е�intent���󣬳��Դ�intent��ȡ���ؼ�����
		String countyCode = getIntent().getStringExtra("county_code");
		if (!TextUtils.isEmpty(countyCode)) {
			//ȡ���ؼ�����ʱ��ȥ��ѯ����
			publishText.setText("ͬ����...");
			weatherInfoLayout.setVisibility(View.INVISIBLE);
			cityNameText.setVisibility(View.INVISIBLE);
			queryweatherCode(countyCode);//��ѯ�������ţ������ؼ�����
		}else {
			//û���ؼ�����ʱ��ֱ����ʾ���ش洢��������Ϣ
			showWeather();
		}
		switchCity.setOnClickListener(this);
		refreshWeather.setOnClickListener(this);
		
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.switch_city:
			//�л����У��ص�ѡ����е�ҳ��ChooseAreaActivity
			Intent intent = new Intent(this,ChooseAreaActivity.class);
			intent.putExtra("from_weather_activity", true);
			startActivity(intent);
			finish();
			break;
			
		case R.id.refresh_weather:
			publishText.setText("ͬ����...");
			SharedPreferences prefs = PreferenceManager.
					getDefaultSharedPreferences(this);
			String weatherCode = prefs.getString("weather_code", "");//��ȡcityId
			if (!TextUtils.isEmpty(weatherCode)) {
				queryweatherInfo(weatherCode);
			}
			break;

		default:
			break;
		}
	}

	/*
	 * ��ѯ�ؼ���������Ӧ����������
	 */
	private void queryweatherCode(String countyCode) {
		String address = "http://www.weather.com.cn/data/list3/city"+countyCode+".xml";
		queryFromServer(address,"countyCode");
	}

	/*
	 * ��ѯ������������Ӧ������
	 */
	public void queryweatherInfo(String weatherCode)
	{		
		String address = "http://www.weather.com.cn/data/cityinfo/"+weatherCode+".html";
		queryFromServer(address, "weatherCode");
	}
	
	/*
	 * ���ݴ���ĵ�ַ������ȥ���������ѯ�������Ż���������Ϣ��
	 */
	private void queryFromServer(final String address,final String type) {
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
			
			//���������ص�������Ȼ��ص���onFinish()������ 
			@Override
			public void onFinish(final String response) {
				if ("countyCode".equals(type)) {
					if (!TextUtils.isEmpty(response)) {
						//�ӷ��������ص������н�������������
						String[] array = response.split("\\|");
						if (array != null && array.length == 2) {
							String weatherCode = array[1];
							//Toast.makeText(WeatherActivity.this, "weatherCode is "+weatherCode, Toast.LENGTH_SHORT).show();
							queryweatherInfo(weatherCode);
						}
					}
				}
				//�����������Ų�ѯ������Ϣ��������Ϣ����JSON��ʽ����
				else if("weatherCode".equals(type))
				{
					//ʹ��JSONobject������ȫ�������������洢��SharedPreferences�ļ��С�
					Utility.handleWeatherResponse(WeatherActivity.this, response);
					runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
							showWeather();//��SharedPreferences��ȡ�����ݣ���ʾ�ڽ��档
						}
					});
				}
			}
			
			@Override
			public void onError(Exception e) {
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						publishText.setText("ͬ��ʧ��");
					}
				});
			}
		});
	}

	/*
	 * ��SharedPreferences�ļ��ж�ȡ�洢��������Ϣ������ʾ�������ϡ�
	 */
	private void showWeather() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		cityNameText.setText(prefs.getString("city_name", ""));
		temp1Text.setText(prefs.getString("temp1", ""));
		temp2Text.setText(prefs.getString("temp2", ""));
		weatherDespText.setText(prefs.getString("weather_desp", ""));
		publishText.setText("����"+prefs.getString("publish_time", "")+"����");
		currentDateText.setText(prefs.getString("current_data", ""));
		weatherInfoLayout.setVisibility(View.VISIBLE);
		cityNameText.setVisibility(View.VISIBLE);
		
		//���Զ����·���ӵ�showWeather()���
		//һ��ѡ�г��гɹ����������󣬽���һֱ�ں�̨���С�
		Intent intent = new Intent(this,AutoUpdateService.class);
		startService(intent);
	}

	
}
