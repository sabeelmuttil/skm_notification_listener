import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:skm_notification_listener/skm_notification_listener.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  const MethodChannel methodChannel =
      MethodChannel('skm/notifications_channel');

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(methodChannel, (MethodCall methodCall) async {
      switch (methodCall.method) {
        case 'requestPermission':
          return true;
        case 'isPermissionGranted':
          return true;
        default:
          return null;
      }
    });
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(methodChannel, null);
  });

  group('NotificationListenerService', () {
    test('requestPermission returns true when permission is granted', () async {
      final result = await SkmNotificationListener.requestPermission();
      expect(result, isTrue);
    });

    test('requestPermission returns false and logs on PlatformException',
        () async {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(methodChannel,
              (MethodCall methodCall) async {
        if (methodCall.method == 'requestPermission') {
          throw PlatformException(code: 'PERMISSION_DENIED');
        }
        return null;
      });

      final result = await SkmNotificationListener.requestPermission();
      expect(result, isFalse);
    });

    test('isPermissionGranted returns true when permission is granted',
        () async {
      final result = await SkmNotificationListener.isPermissionGranted();
      expect(result, isTrue);
    });

    test('isPermissionGranted returns false and logs on PlatformException',
        () async {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(methodChannel,
              (MethodCall methodCall) async {
        if (methodCall.method == 'isPermissionGranted') {
          throw PlatformException(code: 'PERMISSION_DENIED');
        }
        return null;
      });

      final result = await SkmNotificationListener.isPermissionGranted();
      expect(result, isFalse);
    });
  });
}
