package baidumapsdk.demo.createmap;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.LocationClient;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;

import baidumapsdk.demo.R;
import baidumapsdk.demo.layers.LocationTypeDemo;

/**
 * 基础地图类型
 */
public class MapTypeDemo extends AppCompatActivity  {

    // MapView 是地图主控件
    private MapView mMapView;
    private BaiduMap mBaiduMap;
    private LocationClient mLocClient=null;
    private MyLocationConfiguration.LocationMode mCurrentMode;
    private SensorManager mSensorManager;
    private Double lastX = 0.0;
    private int mCurrentDirection = 0;
    private double mCurrentLat = 0.0;
    private double mCurrentLon = 0.0;

    //是否首次定位
    private boolean isFirstLoc = true;
    private boolean isLocationLayerEnable = true;
    private MyLocationData myLocationData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_type);

        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();

        mSensorManager=(SensorManager)getSystemService(SENSOR_SERVICE);
        mCurrentMode=MyLocationConfiguration.LocationMode.NORMAL;

        // 构建地图状态
        MapStatus.Builder builder = new MapStatus.Builder();

        //获取当前定位的经纬度信息
//        double latitude=myLocationData.latitude;
//        double longitide=myLocationData.longitude;
//        System.out.println(latitude+" "+longitide);

        // 默认 天安门
        LatLng center = new LatLng(39.915071, 116.403907);
        // 默认 11级
        float zoom = 11.0f;

        // 该Intent是OfflineDemo中查看离线地图调起的
        Intent intent = getIntent();
        if (null != intent) {
            center = new LatLng(intent.getDoubleExtra("y", 39.915071),
                    intent.getDoubleExtra("x", 116.403907));
            zoom = intent.getFloatExtra("level", 11.0f);
        }

        builder.target(center).zoom(zoom);
        MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newMapStatus(builder.build());

        // 设置地图状态
        mBaiduMap.setMapStatus(mapStatusUpdate);
    }

    /**
     * 设置底图显示模式
     */
    public void setMapMode(View view) {
        boolean checked = ((RadioButton) view).isChecked();
        switch (view.getId()) {
            // 普通图
            case R.id.normal:
                if (checked) {
                    mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                }
                break;
            // 卫星图
            case R.id.statellite:
                if (checked) {
                    mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
                }
                break;
            // 空白地图
            case R.id.none:
                if (checked) {
                    mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NONE);
                }
                break;
            default:
                break;
        }
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
        mBaiduMap.setMyLocationEnabled(false);
        mMapView.onDestroy();
        mLocClient.stop();
    }





    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
    private void initLocation(){
        LocationClientOption option=new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setCoorType("bd09ll");
        option.setScanSpan(1000);
        option.setOpenGps(true);
        option.setLocationNotify(true);
        option.setIgnoreKillProcess(false);
        option.SetIgnoreCacheException(false);
        option.setWifiCacheTimeOut(5*60*1000);
        option.setEnableSimulateGps(false);
        option.setIsNeedAddress(true);
        option.setNeedNewVersionRgc(true);
        mLocClient.setLocOption(option);
    }

    private class MyLocationListenr extends BDAbstractLocationListener{

        @Override
        public void onReceiveLocation(BDLocation location) {
            navigateTo(location);
              /*
            StringBuilder currentPosition = new StringBuilder();
            currentPosition.append("纬度:").append(location.getLatitude()).append("\n");
            currentPosition.append("经度:").append(location.getLongitude()).append("\n");
            currentPosition.append("国家:").append(location.getCountry()).append("\n");
            currentPosition.append("省:").append(location.getProvince()).append("\n");
            currentPosition.append("市:").append(location.getCity()).append("\n");
            currentPosition.append("区:").append(location.getDistrict()).append("\n");
            currentPosition.append("村镇:").append(location.getTown()).append("\n");
            currentPosition.append("街道:").append(location.getStreet()).append("\n");
            currentPosition.append("地址:").append(location.getAddrStr()).append("\n");
            currentPosition.append("定位方式：");
            if (location.getLocType() == BDLocation.TypeGpsLocation) {
                currentPosition.append("GPS");
            } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {
                currentPosition.append("网络");
            }
            locationInfo.setText(currentPosition);

             */
        }
    }

    private void navigateTo(BDLocation bdLocation){
        if(isFirstLoc){
            LatLng ll=new LatLng(bdLocation.getLatitude(),bdLocation.getLongitude());
            MapStatusUpdate update=MapStatusUpdateFactory.newLatLng(ll);
            mBaiduMap.animateMapStatus(update);
            update=MapStatusUpdateFactory.zoomTo(16f);
            mBaiduMap.animateMapStatus(update);
            isFirstLoc=false;
        }
        MyLocationData.Builder locationBuilder=new MyLocationData.Builder();
        locationBuilder.latitude(bdLocation.getLatitude());
        locationBuilder.longitude(bdLocation.getLongitude());
        MyLocationData locationData=locationBuilder.build();
        mBaiduMap.setMyLocationData(locationData);
    }

}
