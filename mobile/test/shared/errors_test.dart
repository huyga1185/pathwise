import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:pathwise_driver/data/api_error.dart';
import 'package:pathwise_driver/shared/errors/errors.dart';
import 'package:pathwise_driver/shared/format.dart';

/// Rows of the code table of docs/ux/UX_BRIEF.md §6.1: code → sentence (notes in brackets after the sentence dropped).
Map<String, String> uxBriefSentences() {
  final text = File('../docs/ux/UX_BRIEF.md').readAsStringSync();
  final section = text.substring(text.indexOf('### 6.1'), text.indexOf('### 6.2'));
  final sentences = <String, String>{};
  for (final line in section.split('\n')) {
    final cells = line.split('|').map((cell) => cell.trim()).toList();
    if (cells.length < 4 || !cells[1].startsWith('`')) continue;
    final sentence = cells[2].replaceFirst(RegExp(r'\.\s+\(.*\)$'), '.');
    if (sentence.startsWith('(')) continue;
    for (final code in RegExp('`([A-Z_]+)`').allMatches(cells[1])) {
      sentences[code.group(1)!] = sentence;
    }
  }
  return sentences;
}

void main() {
  test('câu lỗi giống từng chữ bảng UX_BRIEF §6.1 (đổi câu thì sửa UX_BRIEF trước)', () {
    final expected = uxBriefSentences();
    expect(expected.length, greaterThan(30));
    for (final entry in expected.entries) {
      expect(errorMessages[entry.key], entry.value, reason: entry.key);
    }
  });

  test('messageFor điền Retry-After và mã tra cứu; mã lạ có câu chung; mất mạng có câu riêng', () {
    expect(
      messageFor(const ApiError(code: 'RATE_LIMITED', retryAfter: 252)),
      'Bạn thao tác quá nhiều lần. Thử lại sau 4 phút 12 giây.',
    );
    expect(messageFor(const ApiError(code: 'SOMETHING_NEW', requestId: 'abc')), 'Đã có lỗi xảy ra. Mã tra cứu: abc.');
    expect(
      messageFor(const ApiError(code: ApiError.networkError)),
      'Không kết nối được máy chủ. Kiểm tra mạng rồi thử lại.',
    );
  });

  test('formatWait và formatPhone', () {
    expect(formatWait(45), '45 giây');
    expect(formatWait(3600 + 300), '1 giờ 5 phút');
    expect(formatPhone('+84905123456'), '0905 123 456');
    expect(formatPhone('+14155550100'), '+14155550100');
  });
}
