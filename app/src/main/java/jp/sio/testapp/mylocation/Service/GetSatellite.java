package jp.sio.testapp.mylocation.Service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.NonNull;

import java.util.Timer;
import java.util.TimerTask;

import jp.sio.testapp.mylocation.L;
import jp.sio.testapp.mylocation.R;
import jp.sio.testapp.mylocation.Repository.LocationLog;

/**
 * 衛生情報取得を行うためのService
 * Created by NTT docomo on 2021/07/07
 *
 * .
 */

public class GetSatellite extends Service implements LocationListener {

    private LocationManager locationManager;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;

    private Handler resultHandler;

    //設定値の格納用変数
    private final String locationType = "GetSatellite";
    private String constellationType;
    private int svid;
    private boolean usedInFix;
    private float basebandCn0DbHz;
    private float carrierFrequencyHz;
    private float cn0DbHz;
    private float elevationDegrees;

    //測位開始時間、終了時間
    private long satelliteChangeTime;

    public class GetSatelliteService_Binder extends Binder {
        public GetSatellite getService() {
            return GetSatellite.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        resultHandler = new Handler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        super.onStartCommand(intent, flags, startid);
        L.d("onStartCommand");

        //サービスがKillされるのを防止する処理
        //サービスがKillされにくくするために、Foregroundで実行する

        /** foregroundServiseの挙動がおかしいのでいったん削除
         Notification notification = new Notification.Builder(getApplicationContext(), "uebService")
         .setContentTitle("uebService")
         .setSmallIcon(R.drawable.icon)
         .setContentText("service start")
         .build();

         startForeground(1, notification);
         **/

        //画面が消灯しないようにする処理
        //画面が消灯しないようにPowerManagerを使用
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        //PowerManagerの画面つけっぱなし設定SCREEN_BRIGHT_WAKE_LOCK、非推奨の設定値だが試験アプリ的にはあったほうがいいので使用
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, getString(R.string.locationUeb));
        wakeLock.acquire();

        //設定値の取得
        // *1000は sec → msec の変換

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationStart();

        return START_STICKY;
    }

    /**
     * 測位を開始する時の処理
     */
    public void locationStart() {

        L.d("locationStart");
        GnssStatus.Callback mGnssStatusCallback = null;

        //MyLocationUsecaseで起動時にPermissionCheckを行っているのでここでは行わない
        L.d("GnssStatus.Callback");

        mGnssStatusCallback = new GnssStatus.Callback() {
            @Override
            public void onFirstFix(int ttff) {
            }

            @Override
            public void onStarted() {
            }

            @Override
            public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
                super.onSatelliteStatusChanged(status);
                L.d("onSatelliteStatusChanged");
                satelliteChangeTime = System.currentTimeMillis();

                for(int i = 0;i < status.getSatelliteCount();i++){
                    switch (status.getConstellationType(i)){
                        case GnssStatus.CONSTELLATION_BEIDOU:
                            constellationType = "BEIDOU";
                            break;
                        case GnssStatus.CONSTELLATION_GALILEO:
                            constellationType = "GALILEO";
                            break;
                        case GnssStatus.CONSTELLATION_GLONASS:
                            constellationType = "GLONASS";
                            break;
                        case GnssStatus.CONSTELLATION_GPS:
                            constellationType = "GPS";
                            break;
                        case GnssStatus.CONSTELLATION_IRNSS:
                            constellationType = "IRNSS";
                            break;
                        case GnssStatus.CONSTELLATION_QZSS:
                            constellationType = "BEIDOU";
                            break;
                        case GnssStatus.CONSTELLATION_SBAS:
                            constellationType = "SBAS";
                            break;
                        case GnssStatus.CONSTELLATION_UNKNOWN:
                            constellationType = "UNKNOWN";
                            break;

                    }
                    svid = status.getSvid(i);
                    usedInFix = status.usedInFix(i);
                    basebandCn0DbHz = status.getBasebandCn0DbHz(i);
                    carrierFrequencyHz = status.getCarrierFrequencyHz(i);
                    cn0DbHz = status.getCn0DbHz(i);
                    elevationDegrees = status.getElevationDegrees(i);
                    sendLocationBroadCast(satelliteChangeTime,constellationType,svid,usedInFix,basebandCn0DbHz,carrierFrequencyHz,cn0DbHz,elevationDegrees);

                }
            }
        };
        locationManager.registerGnssStatusCallback(mGnssStatusCallback);

    }

    /**
     * 測位が終了してこのServiceを閉じるときの処理
     * 測位回数満了、停止ボタンによる停止を想定した処理
     */
    public void serviceStop() {
        L.d("serviceStop");
        if (locationManager != null) {
            locationManager.removeUpdates(this);
            locationManager = null;
        }
        //Serviceを終わるときにForegroundも停止する
        stopForeground(true);
        sendServiceEndBroadCast();

        if(wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
        if (powerManager != null) {
            powerManager = null;
        }
        stopSelf();
    }

    @Override
    public void onLocationChanged(final Location location) {
    }


    @Override
    public void onDestroy() {
        L.d("onDestroy");
        serviceStop();
        super.onDestroy();
    }


    /**
     * 測位完了を上に通知するBroadcast 測位結果を入れる
     *
     * @param sattelliteChangeTime 衛生情報が更新された時間
     * @param constellationType 衛星の種類
     * @param svid              衛星の番号
     * @param usedInFix         fixに使われた衛星
     * @param basebandCn0DbHz
     * @param carrierFrequencyHz
     * @param cn0DbHz
     * @param elevationDegrees
     * */
    protected void sendLocationBroadCast(long sattelliteChangeTime, String constellationType,int svid, boolean usedInFix, float basebandCn0DbHz, float carrierFrequencyHz, float cn0DbHz, float elevationDegrees) {
        L.d("sendLocation");
        Intent broadcastIntent = new Intent(getResources().getString(R.string.getSatellite));
        broadcastIntent.putExtra(getResources().getString(R.string.SatelliteChangeTime), sattelliteChangeTime);
        broadcastIntent.putExtra(getResources().getString(R.string.SatelliteConstellationType), constellationType);
        broadcastIntent.putExtra(getResources().getString(R.string.SatelliteSvid), svid);
        broadcastIntent.putExtra(getResources().getString(R.string.SatelliteUsedInFix), usedInFix);
        broadcastIntent.putExtra(getResources().getString(R.string.SatelliteBaseBandCn0DbHz), basebandCn0DbHz);
        broadcastIntent.putExtra(getResources().getString(R.string.SatelliteCarrierFrequencyHz), carrierFrequencyHz);
        broadcastIntent.putExtra(getResources().getString(R.string.SatelliteCn0DnHz),cn0DbHz);
        broadcastIntent.putExtra(getResources().getString(R.string.SatelliteGetElevationDegrees),elevationDegrees);

        sendBroadcast(broadcastIntent);
    }

    /**
     * Serviceを破棄することを通知するBroadcast
     */
    protected void sendServiceEndBroadCast() {
        L.d("sendServiceEndBroadcast");
        Intent broadcastIntent = new Intent(getResources().getString(R.string.getSatellite));
        broadcastIntent.putExtra(getResources().getString(R.string.category), getResources().getString(R.string.categoryServiceEnd));
        sendBroadcast(broadcastIntent);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true; // 再度クライアントから接続された際に onRebind を呼び出させる場合は true を返す
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new GetSatelliteService_Binder();
    }

    @Override
    public void onRebind(Intent intent) {
    }
}