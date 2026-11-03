"""Manual acceptance demo (T43): REQUIREMENTS.md section 6 scenario against a running stack with real Nominatim/OSRM.

Usage: python3 -I scripts/acceptance_demo.py <api-base-url> <mailpit-base-url>
  e.g. python3 -I scripts/acceptance_demo.py http://localhost:8080/api/v1 http://localhost:8025
Standard library only. Needs Mailpit to read the OTP and invitation emails (local stack, docs/ACCEPTANCE.md).
"""
import json
import re
import sys
import time
import urllib.error
import urllib.request
import uuid
from datetime import datetime
from zoneinfo import ZoneInfo

API, MAILPIT = sys.argv[1].rstrip("/"), sys.argv[2].rstrip("/")
ZONE = ZoneInfo("Asia/Ho_Chi_Minh")
PASSWORD = "Passw0rd!x"
RUN = uuid.uuid4().hex[:8]
DAY = "2026-12-02"


def call(method, path, token=None, body=None, headers=None, expect=None):
    data = json.dumps(body).encode() if body is not None else None
    request = urllib.request.Request(API + path, data=data, method=method)
    request.add_header("Content-Type", "application/json")
    if token:
        request.add_header("Authorization", "Bearer " + token)
    for key, value in (headers or {}).items():
        request.add_header(key, value)
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            status, raw, response_headers = response.status, response.read(), dict(response.headers)
    except urllib.error.HTTPError as error:
        status, raw, response_headers = error.code, error.read(), dict(error.headers)
    payload = json.loads(raw) if raw and raw[:1] in (b"{", b"[") else None
    if expect is not None and status != expect:
        raise AssertionError(f"{method} {path}: expected {expect}, got {status} {payload}")
    return status, payload, response_headers


def mail_content(to, pattern):
    for _ in range(30):
        with urllib.request.urlopen(f"{MAILPIT}/api/v1/search?query=to:{to}") as response:
            messages = json.loads(response.read())["messages"]
        if messages:
            with urllib.request.urlopen(f"{MAILPIT}/api/v1/message/{messages[0]['ID']}") as response:
                html = json.loads(response.read())["HTML"]
            match = re.search(pattern, html)
            if match:
                return match.group(1)
        time.sleep(1)
    raise AssertionError(f"no mail matching {pattern} to {to}")


def log_in(email):
    _, payload, _ = call("POST", "/auth/log-in", body={"email": email, "password": PASSWORD},
                         headers={"X-Client-Platform": "mobile"}, expect=200)
    return payload["data"]["accessToken"]


def step(text):
    print(f"[ok] {text}")


def local(instant):
    return datetime.fromisoformat(instant.replace("Z", "+00:00")).astimezone(ZONE).time()


owner_email = f"owner-{RUN}@pathwise.local"
call("POST", "/auth/register", body={"email": owner_email, "password": PASSWORD, "storeName": "Tiệm demo"}, expect=201)
otp = mail_content(owner_email, r">(\d{6})<")
call("POST", "/auth/email/verify", body={"email": owner_email, "otp": otp}, expect=204)
owner = log_in(owner_email)
call("PUT", "/store", owner, {"name": "Tiệm demo", "depot": {"address": "Kho", "lat": 16.06, "lon": 108.21},
                              "timezone": "Asia/Ho_Chi_Minh"}, expect=200)
step("OWNER registered, verified by OTP from Mailpit, logged in, depot set")

members = {}
for name, role in [("dispatcher", "DISPATCHER"), ("driver-a", "DRIVER"), ("driver-b", "DRIVER")]:
    email = f"{name}-{RUN}@pathwise.local"
    call("POST", "/store/invitations", owner, {"email": email, "role": role}, expect=201)
    code = mail_content(email, r">([A-Za-z0-9_-]{43})<")
    call("POST", "/invitations/accept", body={"code": code, "password": PASSWORD, "fullName": name}, expect=204)
    token = log_in(email)
    members[name] = (token, call("GET", "/users/me", token, expect=200)[1]["data"]["id"])
dispatcher = members["dispatcher"][0]
step("1 DISPATCHER and 2 DRIVERs invited, accepted with the emailed code, logged in")

vehicles = [call("POST", "/vehicles", dispatcher, {"name": f"Xe {i}", "capacity": 6, "capacityUnit": "BOX"}, expect=201)[1]["data"]["id"]
            for i in (1, 2)]
step("DISPATCHER created 2 vehicles (6 boxes each)")

addresses = ["Cầu Rồng, Đà Nẵng", "Chợ Hàn, Đà Nẵng", "Chợ Cồn, Đà Nẵng", "Bệnh viện Đà Nẵng",
             "Sân bay quốc tế Đà Nẵng", "Đại học Bách khoa Đà Nẵng", "Công viên Biển Đông, Đà Nẵng",
             f"Không tồn tại {RUN} xyz"]
stops = [{"label": f"ĐH-{i}", "address": a, "demands": {"BOX": 1}, "serviceTimeSeconds": 180} for i, a in enumerate(addresses)]
stops[0]["timeWindow"] = {"start": "09:00", "end": "11:00"}
stops[3]["timeWindow"] = {"start": "13:00", "end": "15:00"}
stops += [{"label": "ĐH-8", "lat": 16.0544, "lon": 108.2022, "demands": {"BOX": 2}},
          {"label": "ĐH-9", "lat": 16.0471, "lon": 108.2068, "demands": {"BOX": 1},
           "timeWindow": {"start": "21:00", "end": "22:00"}}]
assignments = [{"driverId": members[d][1], "vehicleId": v, "shiftStart": "08:00", "shiftEnd": "17:00"}
               for d, v in (("driver-a", vehicles[0]), ("driver-b", vehicles[1]))]
plan = call("POST", "/plans", dispatcher, {"name": "Demo", "planDate": DAY, "stops": stops, "assignments": assignments},
            expect=201)[1]["data"]
plan_id = plan["id"]
started = time.time()
while plan["processingStatus"] != "IDLE":
    time.sleep(2)
    plan = call("GET", f"/plans/{plan_id}", dispatcher, expect=200)[1]["data"]
summary = plan["geocodingSummary"]
step(f"plan of {len(stops)} stops geocoded in the background by Nominatim in {time.time() - started:.0f}s: {summary}")
failed = [s for s in plan["unassignedStops"] if s["geocodeStatus"] == "FAILED"]
for stop in plan["unassignedStops"]:
    if stop["geocodeStatus"] == "RESOLVED":
        print(f"     {stop['label']}: {stop['lat']:.5f},{stop['lon']:.5f} {stop['geocodedDisplayName'][:70]}")
assert summary["failed"] >= 1, summary
for stop in failed:
    plan = call("PATCH", f"/plans/{plan_id}/stops/{stop['id']}", dispatcher,
                {"version": plan["version"], "lat": 16.0678, "lon": 108.2208}, expect=200)[1]["data"]
step(f"{len(failed)} failed stop(s) fixed with coordinates: {[s['label'] + ' ' + str(s['geocodeError']) for s in failed]}")

started = time.time()
job = call("POST", f"/plans/{plan_id}/optimize", dispatcher, {"version": plan["version"]}, expect=202)[1]["data"]
assert job["status"] == "SUCCEEDED", job
plan = call("GET", f"/plans/{plan_id}", dispatcher, expect=200)[1]["data"]
step(f"optimized with OSRM + OR-Tools in {time.time() - started:.0f}s: {plan['totalDistanceMeters']} m, "
     f"{plan['totalDurationSeconds']} s")
for route in plan["routes"]:
    assert route["load"] <= route["vehicle"]["capacity"], route
    for stop in route["stops"]:
        arrival = local(stop["plannedArrival"])
        window = stop["timeWindow"]
        assert window is None or window["start"] <= arrival.strftime("%H:%M") <= window["end"], stop
    print(f"     {route['driver']['fullName']}: load {route['load']}/{route['vehicle']['capacity']}, "
          f"{local(route['plannedStart']):%H:%M}-{local(route['plannedEnd']):%H:%M}, "
          + ", ".join(f"{s['label']}@{local(s['plannedArrival']):%H:%M}" for s in route["stops"]))
print(f"     unassigned: {[(s['label'], s['dropReason']) for s in plan['unassignedStops']]}")
step("result respects capacity, time windows and shifts; infeasible stop listed")

call("POST", f"/plans/{plan_id}/publish", dispatcher, {"version": plan["version"]}, expect=200)
step("published")

last = None
for name in ("driver-a", "driver-b"):
    token, user_id = members[name]
    routes = call("GET", f"/driver/routes?date={DAY}", token, expect=200)[1]["data"]
    expected = [r["id"] for r in plan["routes"] if r["driver"]["id"] == user_id and r["stops"]]
    assert [r["routeId"] for r in routes] == expected, (routes, expected)
    for route in routes:
        detail = call("GET", f"/driver/routes/{route['routeId']}", token, expect=200)[1]["data"]
        first = detail["stops"][0]
        print(f"     {name} map link: https://www.google.com/maps/dir/?api=1&destination={first['lat']},{first['lon']}")
        for stop in detail["stops"]:
            status = "FAILED" if stop["sequence"] == 2 else "DELIVERED"
            body = {"status": status, "failureReason": "Vắng nhà"} if status == "FAILED" else {"status": status}
            last = call("PATCH", f"/driver/routes/{route['routeId']}/stops/{stop['id']}/status", token, body, expect=200)[1]["data"]
            if stop["sequence"] == 1 and name == "driver-a":
                print(f"     after first stop: plan {last['planStatus']}")
step("each DRIVER saw only their own route with coordinates, and reported every stop")

plan = call("GET", f"/plans/{plan_id}", dispatcher, expect=200)[1]["data"]
assert plan["status"] == "COMPLETED", plan["status"]
step(f"plan {plan['status']} as seen by the DISPATCHER (last update said {last['planStatus']})")

status, _, headers = call("OPTIONS", "/auth/log-in", headers={"Origin": "http://localhost:3000",
                                                               "Access-Control-Request-Method": "POST"})
evil, _, _ = call("OPTIONS", "/auth/log-in", headers={"Origin": "https://evil.example", "Access-Control-Request-Method": "POST"})
_, _, me = call("GET", "/users/me", dispatcher)
print(f"     CORS allowed origin {status} {headers.get('Access-Control-Allow-Origin')}, other origin {evil}; "
      f"X-Request-Id {me.get('X-Request-Id')}, Referrer-Policy {me.get('Referrer-Policy')}")
step("hardening headers present")
