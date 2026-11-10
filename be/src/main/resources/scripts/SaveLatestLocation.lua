-- Keeps only the newest position of a driver (P-15, D-26).
-- KEYS[1] = location key, a hash: t = recordedAt of the stored point (epoch ms), v = the point as JSON.
-- ARGV[1] = recordedAt of the new point (epoch ms), ARGV[2] = the new point as JSON, ARGV[3] = TTL (ms).
-- Returns 1 if the point was stored, 0 if the stored point is as new or newer (nothing changes).
local stored = redis.call('HGET', KEYS[1], 't')
if stored and tonumber(stored) >= tonumber(ARGV[1]) then
    return 0
end
redis.call('HSET', KEYS[1], 't', ARGV[1], 'v', ARGV[2])
redis.call('PEXPIRE', KEYS[1], ARGV[3])
return 1
