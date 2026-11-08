/// Port for "now" (ARCHITECTURE §2.1): screens ask the clock, tests pin it.
abstract interface class Clock {
  DateTime now();
}

class SystemClock implements Clock {
  const SystemClock();

  @override
  DateTime now() => DateTime.now().toUtc();
}
