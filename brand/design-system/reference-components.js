/* @ds-bundle: {"format":4,"namespace":"PathWise","components":[{"name":"Button"},{"name":"TextField"},{"name":"StatusPill"},{"name":"ProgressBar"},{"name":"DataTable"},{"name":"StopCard"},{"name":"MapMarker"},{"name":"Dialog"},{"name":"Toast"}]} */
(function () {
  'use strict';
  var React = window.React;
  var h = React.createElement;

  function cx() {
    var out = [];
    for (var i = 0; i < arguments.length; i++) if (arguments[i]) out.push(arguments[i]);
    return out.join(' ');
  }
  function omit(obj, keys) {
    var out = {};
    for (var k in obj) if (Object.prototype.hasOwnProperty.call(obj, k) && keys.indexOf(k) < 0) out[k] = obj[k];
    return out;
  }

  /* ---------- helpers exported for clients ---------- */

  // "+84905123456" -> "0905 123 456". Anything else is returned unchanged.
  function formatPhone(e164) {
    if (typeof e164 !== 'string') return '';
    var m = /^\+84(\d{9})$/.exec(e164);
    if (!m) return e164;
    var d = '0' + m[1];
    return d.slice(0, 4) + ' ' + d.slice(4, 7) + ' ' + d.slice(7);
  }

  // API status -> label + tone. Mirrors the status table in the README.
  var STATUS = {
    plan: {
      DRAFT: ['NHÁP', 'muted'],
      PUBLISHED: ['ĐÃ GIAO VIỆC', 'info'],
      IN_PROGRESS: ['ĐANG GIAO', 'signal'],
      COMPLETED: ['HOÀN THÀNH', 'success'],
      CANCELLED: ['ĐÃ HUỶ', 'danger']
    },
    route: {
      PENDING: ['CHỜ GIAO', 'muted'],
      IN_PROGRESS: ['ĐANG GIAO', 'signal'],
      COMPLETED: ['HOÀN THÀNH', 'success']
    },
    stop: {
      PENDING: ['CHỜ GIAO', 'muted'],
      DELIVERED: ['ĐÃ GIAO', 'success'],
      FAILED: ['KHÔNG GIAO ĐƯỢC', 'danger'],
      SKIPPED: ['BỎ QUA', 'signal']
    },
    dropReason: {
      NOT_OPTIMIZED: ['CHƯA XẾP TUYẾN', 'muted'],
      INFEASIBLE: ['KHÔNG XẾP ĐƯỢC', 'signal']
    },
    geocode: {
      PENDING: ['ĐANG TÌM TOẠ ĐỘ', 'info'],
      FAILED: ['KHÔNG TÌM THẤY', 'danger']
    },
    processing: {
      GEOCODING: ['ĐANG TÌM TOẠ ĐỘ', 'info'],
      OPTIMIZING: ['ĐANG TỐI ƯU', 'info']
    },
    invitation: {
      PENDING: ['CHỜ NHẬN', 'info'],
      ACCEPTED: ['ĐÃ NHẬN', 'success'],
      REVOKED: ['ĐÃ THU HỒI', 'muted'],
      EXPIRED: ['HẾT HẠN', 'muted']
    },
    member: {
      PENDING_VERIFICATION: ['CHƯA XÁC THỰC', 'muted'],
      ACTIVE: ['ĐANG HOẠT ĐỘNG', 'success'],
      DISABLED: ['ĐÃ VÔ HIỆU HOÁ', 'danger']
    },
    vehicle: {
      ACTIVE: ['ĐANG DÙNG', 'success'],
      INACTIVE: ['ĐÃ TẮT', 'muted']
    },
    job: {
      QUEUED: ['ĐANG CHỜ', 'muted'],
      RUNNING: ['ĐANG CHẠY', 'info'],
      SUCCEEDED: ['XONG', 'success'],
      FAILED: ['LỖI', 'danger']
    },
    store: {
      ACTIVE: ['HOẠT ĐỘNG', 'success'],
      READ_ONLY: ['TẠM KHOÁ', 'signal']
    }
  };
  function statusInfo(kind, status) {
    var table = STATUS[kind];
    var hit = table && table[status];
    return hit ? { label: hit[0], tone: hit[1] } : null;
  }

  /* ---------- Button ---------- */

  function Button(props) {
    var variant = props.variant || 'secondary';
    var size = props.size || 'md';
    var rest = omit(props, ['variant', 'size', 'busy', 'block', 'className', 'children', 'type']);
    return h('button', Object.assign({}, rest, {
      type: props.type || 'button',
      disabled: props.disabled || props.busy,
      'aria-busy': props.busy ? 'true' : undefined,
      className: cx('pw-btn', 'pw-btn-' + variant, 'pw-btn-' + size, props.block && 'pw-btn-block', props.className)
    }), props.children);
  }

  /* ---------- TextField ---------- */

  function TextField(props) {
    var autoId = React.useId();
    var id = props.id || autoId;
    var hintId = id + '-hint';
    var errId = id + '-err';
    var described = [props.error ? errId : null, props.hint ? hintId : null].filter(Boolean).join(' ') || undefined;
    var Tag = props.multiline ? 'textarea' : 'input';
    var rest = omit(props, ['label', 'hint', 'error', 'multiline', 'className', 'id']);
    return h('div', { className: cx('pw-field', props.error && 'pw-field-error', props.className) },
      h('label', { className: 'pw-field-label', htmlFor: id }, props.label, props.required ? h('span', { 'aria-hidden': 'true', className: 'pw-field-req' }, ' *') : null),
      h(Tag, Object.assign({}, rest, {
        id: id,
        className: 'pw-field-input',
        'aria-invalid': props.error ? 'true' : undefined,
        'aria-describedby': described
      })),
      props.error ? h('p', { id: errId, className: 'pw-field-msg pw-field-msg-error' }, props.error) : null,
      props.hint ? h('p', { id: hintId, className: 'pw-field-msg' }, props.hint) : null
    );
  }

  /* ---------- StatusPill ---------- */

  function StatusPill(props) {
    var info = props.kind ? statusInfo(props.kind, props.status) : null;
    var label = props.children || (info && info.label);
    var tone = props.tone || (info && info.tone) || 'muted';
    if (!label) return null;
    return h('span', { className: cx('pw-pill', 'pw-pill-' + tone, props.className) }, label);
  }

  /* ---------- ProgressBar ---------- */

  function ProgressBar(props) {
    var known = typeof props.value === 'number' && typeof props.max === 'number' && props.max > 0;
    var pct = known ? Math.max(0, Math.min(100, (props.value / props.max) * 100)) : null;
    return h('div', { className: cx('pw-progress', props.className) },
      h('div', { className: 'pw-progress-head' },
        h('span', { className: 'pw-progress-label' }, props.label),
        known ? h('span', { className: 'pw-progress-count' }, props.value + '/' + props.max) : null
      ),
      h('div', {
        className: cx('pw-progress-track', !known && 'pw-progress-indeterminate'),
        role: 'progressbar',
        'aria-label': props.label,
        'aria-valuemin': known ? 0 : undefined,
        'aria-valuemax': known ? props.max : undefined,
        'aria-valuenow': known ? props.value : undefined
      }, h('div', { className: 'pw-progress-fill', style: known ? { width: pct + '%' } : undefined }))
    );
  }

  /* ---------- DataTable ---------- */

  function DataTable(props) {
    var columns = props.columns || [];
    var rows = props.rows || [];
    var keyOf = typeof props.rowKey === 'function' ? props.rowKey : function (r) { return r[props.rowKey || 'id']; };
    var clickable = typeof props.onRowClick === 'function';
    var body;
    if (rows.length === 0) {
      body = h('tr', null, h('td', { className: 'pw-table-empty', colSpan: columns.length }, props.empty || 'Chưa có dữ liệu.'));
    } else {
      body = rows.map(function (row) {
        var key = keyOf(row);
        var selected = props.selectedKey != null && key === props.selectedKey;
        return h('tr', {
          key: key,
          className: cx(selected && 'pw-row-selected', clickable && 'pw-row-clickable'),
          'aria-selected': clickable ? String(selected) : undefined,
          tabIndex: clickable ? 0 : undefined,
          onClick: clickable ? function () { props.onRowClick(row); } : undefined,
          onKeyDown: clickable ? function (e) { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); props.onRowClick(row); } } : undefined
        }, columns.map(function (col) {
          var content = col.render ? col.render(row) : row[col.key];
          return h('td', { key: col.key, className: cx(col.numeric && 'pw-num', col.align === 'end' && 'pw-end') }, content);
        }));
      });
    }
    return h('div', { className: cx('pw-table-wrap', props.className) },
      h('table', { className: 'pw-table' },
        props.caption ? h('caption', { className: 'pw-visually-hidden' }, props.caption) : null,
        h('thead', null, h('tr', null, columns.map(function (col) {
          return h('th', { key: col.key, scope: 'col', className: cx(col.numeric && 'pw-num', col.align === 'end' && 'pw-end'), style: col.width ? { width: col.width } : undefined }, col.header);
        }))),
        h('tbody', null, body)
      )
    );
  }

  /* ---------- StopCard ---------- */

  function StopCard(props) {
    var phone = props.phone ? formatPhone(props.phone) : null;
    return h('article', { className: cx('pw-stop', props.current && 'pw-stop-current', props.className), 'aria-current': props.current ? 'step' : undefined },
      h('div', { className: 'pw-stop-head' },
        props.sequence != null ? h('span', { className: 'pw-stop-seq', 'aria-label': 'Điểm số ' + props.sequence }, props.sequence) : null,
        h('h3', { className: 'pw-stop-title' }, props.title),
        props.status ? h(StatusPill, { kind: 'stop', status: props.status }) : null
      ),
      h('p', { className: 'pw-stop-address' }, props.address),
      (props.eta || props.timeWindow) ? h('p', { className: 'pw-stop-meta' },
        [props.eta ? 'Dự kiến ' + props.eta : null, props.timeWindow ? 'Khung ' + props.timeWindow : null].filter(Boolean).join(' · ')) : null,
      props.note ? h('p', { className: 'pw-stop-note' }, props.note) : null,
      props.failureReason ? h('p', { className: 'pw-stop-reason' }, 'Lý do: ' + props.failureReason) : null,
      (phone || props.actions) ? h('div', { className: 'pw-stop-actions' },
        phone ? h('a', { className: 'pw-btn pw-btn-secondary pw-btn-lg', href: 'tel:' + props.phone, 'aria-label': 'Gọi ' + phone }, 'Gọi ' + phone) : null,
        props.actions || null
      ) : null
    );
  }

  /* ---------- MapMarker ---------- */

  function MapMarker(props) {
    var tone = props.tone || 'route';
    var label = props.label || (props.sequence != null ? 'Điểm ' + props.sequence : 'Điểm giao');
    // data-theme="light": map tiles are light in every theme, so markers keep light-theme colours.
    return h('span', {
      'data-theme': 'light',
      role: 'img',
      'aria-label': label,
      className: cx('pw-marker', props.routeIndex != null ? 'pw-marker-r' + ((props.routeIndex % 7) + 1) : 'pw-marker-' + tone, props.selected && 'pw-marker-selected', props.className)
    }, props.sequence != null ? props.sequence : '');
  }

  /* ---------- Dialog ---------- */

  function Dialog(props) {
    var titleId = React.useId();
    var ref = React.useRef(null);
    React.useEffect(function () {
      if (!props.open) return undefined;
      var prev = document.activeElement;
      var node = ref.current;
      var first = node && node.querySelector('button, [href], input, textarea, select');
      if (first) first.focus();
      function onKey(e) { if (e.key === 'Escape' && props.onClose) props.onClose(); }
      document.addEventListener('keydown', onKey);
      return function () {
        document.removeEventListener('keydown', onKey);
        if (prev && prev.focus) prev.focus();
      };
    }, [props.open]);
    if (!props.open) return null;
    return h('div', { className: 'pw-dialog-backdrop' },
      h('div', { ref: ref, className: cx('pw-dialog', props.size && props.size !== 'md' && 'pw-dialog-' + props.size), role: props.tone === 'danger' ? 'alertdialog' : 'dialog', 'aria-modal': 'true', 'aria-labelledby': titleId },
        h('h2', { id: titleId, className: 'pw-dialog-title' }, props.title),
        h('div', { className: 'pw-dialog-body' }, props.children),
        h('div', { className: 'pw-dialog-actions' },
          h(Button, { variant: 'secondary', onClick: props.onClose }, props.cancelLabel || 'Quay lại'),
          h(Button, { variant: props.tone === 'danger' ? 'danger' : 'primary', onClick: props.onConfirm, busy: props.busy, disabled: props.confirmDisabled }, props.confirmLabel)
        )
      )
    );
  }

  /* ---------- Toast ---------- */

  function Toast(props) {
    var tone = props.tone || 'info';
    return h('div', { className: cx('pw-toast', 'pw-toast-' + tone, props.className), role: tone === 'danger' ? 'alert' : 'status' },
      h('span', { className: 'pw-toast-dot', 'aria-hidden': 'true' }),
      h('p', { className: 'pw-toast-msg' }, props.children),
      props.actionLabel ? h('button', { type: 'button', className: 'pw-toast-action', onClick: props.onAction }, props.actionLabel) : null,
      props.onClose ? h('button', { type: 'button', className: 'pw-toast-close', onClick: props.onClose, 'aria-label': 'Đóng thông báo' }, '×') : null
    );
  }

  var api = {
    Button: Button,
    TextField: TextField,
    StatusPill: StatusPill,
    ProgressBar: ProgressBar,
    DataTable: DataTable,
    StopCard: StopCard,
    MapMarker: MapMarker,
    Dialog: Dialog,
    Toast: Toast,
    formatPhone: formatPhone,
    statusInfo: statusInfo
  };
  window.PathWise = Object.assign(window.PathWise || {}, api);
})();
