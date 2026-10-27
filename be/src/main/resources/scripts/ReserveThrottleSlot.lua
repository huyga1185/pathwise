-- Reserves the next free call slot of a throttle, first come first served.
-- KEYS[1] = throttle key; value = time (ms, Redis clock) at which the next slot becomes free.
-- ARGV[1] = interval between calls (ms), ARGV[2] = longest wait the caller accepts (ms).
-- Returns the wait (ms, >= 0) before the caller's slot, or -1 if that is longer than ARGV[2] (nothing is reserved).
local interval = tonumber(ARGV[1])
local maxWait = tonumber(ARGV[2])

-- Redis clock, so every app instance shares one time base.
local time = redis.call('TIME')
local now = tonumber(time[1]) * 1000 + math.floor(tonumber(time[2]) / 1000)

local nextFree = tonumber(redis.call('GET', KEYS[1]) or '0')
local slot = math.max(now, nextFree)
local wait = slot - now
if wait > maxWait then
    return -1
end

-- The key expires once the reserved slot is over, so an idle throttle leaves nothing behind.
redis.call('SET', KEYS[1], tostring(slot + interval), 'PX', wait + interval)
return wait
