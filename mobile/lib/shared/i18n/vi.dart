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
  static const routesTitle = 'Tuyến hôm nay';
  static const routesSoon = 'Danh sách tuyến sẽ có ở bản tiếp theo (M01).';
  static String hello(String name) => 'Xin chào, $name';
}
