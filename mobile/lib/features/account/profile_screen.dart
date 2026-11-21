import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pathwise_api/pathwise_api.dart';

import '../../shared/errors/errors.dart';
import '../../shared/format.dart';
import '../../shared/i18n/vi.dart';
import '../../shared/theme/tokens.g.dart';
import '../../shared/ui/ui.dart';
import 'profile_data.dart';

/// "Thông tin của tôi" (M06, DRIVER_UI_REDESIGN D3): the driver's name and phone number, so a dispatcher can call them
/// from the tracking screen (P-53). Opened from the account sheet; saved → snackbar and back to the routes.
class ProfileScreen extends ConsumerWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final profile = ref.watch(myProfileProvider);
    return Scaffold(
      appBar: AppBar(title: const Text(Vi.myProfile)),
      body: switch (profile) {
        AsyncValue(:final value?) => _ProfileForm(user: value),
        AsyncValue(:final error?) => ListView(
          padding: const EdgeInsets.all(PwSpacing.space4),
          children: [
            ErrorBanner(title: Vi.profileLoadFailed, detail: messageFor(error)),
            const SizedBox(height: PwSpacing.space4),
            PwButton(label: Vi.retry, block: true, onPressed: () => ref.invalidate(myProfileProvider)),
          ],
        ),
        _ => const Center(child: CircularProgressIndicator(semanticsLabel: Vi.loading)),
      },
    );
  }
}

class _ProfileForm extends ConsumerStatefulWidget {
  const _ProfileForm({required this.user});

  final GetUserResponse user;

  @override
  ConsumerState<_ProfileForm> createState() => _ProfileFormState();
}

class _ProfileFormState extends ConsumerState<_ProfileForm> {
  late final String _initialName = widget.user.fullName ?? '';
  late final String _initialPhone = formatPhone(widget.user.phoneNumber);
  late final _name = TextEditingController(text: _initialName);
  late final _phone = TextEditingController(text: _initialPhone);
  Map<String, String> _fieldErrors = const {};
  Object? _failure;
  bool _saving = false;

  bool get _changed => _name.text != _initialName || _phone.text != _initialPhone;

  @override
  void dispose() {
    _name.dispose();
    _phone.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    FocusScope.of(context).unfocus();
    setState(() {
      _saving = true;
      _fieldErrors = const {};
      _failure = null;
    });
    try {
      await ref.read(profileUpdaterProvider).save(fullName: _name.text, phoneNumber: _phone.text);
      if (!mounted) return;
      // Material 3 snackbar for a short result (DRIVER_UI_REDESIGN §3); it stays on the screen below once this closes.
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text(Vi.profileSaved)));
      Navigator.of(context).pop();
    } on Object catch (error) {
      if (!mounted) return;
      final fields = fieldErrors(error, const {'fullName': Vi.fullName, 'phoneNumber': Vi.phone});
      setState(() {
        _saving = false;
        _fieldErrors = fields;
        _failure = fields.isEmpty ? error : null;
      });
    }
  }

  /// Back with unsaved changes asks first (predictive back, DRIVER_UI_REDESIGN §3 "Back": `PopScope` where a form is
  /// not saved).
  Future<void> _confirmLeave() async {
    final leave = await showPwDialog(
      context,
      title: Vi.discardTitle,
      body: Vi.discardBody,
      confirmLabel: Vi.discard,
      cancelLabel: Vi.keepEditing,
      danger: true,
    );
    if (leave && mounted) Navigator.of(context).pop();
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: !_changed || _saving,
      onPopInvokedWithResult: (didPop, _) {
        if (!didPop) unawaited(_confirmLeave());
      },
      child: ListView(
        padding: const EdgeInsets.fromLTRB(PwSpacing.space4, PwSpacing.space4, PwSpacing.space4, PwSpacing.space6),
        children: [
          if (_failure != null) ...[
            ErrorBanner(title: messageFor(_failure!)),
            const SizedBox(height: PwSpacing.space4),
          ],
          PwTextField(
            label: Vi.fullName,
            controller: _name,
            error: _fieldErrors['fullName'],
            textInputAction: TextInputAction.next,
            autofillHints: const [AutofillHints.name],
            onChanged: (_) => setState(() {}),
          ),
          const SizedBox(height: PwSpacing.space4),
          PwTextField(
            label: Vi.phone,
            controller: _phone,
            hint: Vi.phoneHint,
            placeholder: '0905 123 456',
            error: _fieldErrors['phoneNumber'],
            keyboardType: TextInputType.phone,
            textInputAction: TextInputAction.done,
            autofillHints: const [AutofillHints.telephoneNumber],
            onChanged: (_) => setState(() {}),
            onSubmitted: (_) => unawaited(_save()),
          ),
          const SizedBox(height: PwSpacing.space6),
          PwButton(
            label: Vi.save,
            variant: PwButtonVariant.filled,
            block: true,
            busy: _saving,
            onPressed: () => unawaited(_save()),
          ),
        ],
      ),
    );
  }
}
