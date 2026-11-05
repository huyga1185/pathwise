import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/api_error.dart';
import '../../shared/errors/errors.dart';
import '../../shared/i18n/vi.dart';
import '../../shared/theme/theme.dart';
import '../../shared/theme/tokens.g.dart';
import '../../shared/ui/ui.dart';
import 'login_header.dart';
import 'session.dart';

final _emailPattern = RegExp(r'^[^\s@]+@[^\s@]+\.[^\s@]+$');

/// Prototype "Tài xế — đăng nhập" (SCREENS §2): errors of UX_BRIEF §6 at the top of the form, RATE_LIMITED locks the
/// button for `Retry-After`. "Quên mật khẩu" and "Nhập mã mời" open the web pages (M03).
class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _email = TextEditingController();
  final _password = TextEditingController();
  bool _showPassword = false;
  bool _busy = false;
  String? _emailError;
  String? _passwordError;
  Object? _failure;
  int _lockedSeconds = 0;
  Timer? _lock;

  @override
  void dispose() {
    _email.dispose();
    _password.dispose();
    _lock?.cancel();
    super.dispose();
  }

  Future<void> _submit() async {
    final email = _email.text.trim();
    setState(() {
      _emailError = email.isEmpty ? Vi.emailRequired : (_emailPattern.hasMatch(email) ? null : Vi.emailInvalid);
      _passwordError = _password.text.isEmpty ? Vi.passwordRequired : null;
      _failure = null;
    });
    if (_emailError != null || _passwordError != null) return;
    setState(() => _busy = true);
    try {
      // The router leaves this screen once the session is signed in.
      await ref.read(sessionProvider.notifier).logIn(email, _password.text);
    } on Object catch (error) {
      if (!mounted) return;
      setState(() => _failure = error);
      if (error is ApiError && error.code == 'RATE_LIMITED' && error.retryAfter != null) _startLock(error.retryAfter!);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  void _startLock(int seconds) {
    _lock?.cancel();
    setState(() => _lockedSeconds = seconds);
    _lock = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (!mounted) return timer.cancel();
      setState(() => _lockedSeconds -= 1);
      if (_lockedSeconds <= 0) timer.cancel();
    });
  }

  @override
  Widget build(BuildContext context) {
    final t = context.pw;
    final session = ref.watch(sessionProvider);
    final expired = session is SessionAnonymous && session.expired && _failure == null;
    final banner = _failure == null ? null : _split(messageFor(_failure!));
    return Scaffold(
      body: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const LoginHeader(),
            Padding(
              padding: const EdgeInsets.all(PwSpacing.space6),
              child: AutofillGroup(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Semantics(
                      header: true,
                      child: Text(Vi.loginTitle, style: PwTypeStyles.display.copyWith(color: t.ink, fontSize: 24)),
                    ),
                    const SizedBox(height: PwSpacing.space4),
                    if (expired) ...[
                      const ErrorBanner(title: Vi.sessionExpired),
                      const SizedBox(height: PwSpacing.space4),
                    ],
                    if (banner != null) ...[
                      ErrorBanner(title: banner.$1, detail: banner.$2),
                      const SizedBox(height: PwSpacing.space4),
                    ],
                    PwTextField(
                      label: Vi.email,
                      controller: _email,
                      required: true,
                      error: _emailError,
                      keyboardType: TextInputType.emailAddress,
                      textInputAction: TextInputAction.next,
                      autofillHints: const [AutofillHints.email],
                    ),
                    const SizedBox(height: PwSpacing.space4),
                    PwTextField(
                      label: Vi.password,
                      controller: _password,
                      required: true,
                      error: _passwordError,
                      obscureText: !_showPassword,
                      textInputAction: TextInputAction.done,
                      autofillHints: const [AutofillHints.password],
                      onSubmitted: (_) => unawaited(_submit()),
                      suffix: IconButton(
                        tooltip: _showPassword ? Vi.hidePassword : Vi.showPassword,
                        icon: Icon(_showPassword ? Icons.visibility_off_outlined : Icons.visibility_outlined),
                        color: t.inkMuted,
                        onPressed: () => setState(() => _showPassword = !_showPassword),
                      ),
                    ),
                    Align(
                      alignment: Alignment.centerRight,
                      child: PwButton(label: Vi.forgot, variant: PwButtonVariant.quiet, onPressed: () {}),
                    ),
                    PwButton(
                      label: _lockedSeconds > 0 ? Vi.retryIn(formatWait(_lockedSeconds)) : Vi.submit,
                      variant: PwButtonVariant.primary,
                      block: true,
                      busy: _busy,
                      onPressed: _lockedSeconds > 0 ? null : () => unawaited(_submit()),
                    ),
                    const SizedBox(height: PwSpacing.space6),
                    Wrap(
                      alignment: WrapAlignment.center,
                      crossAxisAlignment: WrapCrossAlignment.center,
                      children: [
                        Text(Vi.invited, style: PwTypeStyles.bodyTouch.copyWith(color: t.inkMuted)),
                        PwButton(label: Vi.enterInvite, variant: PwButtonVariant.quiet, onPressed: () {}),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// "Tài khoản đã bị vô hiệu hoá. Liên hệ chủ cửa hàng." → title and detail (prototype).
  (String, String?) _split(String text) {
    final cut = text.indexOf('. ');
    return cut > 0 ? (text.substring(0, cut + 1), text.substring(cut + 2)) : (text, null);
  }
}
