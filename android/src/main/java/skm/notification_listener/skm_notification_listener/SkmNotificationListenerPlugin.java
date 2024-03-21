package skm.notification_listener.skm_notification_listener;

import static skm.notification_listener.skm_notification_listener.NotificationUtils.isPermissionGranted;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import skm.notification_listener.skm_notification_listener.models.Action;
import skm.notification_listener.skm_notification_listener.models.ActionCache;

/** SkmNotificationListenerPlugin */
public class SkmNotificationListenerPlugin implements FlutterPlugin, ActivityAware, MethodCallHandler, PluginRegistry.ActivityResultListener, EventChannel.StreamHandler {

  private static final String CHANNEL_TAG = "skm/notifications_channel";
  private static final String EVENT_TAG = "skm/notifications_event";

  private MethodChannel channel;
  private EventChannel eventChannel;
  private NotificationReceiver notificationReceiver;
  private Context context;
  private Activity mActivity;

  private Result pendingResult;
  final int REQUEST_CODE_FOR_NOTIFICATIONS = 1199;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    context = flutterPluginBinding.getApplicationContext();
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), CHANNEL_TAG);
    channel.setMethodCallHandler(this);
    eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), EVENT_TAG);
    eventChannel.setStreamHandler(this);
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    pendingResult = result;
      switch (call.method) {
          case "isPermissionGranted":
              result.success(isPermissionGranted(context));
              break;
          case "requestPermission":
              Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
              mActivity.startActivityForResult(intent, REQUEST_CODE_FOR_NOTIFICATIONS);
              break;
          case "sendReply":
              final String message = call.argument("message");

              final Integer notificationIdArgument = call.argument("notificationId");
              // Provide a default value if argument is null
              final int notificationId = notificationIdArgument != null ? notificationIdArgument : 0;

              final Action action = ActionCache.cachedNotifications.get(notificationId);
              if (action == null) {
                  result.error("Notification", "Can't find this cached notification", null);
              }
              try {
                  assert action != null;
                  action.sendReply(context, message);
                  result.success(true);
              } catch (PendingIntent.CanceledException e) {
                  Log.e("YourTag", "Error while handling PendingIntent", e);
                  result.success(false);
              }
              break;
          default:
              result.notImplemented();
              break;
      }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
    eventChannel.setStreamHandler(null);
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    this.mActivity = binding.getActivity();
    binding.addActivityResultListener(this);
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    onDetachedFromActivity();
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    onAttachedToActivity(binding);
  }

  @Override
  public void onDetachedFromActivity() {
    this.mActivity = null;
  }

  @SuppressLint("WrongConstant")
  @Override
  public void onListen(Object arguments, EventChannel.EventSink events) {
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(NotificationConstants.INTENT);
    notificationReceiver = new NotificationReceiver(events);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      context.registerReceiver(notificationReceiver, intentFilter, Context.RECEIVER_EXPORTED);
    }else{
      context.registerReceiver(notificationReceiver, intentFilter);
    }
    Intent listenerIntent = new Intent(context, NotificationReceiver.class);
    context.startService(listenerIntent);
    Log.i("NotificationPlugin", "Started the notifications tracking service.");
  }

  @Override
  public void onCancel(Object arguments) {
    context.unregisterReceiver(notificationReceiver);
    notificationReceiver = null;
  }

  @Override
  public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_CODE_FOR_NOTIFICATIONS) {
      if (resultCode == Activity.RESULT_OK) {
        pendingResult.success(true);
      } else if (resultCode == Activity.RESULT_CANCELED) {
        pendingResult.success(isPermissionGranted(context));
      } else {
        pendingResult.success(false);
      }
      return true;
    }
    return false;
  }
}
