-- KEYS[1] = otp:{purpose}:{userId}, value = sha256 (64 hex chars) followed by the number of wrong attempts.
-- ARGV[1] = sha256 of the submitted OTP, ARGV[2] = max wrong attempts.
-- Returns 1 when the OTP matched (and is deleted: single use), 0 otherwise.
local key = KEYS[1]
local stored = redis.call('GET', key)
if stored == false then
    return 0
end

local hash = string.sub(stored, 1, 64)
local attempts = tonumber(string.sub(stored, 65)) or 0
local maxAttempts = tonumber(ARGV[2])

if hash == ARGV[1] then
    redis.call('DEL', key)
    return 1
end

attempts = attempts + 1
if attempts >= maxAttempts then
    -- Wrong too many times: the OTP is cancelled, a new one must be requested.
    redis.call('DEL', key)
else
    redis.call('SET', key, hash .. tostring(attempts), 'KEEPTTL')
end
return 0
