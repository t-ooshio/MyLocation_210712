package jp.sio.testapp.mylocation.Usecase;

import android.content.Context;

import jp.sio.testapp.mylocation.L;
import jp.sio.testapp.mylocation.Repository.SettingPref;

/**
 * Created by NTT docomo on 2017/05/24.
 * Settingの値を設定したり取得する
 * 設定の保存方法はここで吸収する
 * 今回はSharedPreferenceを使用してる
 */

public class SettingUsecase {
    private SettingPref settingPref;
    private Context context;

    public SettingUsecase(Context context){
        this.context = context;
        settingPref = new SettingPref(context);
        settingPref.createPref();
    }

    /**
     * 設定を初期化する
     */
    public void setDefaultSetting(){
        settingPref.setDefaultSetting();
    }

    /*********************ここからSetter**********************/
    public void setCount(int count){
        settingPref.setCount(count);
    }
    public void setInterval(long interval){
        settingPref.setInterval(interval);
    }
    public void setTimeout(long timeout){
        settingPref.setTimeout(timeout);
    }
    public void setminTime(long minTime){
        settingPref.setminTime(minTime);
    }

    public void setSuplEndWaitTIme(int suplEndWaitTIme){
        settingPref.setSuplEndWaitTime(suplEndWaitTIme);
    }
    public void setDelAssistDataTime(int delAssistDataTime){
        settingPref.setDelAssistDataTime(delAssistDataTime);
    }
    public void setIsGetSatelliteLog(boolean isGetSatelliteLog){
        settingPref.setIsGetSatelliteLog(isGetSatelliteLog);
    }
    public void setIsCold(boolean iscold){
        settingPref.setIsCold(iscold);
    }
    public void setLocationType(String locationType){
        L.d("Usecase:"+locationType);
        settingPref.setLocationType(locationType);
    }

     /*****************ここからGetter*******************/
    public String getLocationType(){
        return settingPref.getLocationType();
    }
    public int getCount(){
        return settingPref.getCount();
    }
    public long getTimeout(){
        return settingPref.getTimeout();
    }
    public long getInterval(){
        return settingPref.getInterval();
    }
    public long getminTime(){
        return settingPref.getminTime();
    }
    public boolean getIsGetSatelliteLog(){
        return settingPref.getIsGetSatelliteLog();
    }
    public boolean getIsCold(){
        return settingPref.getIsCold();
    }
    public int getSuplEndWaitTime(){
        return settingPref.getSuplEndWaitTime();
    }
    public int getDelAssistDataTime(){
        return settingPref.getDelAssistDataTime();
    }

    public void commitSetting(){
        settingPref.commitSetting();
    }
}
