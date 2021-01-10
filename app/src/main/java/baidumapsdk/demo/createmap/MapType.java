package baidumapsdk.demo.createmap;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;

import java.util.List;

import baidumapsdk.demo.R;
import baidumapsdk.demo.search.PoiListAdapter;
import baidumapsdk.demo.search.ReverseGeoCodeDemo;

/**
 * 基础地图类型
 */
public class MapType extends AppCompatActivity implements SensorEventListener, OnGetGeoCoderResultListener,AdapterView.OnItemClickListener {

    // MapView 是地图主控件
    private MapView mMapView;
    private BaiduMap mBaiduMap;
    private boolean isFirstLocate = true;
    private LocationClient locationClient = null;
    private MyLocationListener myListener = new MyLocationListener();
    private MyLocationConfiguration.LocationMode mCurrentMode;
    private Double lastX = 0.0;
    private float radius;
    private double longitude;
    private double latitude;
    private BDLocation mylocation;

    private GeoCoder mSearch=null;//搜索模块
    private int mLoadIndex = 0;
    private RelativeLayout mPoiInfo;
    private ListView mPoiList;
    private BitmapDescriptor mbitmap = BitmapDescriptorFactory.fromResource(R.drawable.icon_marka);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_map_type);
        locationClient = new LocationClient(getApplicationContext());
        locationClient.registerLocationListener(new MyLocationListener());

        mMapView = findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();

        mBaiduMap.setMyLocationEnabled(true);
        //获取传感器服务
        SensorManager mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mCurrentMode = MyLocationConfiguration.LocationMode.NORMAL;
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_UI);

        requestLocation();
        //若无法获得位置，默认天安门为初始位置
        double latitude = 39.915071, longitude = 116.403907;
        if (mBaiduMap.getLocationData() != null) {
            latitude = mBaiduMap.getLocationData().latitude;
            longitude = mBaiduMap.getLocationData().longitude;
        }

        // 构建地图状态
        MapStatus.Builder builder = new MapStatus.Builder();
        LatLng center = new LatLng(latitude, longitude);
        // 默认 11级
        float zoom = 11.0f;

        Intent intent = getIntent();
        if (null != intent) {
            center = new LatLng(intent.getDoubleExtra("y", latitude),
                    intent.getDoubleExtra("x", longitude));
            zoom = intent.getFloatExtra("level", 11.0f);
        }

        builder.target(center).zoom(zoom);
        MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newMapStatus(builder.build());

        // 设置地图状态
//        mBaiduMap.setMapStatus(mapStatusUpdate);
        CheckBox heat_map = this.findViewById(R.id.heat_map);
        CheckBox road_map = this.findViewById(R.id.road_map);
        heat_map.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mBaiduMap.setBaiduHeatMapEnabled(b);
            }
        });
        road_map.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mBaiduMap.setTrafficEnabled(b);
            }
        });

        mPoiList=findViewById(R.id.poi_list);
        mPoiList.setOnItemClickListener(MapType.this);
        mPoiInfo=findViewById(R.id.poi_info);
        //逆地址地理编码的使用,及周边poi的结果
        Switch poiSwitch = findViewById(R.id.adjenct_poi);
        poiSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    search_initListenser();

                }
                else{
                    showNearbyPoiView(false);
                    initListenser();
                }
            }
        });

    }
        //若搜索附近POI的初始化
    private void search_initListenser() {

        mBaiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                searchProcess(latLng);
            }

            @Override
            public void onMapPoiClick(MapPoi mapPoi) {
                searchProcess(mapPoi.getPosition());
            }
        });
        mSearch=GeoCoder.newInstance();
        mSearch.setOnGetGeoCodeResultListener(this);
    }
    /**
     * 搜索点击的POI
     */
    public void searchProcess(LatLng ptCenter) {
        ReverseGeoCodeOption reverseGeoCodeOption=new ReverseGeoCodeOption()
                .location(ptCenter)
                .newVersion(1)
                .pageNum(0)
                .radius(1000);
        // 发起反地理编码请求，该方法必须在监听之后执行，否则会在某些场景出现拿不到回调结果的情况
        mSearch.reverseGeoCode(reverseGeoCodeOption);
    }

    @Override
    public void onGetGeoCodeResult(GeoCodeResult geoCodeResult) {

    }
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {
        if(result==null||result.error!= SearchResult.ERRORNO.NO_ERROR){
            Toast.makeText(MapType.this,"抱歉，未能找到结果",Toast.LENGTH_LONG).show();
            return;
        }
        mBaiduMap.clear();
        BitmapDescriptor bitmapDescriptor=BitmapDescriptorFactory.fromResource(R.drawable.icon_marka);
        //添加poi
        mBaiduMap.addOverlay(new MarkerOptions().position(result.getLocation()).icon(bitmapDescriptor));
        //更新地图中心点
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(result.getLocation()));
        // 获取周边poi结果
        List<PoiInfo> poiList = result.getPoiList();
        if (null != poiList && poiList.size() > 0){
            PoiListAdapter poiListAdapter = new PoiListAdapter(this, poiList);
            mPoiList.setAdapter(poiListAdapter);
            showNearbyPoiView(true);
        }else {
            Toast.makeText(MapType.this, "周边没有poi", Toast.LENGTH_LONG).show();
            showNearbyPoiView(false);
        }

        Toast.makeText(MapType.this, result.getAddress() , Toast.LENGTH_LONG).show();
        bitmapDescriptor.recycle();
    }
    /**
     * 展示poi信息 view
     */
    private void showNearbyPoiView(boolean whetherShow) {
        if (whetherShow) {
            mPoiInfo.setVisibility(View.VISIBLE);
        } else {
            mPoiInfo.setVisibility(View.GONE);
        }
    }

    /**
     * 关闭POI搜索按钮时,对地图事件的消息响应
     */
    private void initListenser() {
        mBaiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MapType.this);
                builder.setIcon(R.drawable.map);
                builder.setMessage("经度:" + latLng.longitude + ",纬度:" + latLng.latitude);
                builder.setPositiveButton("OK", null);
                builder.show();
            }

            @Override
            public void onMapPoiClick(MapPoi mapPoi) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MapType.this);
                builder.setIcon(R.drawable.map);
                builder.setMessage(mapPoi.getName());
                builder.setPositiveButton("OK", null);
                builder.show();
            }
        });
    }

    public void go_current(View view) {
        MapStatus.Builder builder = new MapStatus.Builder();
        float zoom = 18.0f;
        LatLng center = new LatLng(mBaiduMap.getLocationData().latitude, mBaiduMap.getLocationData().longitude);
        builder.target(center).zoom(zoom);
        MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newMapStatus(builder.build());
        mBaiduMap.animateMapStatus(mapStatusUpdate);
        String message = new String();
        String country = mylocation.getCountry();
        String city = mylocation.getCity();
        String province = mylocation.getProvince();
        String district = mylocation.getDistrict();
        String town = mylocation.getTown();
        String street = mylocation.getStreet();
        String address = mylocation.getAddrStr();
        message = "当前位置\n" + country + province + city + district + town + street + "\n" + address;

        Toast toast = Toast.makeText(getApplicationContext(), message, 2);
        toast.setGravity(Gravity.CENTER, 0, 0);

        ImageView imageView = new ImageView(getApplicationContext());
        imageView.setImageResource(R.drawable.gay);
        imageView.setAdjustViewBounds(true);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);

        LinearLayout toastView = (LinearLayout) toast.getView();
        toastView.setOrientation(LinearLayout.VERTICAL);
        toastView.addView(imageView, 0);
        toast.show();
    }

    /**
     * 处理菜单
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_map_choose, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if ((id == R.id.ordinary)) {
            mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
            return true;
        }
        if (id == R.id.satalite) {
            mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
            return true;
        }
        if (id == R.id.blank) {
            mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NONE);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * 开启室内图
     */
    public void openIndoorView(View view) {
        Intent intent = new Intent(MapType.this, IndoorMap.class);
        intent.putExtra("latitude", mBaiduMap.getLocationData().latitude);
        intent.putExtra("longitude", mBaiduMap.getLocationData().longitude);
        this.startActivity(intent);
    }

    /**
     * 清除地图缓存数据，支持清除普通地图和卫星图缓存，再次进入地图页面生效。
     */
    public void cleanMapCache(View view) {
        if (mBaiduMap == null) {
            return;
        }
        int mapType = mBaiduMap.getMapType();
        if (mapType == BaiduMap.MAP_TYPE_NORMAL) {
            // // 清除地图缓存数据
            mBaiduMap.cleanCache(BaiduMap.MAP_TYPE_NORMAL);
        } else if (mapType == BaiduMap.MAP_TYPE_SATELLITE) {
            // 清除地图缓存数据
            mBaiduMap.cleanCache(BaiduMap.MAP_TYPE_SATELLITE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 在activity执行onResume时必须调用mMapView. onResume ()
        mMapView.onResume();
        showNearbyPoiView(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 在activity执行onPause时必须调用mMapView. onPause ()
        mMapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 在activity执行onDestroy时必须调用mMapView.onDestroy()
        mMapView.onDestroy();
        mSearch.destroy();
        mBaiduMap.setMyLocationEnabled(false);
        locationClient.stop();
    }


    private void requestLocation() {

        initLocationOption();

//        mCurrentMode=MyLocationConfiguration.LocationMode.FOLLOWING;
        mCurrentMode = MyLocationConfiguration.LocationMode.NORMAL;
        mBaiduMap.setMyLocationConfiguration(new MyLocationConfiguration(mCurrentMode, true, null));
        MapStatus.Builder builder = new MapStatus.Builder();
        builder.overlook(0);
        mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
        locationClient.start();
    }

    /**
     * 初始化定位参数配置
     */
    private void initLocationOption() {
        LocationClientOption locationClientOption = new LocationClientOption();
        MyLocationListener myLocationListener = new MyLocationListener();
        //注册监听函数
        locationClient.registerLocationListener(myLocationListener);
        locationClientOption.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
//可选，默认gcj02，设置返回的定位结果坐标系，如果配合百度地图使用，建议设置为bd09ll;
        locationClientOption.setCoorType("bd09ll");
//可选，默认0，即仅定位一次，设置发起连续定位请求的间隔需要大于等于1000ms才是有效的
        locationClientOption.setScanSpan(1000);
//可选，设置是否需要地址信息，默认不需要
        locationClientOption.setIsNeedAddress(true);
//可选，设置是否需要地址描述
        locationClientOption.setIsNeedLocationDescribe(true);
//可选，设置是否需要设备方向结果
        locationClientOption.setNeedDeviceDirect(false);
//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        locationClientOption.setLocationNotify(true);
//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        locationClientOption.setIgnoreKillProcess(true);
//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        locationClientOption.setIsNeedLocationDescribe(true);
//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        locationClientOption.setIsNeedLocationPoiList(true);
//可选，默认false，设置是否收集CRASH信息，默认收集
        locationClientOption.SetIgnoreCacheException(false);
//可选，默认false，设置是否开启Gps定位
        locationClientOption.setOpenGps(true);
//可选，默认false，设置定位时是否需要海拔信息，默认不需要，除基础定位版本都可用
        locationClientOption.setIsNeedAltitude(false);
//设置打开自动回调位置模式，该开关打开后，期间只要定位SDK检测到位置变化就会主动回调给开发者，该模式下开发者无需再关心定位间隔是多少，定位SDK本身发现位置变化就会及时回调给开发者
        locationClientOption.setOpenAutoNotifyMode();
//设置打开自动回调位置模式，该开关打开后，期间只要定位SDK检测到位置变化就会主动回调给开发者
        locationClientOption.setOpenAutoNotifyMode(3000, 1, LocationClientOption.LOC_SENSITIVITY_HIGHT);
//需将配置好的LocationClientOption对象，通过setLocOption方法传递给LocationClient对象使用
        locationClient.setLocOption(locationClientOption);
        locationClientOption.setWifiCacheTimeOut(5 * 60 * 1000);
        //可选，V7.2版本新增能力
        //如果设置了该接口，首次启动定位时，会先判断当前Wi-Fi是否超出有效期，若超出有效期，会先重新扫描Wi-Fi，然后定位
//开始定位
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        double x = sensorEvent.values[SensorManager.DATA_X];
        if (Math.abs(x - lastX) > 1.0) {
            Integer mCurrentDirection = (int) x;
            MyLocationData myLocationData = new MyLocationData.Builder()
                    .accuracy(radius)
                    .direction(mCurrentDirection)
                    .latitude(latitude)
                    .longitude(longitude)
                    .build();
            mBaiduMap.setMyLocationData(myLocationData);
        }
        lastX = x;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    /**
     * 调整地图角度为朝向北方
     */
    public void adjustAngle(View view) {
        try{
            float zoomLevel=mBaiduMap.getMapStatus().zoom;
            int rotateAngle=0;
            int overlookAngle=0;
            MapStatus mapStatus=mBaiduMap.getMapStatus();
            if(mapStatus!=null){
                MapStatus build = new MapStatus.Builder(mBaiduMap.getMapStatus()).rotate(rotateAngle).zoom(zoomLevel)
                        .overlook(overlookAngle).build();
                MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newMapStatus(build);
                mBaiduMap.animateMapStatus(mapStatusUpdate);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            radius = location.getRadius();
            String coorType = location.getCoorType();
            int errorCode = location.getLocType();
            navigateTo(location);
        }
    }

    private void navigateTo(BDLocation location) {
        if (isFirstLocate) {
            LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
            MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(ll);
            mBaiduMap.animateMapStatus(update);
            update = MapStatusUpdateFactory.zoomTo(16f);
            mBaiduMap.animateMapStatus(update);
            isFirstLocate = false;
        }

        MyLocationData.Builder locationBuilder = new MyLocationData.Builder();
        locationBuilder.longitude(location.getLongitude());
        locationBuilder.latitude(location.getLatitude());
        MyLocationData locationData = locationBuilder.build();
        mBaiduMap.setMyLocationData(locationData);
        mylocation = location;
    }


}
