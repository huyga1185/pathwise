import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../shared/i18n/vi.dart';
import '../../shared/theme/theme.dart';
import '../../shared/theme/tokens.g.dart';
import '../../shared/ui/ui.dart';

/// The server's limit of `failureReason` (API_SPEC §8, `INVALID_FAILURE_REASON_LENGTH`).
const maxFailureReason = 255;

/// "Không giao được {label}" (prototype "Tài xế — báo kết quả"): quick reasons or a typed one, required, at most 255.
/// Resolves to the trimmed reason, or null when the driver goes back.
Future<String?> showFailureReasonDialog(BuildContext context, {required String label}) => showDialog<String>(
  context: context,
  builder: (context) => _FailureReasonDialog(label: label),
);

class _FailureReasonDialog extends StatefulWidget {
  const _FailureReasonDialog({required this.label});

  final String label;

  @override
  State<_FailureReasonDialog> createState() => _FailureReasonDialogState();
}

class _FailureReasonDialogState extends State<_FailureReasonDialog> {
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
    return AlertDialog(
      backgroundColor: t.surfaceRaised,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(PwRadius.radiusLg)),
      title: Text(Vi.failedTitle(widget.label), style: PwTypeStyles.title.copyWith(color: t.ink)),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(Vi.failedHelp, style: PwTypeStyles.bodyTouch.copyWith(color: t.ink)),
            const SizedBox(height: PwSpacing.space4),
            Semantics(
              container: true,
              label: Vi.reasonChips,
              child: Wrap(
                spacing: PwSpacing.space2,
                runSpacing: PwSpacing.space2,
                children: [
                  for (final quick in Vi.commonReasons)
                    _ReasonChip(text: quick, selected: reason == quick, onTap: _pick),
                ],
              ),
            ),
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
          ],
        ),
      ),
      actions: [
        PwButton(label: Vi.backLabel, onPressed: () => Navigator.of(context).pop()),
        PwButton(
          label: Vi.sendResult,
          variant: PwButtonVariant.danger,
          onPressed: reason.isEmpty ? null : () => Navigator.of(context).pop(reason),
        ),
      ],
    );
  }
}

class _ReasonChip extends StatelessWidget {
  const _ReasonChip({required this.text, required this.selected, required this.onTap});

  final String text;
  final bool selected;
  final ValueChanged<String> onTap;

  @override
  Widget build(BuildContext context) {
    final t = context.pw;
    return Semantics(
      button: true,
      selected: selected,
      child: Material(
        color: selected ? t.surfaceTint : t.surfaceRaised,
        shape: StadiumBorder(side: BorderSide(color: selected ? t.route : t.inkMuted)),
        child: InkWell(
          customBorder: const StadiumBorder(),
          onTap: () => onTap(text),
          child: ConstrainedBox(
            constraints: const BoxConstraints(minHeight: 48),
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: PwSpacing.space4, vertical: PwSpacing.space2),
              child: Center(
                widthFactor: 1,
                heightFactor: 1,
                child: Text(
                  text,
                  style: PwTypeStyles.bodyTouch.copyWith(
                    color: t.ink,
                    fontWeight: selected ? FontWeight.w600 : FontWeight.w400,
                  ),
                ),
              ),
            ),
          ),
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
