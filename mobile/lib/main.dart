import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:timezone/data/latest_10y.dart' as tz_data;

import 'app/app.dart';

void main() {
  // Planned times show in the plan's timezone (P-50); the database is bundled, nothing is fetched.
  tz_data.initializeTimeZones();
  runApp(const ProviderScope(child: PathwiseApp()));
}
