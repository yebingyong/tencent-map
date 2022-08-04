package cn.mancando.cordovaplugin.tencentmap;

import android.app.ProgressDialog;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.tencent.map.geolocation.TencentLocationManager; //导入方法依赖的package包/类
import android.content.Intent;
import android.Manifest;

import org.apache.cordova.PluginResult;
import android.content.pm.PackageManager;


import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.geolocation.TencentLocationListener;
import com.tencent.map.geolocation.TencentLocationRequest;
import java.lang.ref.WeakReference;

import android.location.LocationManager;
import android.os.Looper;

import org.apache.cordova.PermissionHelper;


import android.util.Log;
import android.os.Build;

import android.provider.Settings;

/**
 * This class echoes a string called from JavaScript.
 */
public class TencentMap extends CordovaPlugin {
  private static final String TAG = "消息啦：";
  public static final int REQUEST_PERMISSION_COARSE_LOCATION = 1000;
  public static final int REQUEST_PERMISSION_FINE_LOCATION = 2000;
  public static final int REQUEST_PERMISSION_BACKGROUND_LOCATION = 3000;

  private CallbackContext callbackContext;
  private TencentLocationManager mLocationManager;
  private InnerLocationListener mLocationListener;

  private static final String[] SETTINGS = new String[]{"Default","GpsFirst"};
  private int mIndex = 0;
  private String mSettings = SETTINGS[mIndex];


  @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
       this.callbackContext = callbackContext;

        if (action.equals("getLocation")) {
            this.getLocation();
            return true;
        }

        return false;
    }

     private void getLocation() {
       if (!judgeLocationServerState()) {
         this.callbackContext.error("请开启位置服务");
         return;
       }

       if (!PermissionHelper.hasPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
         PermissionHelper.requestPermission(this, REQUEST_PERMISSION_COARSE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION);
       }else if(!PermissionHelper.hasPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)){
         PermissionHelper.requestPermission(this, REQUEST_PERMISSION_FINE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION);
       }else if(Build.VERSION.SDK_INT >= 29 && !PermissionHelper.hasPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)){
         PermissionHelper.requestPermission(this, REQUEST_PERMISSION_BACKGROUND_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION);
       }else {
         //需要先设置同意定位SDK的隐私政策
         TencentLocationManager.setUserAgreePrivacy(true);
         mLocationManager = TencentLocationManager.getInstance(cordova.getActivity());
         mLocationListener = new InnerLocationListener(new WeakReference<TencentMap>(this),this.callbackContext);

         if (Build.VERSION.SDK_INT >= 23) {
//         requirePermission();
           Log.i(TAG, "要求权限啦");
         }

         this.startSingleLocation();
       }
    }

  public void startSingleLocation() {
    if (mLocationManager != null) {
      TencentLocationRequest request = TencentLocationRequest.create();
      // 设置是否优先获取gps点，准确性更高
      request.setGpsFirst(true).setGpsFirstTimeOut(5*1000);
      request.setQQ("10001").setRequestLevel(3);

      //也可以使用子线程，但是必须包含Looper
      int re = mLocationManager.requestSingleFreshLocation(request, mLocationListener, Looper.getMainLooper());
      //0-成功注册监听器, 1-设备缺少使用腾讯定位服务需要的基本条件, 2-manifest 中配置的 key 不正确, 3-自动加载libtencentloc.so失败， 4-未设置或未同意用户隐私
      if(re !=0){
        if(re == 1){
          this.callbackContext.error("设备缺少使用腾讯定位服务需要的基本条件");
        }
        if(re != 0 && re != 1){
          this.callbackContext.error("定位失败");
        }
        //移除位置监听器并停止定位.
        mLocationManager.removeUpdates(mLocationListener);
      }
      Log.i(TAG, "re: " + re);
    }
  }

  //是否开启位置服务
  private boolean judgeLocationServerState() {
    try {
      return Settings.Secure.getInt(cordova.getActivity().getContentResolver(), Settings.Secure.LOCATION_MODE) > 1;
    } catch (Settings.SettingNotFoundException e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
    //权限申请时被拒绝后
    for (int r : grantResults) {
      if (r == PackageManager.PERMISSION_DENIED) {
        this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "定位失败：请授权"));
        return;
      }
    }
    switch (requestCode) {
      case REQUEST_PERMISSION_COARSE_LOCATION:
      case REQUEST_PERMISSION_FINE_LOCATION:
      case REQUEST_PERMISSION_BACKGROUND_LOCATION:
        this.getLocation();
        break;
    }
  }

  private static class InnerLocationListener implements TencentLocationListener {
    private WeakReference<TencentMap> mMainActivityWRF;
    private CallbackContext listennerCallbackContext;

    public InnerLocationListener(WeakReference<TencentMap> mainActivityWRF,CallbackContext callbackContext) {
      mMainActivityWRF = mainActivityWRF;
      listennerCallbackContext = callbackContext;
    }


    @Override
    public void onLocationChanged(TencentLocation location, int error,
                                  String reason) {
      if (mMainActivityWRF != null) {
        TencentMap mainActivity = mMainActivityWRF.get();
        if (mainActivity != null) {
          if (TencentLocation.ERROR_OK == error && location != null) {
            // 定位成功

            Log.i(TAG, location.toString());
            String lng = String.valueOf(location.getLongitude());//经度
            String lat = String.valueOf(location.getLatitude());//纬度
            try {
              JSONObject json = new JSONObject();
              json.put("lng", location.getLongitude());
              json.put("lat", location.getLatitude());

              listennerCallbackContext.success(json);
            }catch (JSONException e){
              String errMsg = e.getMessage();
              Log.i(TAG, errMsg,e);
              listennerCallbackContext.error(errMsg);
            }

          } else {
            // 定位失败
            switch (error) {
              case TencentLocation.ERROR_NETWORK:
                listennerCallbackContext.error("定位失败：暂无网络");
                break;
              case TencentLocation.ERROR_BAD_JSON:
                listennerCallbackContext.error("定位失败：GPS, Wi-Fi 或基站错误");
                break;
              case TencentLocation.ERROR_WGS84:
                //注意: 仅出现在需要将WGS84坐标转换成GCJ-02坐标时的情形
                listennerCallbackContext.error("定位失败：无法进行坐标转换");
                break;
              default:
                listennerCallbackContext.error("定位失败");
            }
          }
        }
      }
    }

    @Override
    public void onStatusUpdate(String name, int status, String desc) {
      Log.i(TAG, "name: " + name + "status: " + status + "desc: " + desc);
    }
  }

  private void returnMessage(String message) {
    this.callbackContext.success(message);
  }

}

