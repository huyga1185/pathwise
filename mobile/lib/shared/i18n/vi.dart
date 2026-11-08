/// Interface strings of the driver app (ARCHITECTURE §6); error sentences live in shared/errors.
abstract final class Vi {
  static const appName = 'PathWise';
  static const appTagline = 'App giao hàng cho tài xế.';
  static const loginTitle = 'Đăng nhập';
  static const email = 'Email';
  static const password = 'Mật khẩu';
  static const showPassword = 'Hiện mật khẩu';
  static const hidePassword = 'Ẩn mật khẩu';
  static const forgot = 'Quên mật khẩu?';
  static const submit = 'Đăng nhập';
  static const invited = 'Được mời làm tài xế?';
  static const enterInvite = 'Nhập mã mời';
  static String retryIn(String wait) => 'Thử lại sau $wait';
  static const emailRequired = 'Vui lòng nhập email.';
  static const emailInvalid = 'Email không đúng định dạng.';
  static const passwordRequired = 'Vui lòng nhập mật khẩu.';
  static const sessionExpired = 'Phiên đăng nhập đã hết, vui lòng đăng nhập lại.';
  static const loading = 'Đang tải…';
  static const logout = 'Đăng xuất';
  static const noAccessTitle = 'Tài khoản này không giao hàng';
  static const noAccessLead =
      'App PathWise dành cho tài xế. Chủ cửa hàng và điều phối viên dùng trang web PathWise trên máy tính.';
  static const retry = 'Thử lại';
  static const logoutTitle = 'Đăng xuất khỏi app?';
  static const logoutBody = 'Bạn cần đăng nhập lại để xem tuyến của mình.';
  static String accountMenu(String name) => '$name — đăng xuất';

  // Tuyến hôm nay (M01)
  static const myRoutes = 'Tuyến của tôi';
  static const today = 'Hôm nay';
  static const yesterday = 'Hôm qua';
  static const tomorrow = 'Ngày mai';
  static const previousDay = 'Ngày trước';
  static const nextDay = 'Ngày sau';
  static const stopsDone = 'Điểm đã xong';
  static String plannedStart(String time) => 'Xuất phát dự kiến $time';
  static String stopCount(int count) => '$count điểm giao';
  static const startRoute = 'Bắt đầu tuyến';
  static const continueRoute = 'Tiếp tục giao';
  static const reviewRoute = 'Xem lại tuyến';
  static const noRoutesTitle = 'Chưa có tuyến nào';
  static const noRoutesLead = 'Tuyến hiện ở đây khi điều phối viên giao việc cho bạn. Kéo xuống để tải lại.';
  static const routesLoadFailed = 'Không tải được danh sách tuyến.';

  // Chi tiết tuyến (M01)
  static const backToRoutes = 'Về danh sách tuyến';
  static const nextStop = 'Điểm tiếp theo';
  static String restStops(int count) => 'Còn $count điểm sau đó';
  static String doneStops(int count) => 'Đã xong ($count)';
  static const collapse = 'Thu gọn';
  static const expand = 'Xem';
  static const directions = 'Chỉ đường';
  static const routeDone = 'Bạn đã xong tuyến này.';
  static String returnTo(String place) => 'Quay về kho: $place.';
  static String startAndEnd(String place) => 'Xuất phát và quay về: $place.';
  static String startThenEnd(String start, String end) => 'Xuất phát: $start. Quay về: $end.';
  static const noAddress = 'Không có địa chỉ, chỉ đường theo toạ độ';
  static const routeLoadFailed = 'Không tải được tuyến.';
  static const cannotOpen = 'Không mở được ứng dụng trên máy.';
}
