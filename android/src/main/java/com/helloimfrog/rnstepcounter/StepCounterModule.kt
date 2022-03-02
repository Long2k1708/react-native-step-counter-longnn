package com.helloimfrog.rnstepcounter

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.annotation.Nullable
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter


class StepCounterModule(
  private val reactContext: ReactApplicationContext
) : ReactContextBaseJavaModule(reactContext), SensorEventListener {

  private val sensorManager: SensorManager by lazy {
    reactContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
  }
  private val sharedPreferences by lazy {
    reactContext.getSharedPreferences("step_counter_longnn_pref", Context.MODE_PRIVATE)
  }
  private val sharedPreferencesEditor by lazy {
    sharedPreferences.edit()
  }

  private var totalCurrentStep: Float = 0f

  override fun getName(): String {
    return "StepCounterModule"
  }

  @ReactMethod
  fun checkSensor(promise: Promise) {
    if (sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null)
      promise.resolve("")
    else
      promise.reject("SENSOR_UNAVAILABLE", "No sensor detected")
  }

  @ReactMethod
  fun startCounter() {
    sensorManager.registerListener(
      this,
      sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER),
      SensorManager.SENSOR_DELAY_UI
    )
    totalCurrentStep = sharedPreferences.getFloat("saved_step_count", 0f)

    val jsEventDataMap = Arguments.createMap()
    sendEvent("on_counter_start", jsEventDataMap)
  }

  @ReactMethod
  fun stopCounter() {
    sensorManager.unregisterListener(this)
    sharedPreferencesEditor.putFloat("saved_step_count", totalCurrentStep)
    sharedPreferencesEditor.apply()
    sharedPreferencesEditor.commit()

    val jsEventDataMap = Arguments.createMap()
    sendEvent("on_counter_stop", jsEventDataMap)
  }

  override fun onSensorChanged(p0: SensorEvent) {
    totalCurrentStep = p0.values[0]

    val jsEventDataMap = Arguments.createMap()
    jsEventDataMap.putDouble("stepValue", totalCurrentStep.toDouble())
    sendEvent("on_step_change", jsEventDataMap)
  }

  override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
  }

  private fun sendEvent(
    eventName: String,
    @Nullable params: WritableMap
  ) {
    reactContext
      .getJSModule(RCTDeviceEventEmitter::class.java)
      .emit(eventName, params)
  }
}
