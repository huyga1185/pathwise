import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../shared/i18n/vi.dart';
import '../../shared/theme/theme.dart';
import '../../shared/theme/tokens.g.dart';
import '../../shared/ui/ui.dart';

/// The server's limit of `failureReason` (API_SPEC §8, `INVALID_FAILURE_REASON_LENGTH`).
const maxFailureReason = 255;

/// "Không giao được {label}": a Material 3 modal bottom sheet (chủ dự án chọn, Q3) with the reason field, quick reasons
/// as suggestion chips below it (M3: suggestion chips *"typically appear beneath an input field"*), required, at most
/// 255. The sheet rises above the keyboard. Resolves to the trimmed reason, or null when the driver goes back.
Future<String?> showFailureReasonSheet(BuildContext context, {required String label}) => showModalBottomSheet<String>(
  context: context,
  isScrollControlled: true,
  useSafeArea: true,
  showDragHandle: true,
  builder: (context) => _FailureReasonSheet(label: label),
);

class _FailureReasonSheet extends StatefulWidget {
  const _FailureReasonSheet({required this.label});

  final String label;

  @override
  State<_FailureReasonSheet> createState() => _FailureReasonSheetState();
}

class _FailureReasonSheetState extends State<_FailureReasonSheet> {
  final _reason = TextEditingController();

  @override
  void initState() {
    super.initState();
    _reason.addListener(() => setState(() {}));
  }

  @override
  void dispose() {
    _reason.dispose();
    super.dispose();
  }

  void _pick(String reason) => _reason.value = TextEditingValue(
    text: reason,
    selection: TextSelection.collapsed(offset: reason.length),
  );

  @override
  Widget build(BuildContext context) {
    final t = context.pw;
    final reason = _reason.text.trim();
    OutlineInputBorder border(Color colour, double width) => OutlineInputBorder(
      borderRadius: BorderRadius.circular(PwRadius.radiusSm),
      borderSide: BorderSide(color: colour, width: width),
    );
    return Padding(
      // Above the keyboard: the field, the chips and "Gửi kết quả" stay in view while typing.
      padding: EdgeInsets.only(bottom: MediaQuery.viewInsetsOf(context).bottom),
      // Clear of the gesture bar when the keyboard is down ("Gửi kết quả" sat on it, D5).
      child: SafeArea(
        top: false,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Flexible(
              child: SingleChildScrollView(
                padding: const EdgeInsets.fromLTRB(PwSpacing.space4, 0, PwSpacing.space4, PwSpacing.space2),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Semantics(
                      header: true,
                      child: Text(Vi.failedTitle(widget.label), style: PwTypeStyles.title.copyWith(color: t.ink)),
                    ),
                    const SizedBox(height: PwSpacing.space2),
                    Text(Vi.failedHelp, style: PwTypeStyles.bodyTouch.copyWith(color: t.ink)),
                    const SizedBox(height: PwSpacing.space4),
                    Text.rich(
                      TextSpan(
                        text: Vi.reason,
                        children: [
                          TextSpan(
                            text: ' *',
                            style: TextStyle(color: t.danger),
                          ),
                        ],
                      ),
                      style: PwTypeStyles.heading.copyWith(color: t.ink),
                    ),
                    const SizedBox(height: PwSpacing.space1),
                    Semantics(
                      label: Vi.reason,
                      child: TextField(
                        controller: _reason,
                        minLines: 3,
                        maxLines: 5,
                        keyboardType: TextInputType.multiline,
                        inputFormatters: const [_Utf16LengthLimit(maxFailureReason)],
                        style: PwTypeStyles.bodyTouch.copyWith(color: t.ink),
                        decoration: InputDecoration(
                          filled: true,
                          fillColor: t.surfaceRaised,
                          contentPadding: const EdgeInsets.all(PwSpacing.space2),
                          enabledBorder: border(t.inkMuted, 1),
                          focusedBorder: border(t.route, 2),
                        ),
                      ),
                    ),
                    const SizedBox(height: PwSpacing.space1),
                    Text(
                      Vi.reasonCount(_reason.text.length),
                      textAlign: TextAlign.right,
                      style: PwTypeStyles.small.copyWith(color: t.inkMuted),
                    ),
                    const SizedBox(height: PwSpacing.space2),
                    Semantics(
                      container: true,
                      label: Vi.reasonChips,
                      child: Wrap(
                        spacing: PwSpacing.space2,
                        runSpacing: PwSpacing.space2,
                        children: [
                          for (final quick in Vi.commonReasons)
                            // Suggestion chip (M3; Flutter's ActionChip): fills the field, which the driver may then edit.
                            ActionChip(
                              label: Text(quick, style: PwTypeStyles.bodyTouch.copyWith(color: t.ink)),
                              onPressed: () => _pick(quick),
                            ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
            // Outside the scrolling part: the actions stay in view above the keyboard while the driver types (on the
            // emulator "Gửi kết quả" was under the keyboard, D5). As in the dialogs (showPwDialog): at the end,
            // stacked at the end when they do not fit.
            Padding(
              padding: const EdgeInsets.fromLTRB(
                PwSpacing.space4,
                PwSpacing.space2,
                PwSpacing.space4,
                PwSpacing.space4,
              ),
              child: OverflowBar(
                alignment: MainAxisAlignment.end,
                overflowAlignment: OverflowBarAlignment.end,
                spacing: PwSpacing.space2,
                overflowSpacing: PwSpacing.space2,
                children: [
                  PwButton(
                    label: Vi.backLabel,
                    variant: PwButtonVariant.text,
                    onPressed: () => Navigator.of(context).pop(),
                  ),
                  PwButton(
                    label: Vi.sendResult,
                    variant: PwButtonVariant.danger,
                    onPressed: reason.isEmpty ? null : () => Navigator.of(context).pop(reason),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// Caps the text at [max] UTF-16 code units, the way the server counts (`@Size` on a Java String). Flutter's own
/// limit counts characters, and a Vietnamese letter typed with combining marks is several code units.
class _Utf16LengthLimit extends TextInputFormatter {
  const _Utf16LengthLimit(this.max);

  final int max;

  @override
  TextEditingValue formatEditUpdate(TextEditingValue oldValue, TextEditingValue newValue) {
    if (newValue.text.length <= max) return newValue;
    if (oldValue.text.length >= max) return oldValue;
    var text = newValue.text.substring(0, max);
    // Never keep half of a surrogate pair.
    if (text.isNotEmpty && text.codeUnitAt(text.length - 1) & 0xFC00 == 0xD800) text = text.substring(0, max - 1);
    return TextEditingValue(
      text: text,
      selection: TextSelection.collapsed(offset: text.length),
    );
  }
}
