import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:pathwise_driver/platform/location.dart';

/// The positions plugin: one stream at a time, as geolocator on Android.
class FakePlugin {
  final opened = <({bool precise, String text})>[];
  StreamController<LocationFix>? current;

  Stream<LocationFix> positions({required bool precise, required String title, required String text}) {
    opened.add((precise: precise, text: text));
    final controller = StreamController<LocationFix>();
    controller.onCancel = () {
      if (current == controller) current = null;
    };
    current = controller;
    return controller.stream;
  }
}

LocationFix fix(double lat) => LocationFix(lat: lat, lon: 108.2, recordedAt: DateTime.utc(2026, 12));

void main() {
  test('nhiều người nghe dùng chung một luồng; dẫn đường mở lại luồng dày hơn, rời đi thì trở về', () async {
    final plugin = FakePlugin();
    final source = GeolocatorLocationSource(positions: plugin.positions);

    final sharing = <double>[];
    final sharingSub = source.watch(title: 'T', text: 'Chia sẻ').listen((f) => sharing.add(f.lat));
    await pumpEventQueue();
    expect(plugin.opened, [(precise: false, text: 'Chia sẻ')]);

    final navigation = <double>[];
    final navigationSub = source
        .watch(title: 'T', text: 'Dẫn đường', precise: true)
        .listen((f) => navigation.add(f.lat));
    await pumpEventQueue();
    // Reopened with the finer settings, keeping the notification of sharing.
    expect(plugin.opened.last, (precise: true, text: 'Chia sẻ'));

    plugin.current!.add(fix(1));
    await pumpEventQueue();
    expect(sharing, [1]);
    expect(navigation, [1]);

    await navigationSub.cancel();
    await pumpEventQueue();
    expect(plugin.opened.last, (precise: false, text: 'Chia sẻ'));
    plugin.current!.add(fix(2));
    await pumpEventQueue();
    expect(sharing, [1, 2]);
    expect(navigation, [1]);

    await sharingSub.cancel();
    await pumpEventQueue();
    expect(plugin.current, isNull, reason: 'nobody listens: the stream is closed');
    expect(plugin.opened, hasLength(3));
  });

  test('chỉ dẫn đường thì dùng thông báo của dẫn đường; lỗi của luồng tới mọi người nghe', () async {
    final plugin = FakePlugin();
    final source = GeolocatorLocationSource(positions: plugin.positions);
    final errors = <Object>[];
    final first = source.watch(title: 'T', text: 'Dẫn đường', precise: true).listen((_) {}, onError: errors.add);
    final second = source.watch(title: 'T', text: 'Dẫn đường', precise: true).listen((_) {}, onError: errors.add);
    await pumpEventQueue();
    expect(plugin.opened, [(precise: true, text: 'Dẫn đường')], reason: 'same settings: not reopened');

    plugin.current!.addError(const LocationUnavailable(LocationAccess.serviceOff));
    await pumpEventQueue();
    expect(errors, hasLength(2));
    await first.cancel();
    await second.cancel();
  });
}
