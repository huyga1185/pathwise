"""Seed a running stack with an OWNER who also drives and has today's route, for checking the driver app on an
emulator (docs/client/DRIVER_UI_REDESIGN.md §5).

Usage: python3 -I scripts/seed_driver_demo.py [api-base-url] [mailpit-api-url]
  defaults: http://localhost:8091/api/v1 http://localhost:8035/api/v1 (the e2e stack, compose.e2e.yaml)
Prints the driver's email, password and route id. Six stops in Đà Nẵng (one with a very long name and address), the
first delivered and the second failed. Standard library only; needs Mailpit to read the sign-up code.
"""
import json
import re
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime, timedelta, timezone

API = (sys.argv[1] if len(sys.argv) > 1 else 'http://localhost:8091/api/v1').rstrip('/')
MAILPIT = (sys.argv[2] if len(sys.argv) > 2 else 'http://localhost:8035/api/v1').rstrip('/')
RUN = format(int(time.time()), 'x')
EMAIL = f'taixe-{RUN}@pathwise.test'
PASSWORD = 'matkhau-taixe-1'


def call(method, path, body=None, token=None, base=API, expect=(200, 201, 204)):
    req = urllib.request.Request(base + path, method=method,
                                 data=None if body is None else json.dumps(body).encode())
    req.add_header('Content-Type', 'application/json')
    req.add_header('X-Client-Platform', 'mobile')
    if token:
        req.add_header('Authorization', f'Bearer {token}')
    try:
        with urllib.request.urlopen(req) as res:
            raw = res.read()
            status = res.status
    except urllib.error.HTTPError as err:
        raw, status = err.read(), err.code
    if status not in expect:
        raise SystemExit(f'{method} {path} -> {status} {raw[:400]!r}')
    return json.loads(raw) if raw else None


def otp(email):
    for _ in range(30):
        found = call('GET', f'/search?query=to:%22{email}%22', base=MAILPIT)
        if found['messages']:
            text = call('GET', f"/message/{found['messages'][0]['ID']}", base=MAILPIT)['Text']
            return re.search(r'\b(\d{6})\b', text).group(1)
        time.sleep(1)
    raise SystemExit('no OTP')


call('POST', '/auth/register', {'storeName': 'Tiệm Hoa Mai Đà Nẵng', 'email': EMAIL, 'password': PASSWORD})
call('POST', '/auth/email/verify', {'email': EMAIL, 'otp': otp(EMAIL)})
token = call('POST', '/auth/log-in', {'email': EMAIL, 'password': PASSWORD})['data']['accessToken']

depot = {'address': '30 Bạch Đằng, Hải Châu, Đà Nẵng', 'lat': 16.0711, 'lon': 108.2245}
call('PUT', '/store', {'name': 'Tiệm Hoa Mai Đà Nẵng', 'depot': depot, 'timezone': 'Asia/Ho_Chi_Minh'}, token)
call('PATCH', '/store/members/me/can-drive', {'canDrive': True}, token)
me = call('GET', '/users/me', token=token)['data']
call('PATCH', '/users/me', {'fullName': 'Nguyễn Văn An'}, token)
vehicle = call('POST', '/vehicles', {'name': 'Xe tải nhỏ', 'plateNumber': '43C-123.45', 'capacity': 40,
                                     'capacityUnit': 'BOX'}, token)['data']

stops = [
    ('Chị Lan — 2 hộp hoa', 'Nguyễn Thị Lan', '0905123456', '879 Hùng Vương, Hải Châu', 16.0668, 108.2120),
    ('Quán cà phê Gió', 'Trần Minh', '0905234567', '12 Lê Duẩn, Hải Châu', 16.0716, 108.2179),
    ('Văn phòng Công ty TNHH Thương mại Dịch vụ và Xuất nhập khẩu Hoàng Gia Phát (tầng 7, toà nhà Vĩnh Trung Plaza)',
     'Phạm Thị Thanh Hương', '0905345678', '255–257 Hùng Vương, phường Vĩnh Trung, quận Thanh Khê, thành phố Đà Nẵng',
     16.0670, 108.2085),
    ('Anh Bảo', 'Lê Bảo', '0905456789', '45 Nguyễn Văn Linh, Thanh Khê', 16.0608, 108.2116),
    ('Tiệm bánh Mây', 'Võ Thu', '0905567890', '101 Hàm Nghi, Thanh Khê', 16.0649, 108.2056),
    ('Cô Hạnh', 'Đỗ Hạnh', '0905678901', '22 Ông Ích Khiêm, Hải Châu', 16.0697, 108.2140),
]
today = (datetime.now(timezone.utc) + timedelta(hours=7)).date().isoformat()
plan = call('POST', '/plans', {
    'name': 'Giao hoa buổi sáng', 'planDate': today, 'depot': depot,
    'stops': [{'label': l, 'recipientName': n, 'recipientPhone': p, 'address': a, 'lat': la, 'lon': lo,
               'demands': {'BOX': 2}, 'serviceTimeSeconds': 300, 'note': 'Gọi trước khi tới 5 phút' if i == 0 else None}
              for i, (l, n, p, a, la, lo) in enumerate(stops)],
    'assignments': [{'driverId': me['id'], 'vehicleId': vehicle['id'], 'shiftStart': '07:00', 'shiftEnd': '18:00'}],
}, token)['data']

job = call('POST', f"/plans/{plan['id']}/optimize", {'version': plan['version']}, token, expect=(200, 201, 202))['data']
for _ in range(60):
    job = call('GET', f"/jobs/{job['id']}", token=token)['data']
    if job['status'] in ('SUCCEEDED', 'FAILED'):
        break
    time.sleep(1)
if job['status'] != 'SUCCEEDED':
    raise SystemExit(f'optimize: {job}')
plan = call('GET', f"/plans/{plan['id']}", token=token)['data']
plan = call('POST', f"/plans/{plan['id']}/publish", {'version': plan['version']}, token)['data']

token = call('POST', '/auth/log-in', {'email': EMAIL, 'password': PASSWORD})['data']['accessToken']  # canDrive in the token
routes = call('GET', '/driver/routes', token=token)['data']
route = call('GET', f"/driver/routes/{routes[0]['routeId']}", token=token)['data']
first, second = route['stops'][0], route['stops'][1]
call('PATCH', f"/driver/routes/{route['routeId']}/stops/{first['id']}/status", {'status': 'DELIVERED'}, token)
call('PATCH', f"/driver/routes/{route['routeId']}/stops/{second['id']}/status",
     {'status': 'FAILED', 'failureReason': 'Khách không nghe máy'}, token)
print(json.dumps({'email': EMAIL, 'password': PASSWORD, 'routeId': route['routeId'], 'stops': len(route['stops'])}))
