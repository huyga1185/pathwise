#!/usr/bin/env bash
# PathWise host monitor (T54, P-59; runbook docs/DEPLOYMENT.md §9). Run by cron every 5 minutes on the VPS:
#   */5 * * * * root /opt/pathwise-prod/monitor/pathwise-monitor.sh /opt/pathwise-prod >> /var/log/pathwise-monitor.log 2>&1
#
# Checks: disk usage, available memory, the containers of the compose project (stopped, restarting, unhealthy,
# restarted since the last run), the app's health endpoint, and the backup status files of compose.backup.yaml
# (T52). Problems are emailed through the SMTP server of the app (SPRING_MAIL_* in .env): once when they start, again
# every MONITOR_REMIND_HOURS while they last, and once when they are gone.
#
# Usage: pathwise-monitor.sh [--dry-run | --test-email] DIR
#   DIR           the environment directory (compose.prod.yaml, .env, backups/)
#   --dry-run     print the problems found, send nothing, change no state; exit 1 if there is any
#   --test-email  send one test email and exit
#
# Settings come from DIR/.env; a variable set in the environment wins (handy for a test with low thresholds):
#   MONITOR_ALERT_EMAIL            recipients, comma separated (required)
#   MONITOR_NAME                   name in the subject (default: the directory name)
#   MONITOR_DISK_PERCENT           alert above this disk usage (85)
#   MONITOR_MEM_AVAILABLE_PERCENT  alert below this share of available memory (10)
#   MONITOR_SERVICES               services that must be running (mariadb redis app)
#   MONITOR_HEALTH_URL             (http://127.0.0.1:${APP_PORT}/actuator/health); "none" skips the check
#   MONITOR_BACKUP_MAX_AGE_HOURS   alert when the last backup is older (26)
#   MONITOR_OFFSITE_MAX_AGE_HOURS  alert when the last copy to S3 is older, if BACKUP_S3_BUCKET is set (3)
#   MONITOR_REMIND_HOURS           repeat an ongoing problem after this many hours (6)
#   MONITOR_STATE_DIR              where the open problems are kept (DIR/.monitor)
#   MONITOR_FROM                   sender (MAIL_OTP_SENDER_ADDRESS)

set -uo pipefail

MODE=run
case "${1:-}" in
  --dry-run) MODE=dry-run; shift ;;
  --test-email) MODE=test-email; shift ;;
esac
if [[ $# -ne 1 || ! -d $1 ]]; then
  echo "usage: $0 [--dry-run | --test-email] DIR" >&2
  exit 2
fi
DIR=$(cd "$1" && pwd)
ENV_FILE="$DIR/.env"

# Value of a setting: the environment first, then the last NAME=value line of .env (never executed), then $2.
conf() {
  local name=$1 default=${2-} line value
  if [[ -n ${!name+x} ]]; then
    printf '%s' "${!name}"
    return
  fi
  line=$(grep -E "^[[:space:]]*(export[[:space:]]+)?${name}=" "$ENV_FILE" 2>/dev/null | tail -n 1)
  if [[ -z $line ]]; then
    printf '%s' "$default"
    return
  fi
  value=${line#*=}
  value=${value%$'\r'}
  if [[ $value =~ ^\"(.*)\"$ || $value =~ ^\'(.*)\'$ ]]; then value=${BASH_REMATCH[1]}; fi
  printf '%s' "$value"
}

NAME=$(conf MONITOR_NAME "$(basename "$DIR")")
DISK_PERCENT=$(conf MONITOR_DISK_PERCENT 85)
MEM_AVAILABLE_PERCENT=$(conf MONITOR_MEM_AVAILABLE_PERCENT 10)
SERVICES=$(conf MONITOR_SERVICES "mariadb redis app")
HEALTH_URL=$(conf MONITOR_HEALTH_URL "http://127.0.0.1:$(conf APP_PORT 8080)/actuator/health")
BACKUP_MAX_AGE_HOURS=$(conf MONITOR_BACKUP_MAX_AGE_HOURS 26)
OFFSITE_MAX_AGE_HOURS=$(conf MONITOR_OFFSITE_MAX_AGE_HOURS 3)
REMIND_HOURS=$(conf MONITOR_REMIND_HOURS 6)
STATE_DIR=$(conf MONITOR_STATE_DIR "$DIR/.monitor")
# Compose names the project after the directory: lower case, only letters, digits, - and _.
PROJECT=$(conf COMPOSE_PROJECT_NAME "$(basename "$DIR" | tr '[:upper:]' '[:lower:]' | tr -cd 'a-z0-9_-')")
NOW=$(date +%s)

# --- Problems found by this run: key → message. Events (a restart) are reported once and never kept open.
declare -A PROBLEMS=()
declare -A EVENTS=()
# Restart counts to remember (service → "container-id count"), written once the run has reported them.
declare -A RESTARTS=()
problem() { PROBLEMS[$1]=$2; }
event() { EVENTS[$1]=$2; }

check_disk() {
  local paths=("$DIR") root seen=" " path line used mount
  root=$(docker info -f '{{.DockerRootDir}}' 2>/dev/null) && [[ -n $root ]] && paths+=("$root")
  for path in "${paths[@]}"; do
    line=$(df -P "$path" 2>/dev/null | awk 'NR == 2 { sub("%", "", $5); print $5, $6 }') || continue
    [[ -z $line ]] && continue
    read -r used mount <<<"$line"
    [[ $seen == *" $mount "* ]] && continue
    seen+="$mount "
    if (( used > DISK_PERCENT )); then
      problem "disk${mount//\//_}" "Đĩa $mount đã dùng ${used}% (ngưỡng ${DISK_PERCENT}%)."
    fi
  done
}

check_memory() {
  local available_percent
  available_percent=$(awk '/^MemTotal:/ { t = $2 } /^MemAvailable:/ { a = $2 } END { if (t > 0) printf "%d", a * 100 / t }' /proc/meminfo)
  [[ -z $available_percent ]] && return
  if (( available_percent < MEM_AVAILABLE_PERCENT )); then
    problem memory "RAM khả dụng còn ${available_percent}% (ngưỡng ${MEM_AVAILABLE_PERCENT}%). Xem: docker stats --no-stream"
  fi
}

check_containers() {
  local ids line service status health exit_code restarts oom id name previous_id previous_restarts
  declare -A running=()
  if ! ids=$(docker ps -aq --filter "label=com.docker.compose.project=$PROJECT" \
      --filter "label=com.docker.compose.oneoff=False" 2>&1); then
    problem docker "Không đọc được trạng thái container: $ids"
    return
  fi
  mkdir -p "$STATE_DIR/restarts"
  if [[ -n $ids ]]; then
    # "index" and not .State.Health: a container without healthcheck has no such key, and Docker fails the template.
    # shellcheck disable=SC2086 # one id per word
    while IFS='|' read -r service status health exit_code restarts oom id name; do
      name=${name#/}
      case "$status" in
        running)
          running[$service]=1
          if [[ $health == unhealthy ]]; then
            problem "container_$service" "Container $name (service $service) đang unhealthy. Xem: docker logs --tail 100 $name"
          fi
          ;;
        restarting)
          problem "container_$service" "Container $name (service $service) đang khởi động lại liên tục. Xem: docker logs --tail 100 $name"
          ;;
        exited | dead)
          # A one-shot service (routing-data, osrm-prepare) ends with 0: that is its success.
          if [[ $exit_code != 0 ]]; then
            local why="mã thoát $exit_code"
            [[ $oom == true ]] && why+=", bị kill vì hết RAM"
            problem "container_$service" "Container $name (service $service) đã dừng ($why)."
          fi
          ;;
      esac
      # Docker restarted it (restart: unless-stopped) since the last run: a crash, even if it is up again.
      previous_id="" previous_restarts=0
      if [[ -f $STATE_DIR/restarts/$service ]]; then read -r previous_id previous_restarts <"$STATE_DIR/restarts/$service"; fi
      RESTARTS[$service]="$id $restarts"
      if [[ $id == "$previous_id" ]] && (( restarts > previous_restarts )); then
        event "restart_$service" "Container $name (service $service) đã tự khởi động lại $(( restarts - previous_restarts )) lần từ lần kiểm tra trước$([[ $oom == true ]] && echo ', bị kill vì hết RAM')."
      fi
    done < <(docker inspect -f '{{index .Config.Labels "com.docker.compose.service"}}|{{.State.Status}}|{{with index .State "Health"}}{{.Status}}{{end}}|{{.State.ExitCode}}|{{.RestartCount}}|{{.State.OOMKilled}}|{{.Id}}|{{.Name}}' $ids)
  fi
  for service in $SERVICES; do
    if [[ -z ${running[$service]+x} && -z ${PROBLEMS[container_$service]+x} ]]; then
      problem "container_$service" "Service $service của project $PROJECT không chạy."
    fi
  done
}

check_health() {
  [[ $HEALTH_URL == none ]] && return
  local body
  body=$(curl -sS --max-time 10 "$HEALTH_URL" 2>&1)
  if [[ $body != *'"status":"UP"'* ]]; then
    problem health "App không khoẻ: $HEALTH_URL trả: ${body:0:200}"
  fi
}

# Age in hours of a file, or nothing if it does not exist.
age_hours() { [[ -f $1 ]] && echo $(( (NOW - $(stat -c %Y "$1")) / 3600 )); }

check_backup() {
  local status="$DIR/backups/status" age
  # No backup service in this environment (compose.backup.yaml not used): nothing to check.
  [[ -d $status || -f $DIR/compose.backup.yaml ]] || return
  if [[ -f $status/last-error ]]; then
    problem backup "Backup gần nhất lỗi: $(head -c 300 "$status/last-error"). Xem: docker compose -f compose.prod.yaml -f compose.backup.yaml logs backup"
  fi
  age=$(age_hours "$status/last-success")
  if [[ -z $age ]]; then
    problem backup_age "Chưa có bản backup nào thành công ($status/last-success không có)."
  elif (( age >= BACKUP_MAX_AGE_HOURS )); then
    problem backup_age "Bản backup thành công gần nhất đã ${age} giờ (ngưỡng ${BACKUP_MAX_AGE_HOURS} giờ)."
  fi
  [[ -n $(conf BACKUP_S3_BUCKET) ]] || return
  if [[ -f $status/last-offsite-error ]]; then
    problem offsite "Chép backup lên S3 lỗi lúc $(head -c 100 "$status/last-offsite-error"). Xem: docker compose -f compose.prod.yaml -f compose.backup.yaml logs backup-offsite"
  fi
  age=$(age_hours "$status/last-offsite-success")
  if [[ -z $age ]]; then
    problem offsite_age "Chưa chép được backup nào lên S3."
  elif (( age >= OFFSITE_MAX_AGE_HOURS )); then
    problem offsite_age "Lần chép backup lên S3 thành công gần nhất đã ${age} giờ (ngưỡng ${OFFSITE_MAX_AGE_HOURS} giờ)."
  fi
}

# --- Email through the app's SMTP server (curl). The password goes to curl on stdin, never on its command line.
curl_quote() { local s=${1//\\/\\\\}; printf '"%s"' "${s//\"/\\\"}"; }

send_email() {
  local subject=$1 body=$2 host port user password from to url message rcpt
  host=$(conf SPRING_MAIL_HOST)
  port=$(conf SPRING_MAIL_PORT 587)
  user=$(conf SPRING_MAIL_USERNAME)
  password=$(conf SPRING_MAIL_PASSWORD)
  from=$(conf MONITOR_FROM "$(conf MAIL_OTP_SENDER_ADDRESS)")
  to=$(conf MONITOR_ALERT_EMAIL)
  if [[ -z $host || -z $from || -z $to ]]; then
    echo "cannot email: SPRING_MAIL_HOST, MONITOR_FROM/MAIL_OTP_SENDER_ADDRESS and MONITOR_ALERT_EMAIL are required" >&2
    return 1
  fi
  if [[ $port == 465 ]]; then url="smtps://$host:$port"; else url="smtp://$host:$port"; fi
  local args=(--silent --show-error --max-time 30 --crlf --url "$url" --mail-from "$from" --upload-file - -K /dev/fd/3)
  if [[ $(conf SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE) == true ]]; then args+=(--ssl-reqd); fi
  IFS=',' read -ra rcpt <<<"$to"
  for to in "${rcpt[@]}"; do
    to=$(echo "$to" | tr -d '[:space:]')
    [[ -n $to ]] && args+=(--mail-rcpt "$to")
  done
  message=$(printf 'From: PathWise monitor <%s>\nTo: %s\nSubject: =?UTF-8?B?%s?=\nDate: %s\nMIME-Version: 1.0\nContent-Type: text/plain; charset=UTF-8\nContent-Transfer-Encoding: 8bit\n\n%s\n' \
    "$from" "$(conf MONITOR_ALERT_EMAIL)" "$(printf '%s' "$subject" | base64 -w 0)" "$(date -R)" "$body")
  local config=""
  if [[ -n $user ]]; then config="user = $(curl_quote "$user:$password")"; fi
  printf '%s\n' "$message" | curl "${args[@]}" 3<<<"$config"
}

if [[ $MODE == test-email ]]; then
  if send_email "[PathWise $NAME] Thử cảnh báo" "Email thử của pathwise-monitor trên $(uname -n) lúc $(date '+%F %T %Z'). Nhận được email này là cảnh báo đã gửi được."; then
    echo "test email sent"
    exit 0
  fi
  exit 1
fi

mkdir -p "$STATE_DIR/open" || exit 1
if command -v flock >/dev/null; then
  exec 9>"$STATE_DIR/lock"
  flock -n 9 || { echo "another run is in progress"; exit 0; }
fi

check_disk
check_memory
check_containers
check_health
check_backup

if [[ $MODE == dry-run ]]; then
  for key in "${!PROBLEMS[@]}"; do echo "PROBLEM $key: ${PROBLEMS[$key]}"; done
  for key in "${!EVENTS[@]}"; do echo "EVENT $key: ${EVENTS[$key]}"; done
  (( ${#PROBLEMS[@]} + ${#EVENTS[@]} == 0 )) && { echo "OK"; exit 0; }
  exit 1
fi

# --- Compare with the problems already open: new, still there (remind), gone.
new="" ongoing="" resolved="" remind_keys=() new_keys=() resolved_keys=()
for key in "${!PROBLEMS[@]}"; do
  file="$STATE_DIR/open/$key"
  if [[ -f $file ]]; then
    read -r first_seen last_sent <"$file"
    if (( NOW - last_sent >= REMIND_HOURS * 3600 )); then
      ongoing+="- ${PROBLEMS[$key]} (từ $(date -d "@$first_seen" '+%F %H:%M'))"$'\n'
      remind_keys+=("$key")
    fi
  else
    new+="- ${PROBLEMS[$key]}"$'\n'
    new_keys+=("$key")
  fi
done
for key in "${!EVENTS[@]}"; do new+="- ${EVENTS[$key]}"$'\n'; done
for file in "$STATE_DIR"/open/*; do
  [[ -f $file ]] || continue
  key=$(basename "$file")
  if [[ -z ${PROBLEMS[$key]+x} ]]; then
    resolved+="- $(sed -n 2p "$file")"$'\n'
    resolved_keys+=("$key")
  fi
done

save_restarts() { for service in "${!RESTARTS[@]}"; do echo "${RESTARTS[$service]}" >"$STATE_DIR/restarts/$service"; done; }

if [[ -z $new$ongoing$resolved ]]; then save_restarts; exit 0; fi

if [[ -n $new ]]; then subject="[PathWise $NAME] $(( ${#new_keys[@]} + ${#EVENTS[@]} )) sự cố mới"
elif [[ -n $ongoing ]]; then subject="[PathWise $NAME] Vẫn còn sự cố"
else subject="[PathWise $NAME] Đã hết sự cố"; fi
body="Máy: $(uname -n), thư mục $DIR, lúc $(date '+%F %T %Z')."$'\n'
[[ -n $new ]] && body+=$'\nSự cố mới:\n'"$new"
[[ -n $ongoing ]] && body+=$'\nVẫn còn:\n'"$ongoing"
[[ -n $resolved ]] && body+=$'\nĐã hết:\n'"$resolved"
body+=$'\nHướng dẫn xử lý: docs/DEPLOYMENT.md §9 trong repo PathWise.'

if ! send_email "$subject" "$body"; then
  # State unchanged: the next run tries again.
  echo "$(date -Iseconds) alert email failed: $subject" >&2
  exit 1
fi
echo "$(date -Iseconds) alert sent: $subject"
save_restarts
for key in "${new_keys[@]}"; do printf '%s %s\n%s\n' "$NOW" "$NOW" "${PROBLEMS[$key]}" >"$STATE_DIR/open/$key"; done
for key in "${remind_keys[@]}"; do
  read -r first_seen _ <"$STATE_DIR/open/$key"
  printf '%s %s\n%s\n' "$first_seen" "$NOW" "${PROBLEMS[$key]}" >"$STATE_DIR/open/$key"
done
for key in "${resolved_keys[@]}"; do rm -f "$STATE_DIR/open/$key"; done
exit 0
