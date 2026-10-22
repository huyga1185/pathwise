local requestOtpKey = KEYS[1]
local requestOtpValue = ARGV[1]

local otp = redis.call('GET', requestOtpKey)
-- Case: otp not found
if otp == false then
    return 0
end

local otpValue = string.sub(otp, 1, 64)
local otpTry = tonumber(string.sub(otp, 65))

if otpTry >= 3 then
    -- Case: otp try exceeded
    redis.call('DEL', requestOtpKey)
    return 0
else
    if otpValue == requestOtpValue then
        -- Case: otp matched
        redis.call('DEL', requestOtpKey)
        return 1
    else
        -- Case: otp not matched
        otpTry = otpTry + 1
        local newOtpTry = tostring(otpTry)
        local newOtp = otpValue .. newOtpTry
        redis.call('SET', requestOtpKey, newOtp, 'KEEPTTL')
        return 0
    end
end
