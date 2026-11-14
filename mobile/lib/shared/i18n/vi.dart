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
  static String accountMenu(String name) => '$name — tài khoản và giao diện';
  static const appearance = 'Giao diện';
  // Location sharing (T32, P-54; prototype "Tài xế — chia sẻ vị trí").
  static const shareAskTitle = 'Chia sẻ vị trí cho tuyến này?';
  static String shareAskBody(String plan) =>
      'Trong lúc giao tuyến $plan, PathWise gửi vị trí của bạn cho điều phối viên của cửa hàng, kể cả khi bạn đang '
      'mở Google Maps để chỉ đường. App tự dừng khi tuyến xong hoặc bạn đăng xuất.';
  static const shareRemember = 'Ghi nhớ lựa chọn, không hỏi lại';
  static const shareRememberHint = 'Đổi lại trong menu tài khoản (chạm chữ viết tắt tên).';
  static const shareConfirm = 'Chia sẻ vị trí';
  static const shareDecline = 'Không chia sẻ';
  static const sharingOn = 'Đang chia sẻ vị trí với điều phối viên';
  static const sharingDeclinedTitle = 'Bạn đang không chia sẻ vị trí cho tuyến này.';
  static const sharingDeclinedBody = 'Điều phối viên thấy "tài xế tắt chia sẻ vị trí". Bật lại lúc nào cũng được.';
  static const sharingEnable = 'Bật chia sẻ vị trí';
  static const sharingDeniedTitle = 'Chưa chia sẻ vị trí.';
  static const sharingDeniedBody = 'Máy chưa cho PathWise dùng vị trí nên điều phối viên không thấy bạn trên bản đồ.';
  static const sharingDeniedForeverBody =
      'Máy chưa cho PathWise dùng vị trí nên điều phối viên không thấy bạn trên bản đồ. Trong cài đặt, cho phép '
      'PathWise dùng vị trí.';
  static const openSettings = 'Mở cài đặt';
  static const gpsOffTitle = 'Định vị (GPS) đang tắt.';
  static const gpsOffBody = 'Bật định vị để điều phối viên thấy bạn trên bản đồ.';
  static const gpsTurnOn = 'Bật định vị';
  static const sharingNotificationTitle = 'Đang chia sẻ vị trí';
  static String sharingNotificationText(String plan) => 'Tuyến $plan. Tự dừng khi tuyến xong hoặc bạn đăng xuất.';
  static const sharingSetting = 'Chia sẻ vị trí khi giao';
  static const shareChoiceAsk = 'Hỏi khi bắt đầu mỗi tuyến';
  static const shareChoiceAskHint = 'Mặc định';
  static const shareChoiceAlways = 'Luôn chia sẻ';
  static const shareChoiceAlwaysHint = 'Không hỏi, tự bật khi mở tuyến đang giao';
  static const shareChoiceNever = 'Không chia sẻ';
  static const shareChoiceNeverHint = 'Không hỏi; bật riêng từng tuyến ở chi tiết tuyến';
  static const themeSystem = 'Theo máy';
  static const themeLight = 'Sáng';
  static const themeDark = 'Tối';

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
  // T56, W11: the store is locked by an ADMIN; the driver still delivers.
  static const storeLocked = 'Cửa hàng đang tạm khoá';
  static String storeLockReason(String reason) => 'Lý do: $reason';
  static const storeLockedDriver = 'Bạn vẫn giao và báo kết quả tuyến bình thường.';

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

  // Dẫn đường (M05, P-57)
  static const navigationTitle = 'Dẫn đường';
  static const navigationNotificationTitle = 'PathWise đang dẫn đường';
  static const navigationNotificationText = 'Đang dẫn đường tới điểm giao. Tự dừng khi bạn rời màn dẫn đường.';
  static const waitingForGps = 'Đang lấy vị trí của bạn…';
  static const findingRoute = 'Đang tìm đường…';
  static const rerouting = 'Đang tìm đường mới…';
  static const routeEnd = 'Điểm kết thúc tuyến';
  static String arrivedAt(String place) => 'Bạn đã tới $place';
  static String remaining(String distance, String duration, String arrival) =>
      '$distance · $duration · tới lúc $arrival';
  static String thenStep(String instruction) => 'Sau đó: $instruction';
  static const recenter = 'Về vị trí của tôi';
  static const navigationFailed = 'Không dẫn đường được lúc này.';
  static const navigationNoGps = 'Chưa lấy được vị trí của bạn.';
  static const navigationRateLimited = 'Tìm đường quá nhiều lần, thử lại sau ít phút.';
  static const navigationNeedsPermission = 'Cần quyền vị trí để dẫn đường.';
  static const navigationPermissionForever = 'Máy đang chặn quyền vị trí của PathWise. Mở cài đặt để cho phép.';
  static const navigationGpsOff = 'Định vị (GPS) đang tắt.';
  static const allowLocation = 'Cho phép dùng vị trí';
  static String openExternalMaps(bool apple) => apple ? 'Mở Apple Maps' : 'Mở Google Maps';
  static String instruction(String maneuver, {String? road, int? exit, String? place}) {
    final onto = road == null ? '' : ' vào $road';
    return switch (maneuver) {
      'depart' => road == null ? 'Bắt đầu đi' : 'Đi theo $road',
      'arrive' => place == null ? 'Tới nơi' : 'Tới $place',
      'straight' => 'Đi thẳng$onto',
      'slightLeft' => 'Chếch sang trái$onto',
      'slightRight' => 'Chếch sang phải$onto',
      'left' => 'Rẽ trái$onto',
      'right' => 'Rẽ phải$onto',
      'sharpLeft' => 'Rẽ gắt sang trái$onto',
      'sharpRight' => 'Rẽ gắt sang phải$onto',
      'uturn' => 'Quay đầu$onto',
      'keepLeft' => 'Đi về bên trái$onto',
      'keepRight' => 'Đi về bên phải$onto',
      'roundabout' => exit == null ? 'Vào vòng xoay$onto' : 'Vào vòng xoay, ra lối thứ $exit$onto',
      _ => 'Đi tiếp$onto',
    };
  }

  // Báo kết quả (M02)
  static const report = 'Báo kết quả';
  static String stopOf(int sequence, int count) => 'Điểm $sequence/$count';
  static const backToRoute = 'Về chi tiết tuyến';
  static const backLabel = 'Quay lại';
  static const callCustomer = 'Gọi khách';
  static String callNumber(String phone) => 'Gọi $phone';
  static String expectedAt(String time) => 'Dự kiến $time';
  static String receiveWindow(String window) => 'Khung nhận $window';
  static String expectedAtLower(String time) => 'dự kiến $time';
  static const noNextStop = 'Không còn điểm nào chờ giao.';
  static const delivered = 'Đã giao';
  static const failed = 'Không giao được';
  static const skipped = 'Bỏ qua';
  static String confirmDeliveredTitle(String label) => 'Xác nhận đã giao $label?';
  static const confirmDeliveredBody = 'Kết quả gửi về cho điều phối viên ngay và không sửa được sau khi gửi.';
  static String confirmSkippedTitle(String label) => 'Bỏ qua $label?';
  static const confirmSkippedBody =
      'Điểm này sẽ không được giao trong tuyến hôm nay. Kết quả không sửa được sau khi gửi.';
  static String failedTitle(String label) => 'Không giao được $label';
  static const failedHelp = 'Chọn nhanh hoặc ghi lý do. Kết quả không sửa được sau khi gửi.';
  static const reasonChips = 'Lý do thường gặp';
  static const commonReasons = ['Vắng nhà', 'Sai địa chỉ', 'Khách hẹn ngày khác', 'Khách từ chối nhận'];
  static const reason = 'Lý do';
  static String reasonCount(int length) => '$length/255';
  static const sendResult = 'Gửi kết quả';
  static String sentDelivered(String label) => 'Đã báo giao thành công $label';
  static String sentFailed(String label) => 'Đã báo không giao được $label';
  static String sentSkipped(String label) => 'Đã bỏ qua $label';
  static String reasonWas(String reason) => 'Lý do: $reason';
  static const goNext = 'Sang điểm tiếp theo';
  static const notSentOffline = 'Chưa gửi được: mất kết nối.';
  static const notSent = 'Chưa gửi được.';
  static const waitAndResend = 'Chờ có sóng rồi bấm Gửi lại; gửi lại cùng kết quả không bị trùng.';
  static const resend = 'Gửi lại';
  static const planNotActiveDetail = 'Không báo kết quả được nữa. Liên hệ điều phối viên nếu cần.';
  static const reportFailed = 'Không gửi được kết quả.';
  static const routeGone = 'Tuyến này không còn.';
  static const routeGoneDetail = 'Liên hệ điều phối viên nếu cần.';
  static const stopDelivering = 'Dừng giao và liên hệ điều phối viên.';
  static const stopNotFound = 'Không tìm thấy điểm giao này trong tuyến.';
}
