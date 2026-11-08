import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'fake_api.dart';

/// `driver-route-detail.json` (route 64, 12 PENDING stops), with [finished] stops set by sequence and the route status.
Map<String, Object?> sampleRoute({Map<int, (String, String?)> finished = const {}, String? status}) {
  final route = {...sample('driver-route-detail')['data']! as Map<String, Object?>};
  route['stops'] = [
    for (final stop in route['stops']! as List<Object?>)
      {
        ...stop! as Map<String, Object?>,
        if (finished[(stop as Map<String, Object?>)['sequence']] case (final status, final reason)) ...{
          'status': status,
          'failureReason': reason,
        },
      },
  ];
  if (status != null) route['status'] = status;
  return route;
}

/// Opens [location] like a deep link would.
Future<void> goTo(WidgetTester tester, String location) async {
  GoRouter.of(tester.element(find.byType(Scaffold).first)).go(location);
  await tester.pumpAndSettle();
}
