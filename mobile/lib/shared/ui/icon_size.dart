import 'package:flutter/widgets.dart';

/// An icon size that grows with the system text size, as the words beside it do (DRIVER_UI_REDESIGN §3 "Vùng chạm":
/// "icon theo cỡ chữ hệ thống"; at 200 % the day arrows stayed small, H6).
double scaledIconSize(BuildContext context, [double size = 24]) => MediaQuery.textScalerOf(context).scale(size);
