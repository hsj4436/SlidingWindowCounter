local key = KEYS[1] -- instance-id
local request_limit = tonumber(ARGV[1])
local current_time = tonumber(ARGV[2]) -- currentTime in seconds
local current_window = tonumber(ARGV[3]) -- currentTime without seconds, e.g. 23:59:59 > 23:59:00

local current_key = key .. ":current"
local previous_key = key .. ":previous"

local r_current_window = tonumber(redis.call("HGET", current_key, "window") or "0")
local r_current_request_count = tonumber(redis.call("HGET", current_key, "count") or "0")

if current_window >= r_current_window + 120 then
    redis.call("HMSET", previous_key, "window", current_window - 60, "count", 0)
    redis.call("HMSET", current_key, "window", current_window, "count", 0)
elseif current_window == r_current_window + 60 then
    redis.call("HMSET", previous_key, "window", r_current_window, "count", r_current_request_count)
    redis.call("HMSET", current_key, "window", current_window, "count", 0)
end

r_current_request_count = tonumber(redis.call("HGET", current_key, "count") or "0")
local r_previous_request_count = tonumber(redis.call("HGET", previous_key, "count") or "0")

if r_current_request_count + (r_previous_request_count * (60 - (current_time - current_window)) / 60) + 1 <= request_limit then
    return redis.call("HINCRBY", current_key, "count", 1)
else
    return -1
end