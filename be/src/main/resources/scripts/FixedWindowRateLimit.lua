-- Counts one request in a fixed window.
-- KEYS[1] = counter key; ARGV[1] = window length (ms).
-- Returns {count in the current window including this request, ms until the window ends}.
local count = redis.call('INCR', KEYS[1])
if count == 1 then
    redis.call('PEXPIRE', KEYS[1], ARGV[1])
end
local ttl = redis.call('PTTL', KEYS[1])
-- A counter without expiry would block forever: give it one (every Redis key has a TTL).
if ttl < 0 then
    redis.call('PEXPIRE', KEYS[1], ARGV[1])
    ttl = tonumber(ARGV[1])
end
return {count, ttl}
