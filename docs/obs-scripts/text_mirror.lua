--[[
text_mirror.lua

Copies the text of one OBS text source into one or more OTHER text sources,
so the same value can be shown twice (e.g. once left-aligned, once
right-aligned) without depending on OBS's "Paste (Reference)" - which only
truly shares a source across DIFFERENT scenes. Placed twice into the SAME
scene, OBS silently creates a detached copy instead, so it stops following
updates from YAPBS's OBS WebSocket connection. This script keeps a
real, independent target source in sync by polling and copying instead.

SETUP (one-time, in OBS):
  1. Tools > Scripts > "+" > select this file.
  2. In the script's own panel, fill in up to 12 pairs of:
       - Source text source   (the source YAPBS actually writes to,
                                e.g. "table1_name1")
       - Target text source   (a second, independent text source you added
                                to the scene, e.g. "table1_name1_right")
     Leave a pair's fields empty if you don't need it.
  3. Done - whenever the source text changes, the target is updated to match,
     several times per second.

You can position/scale/align the target source completely independently of
the source - only its TEXT content follows.
]]

obs = obslua

local PAIR_COUNT = 12
local POLL_INTERVAL_MS = 250

-- configurable via the script's OBS panel
local sources = {}
local targets = {}
for i = 1, PAIR_COUNT do
	sources[i] = ""
	targets[i] = ""
end

-- last-seen text per pair, so we only touch the target source on an actual change
local last_text = {}

local function current_text(source_name)
	if source_name == "" then return nil end
	local source = obs.obs_get_source_by_name(source_name)
	if source == nil then return nil end
	local settings = obs.obs_source_get_settings(source)
	local text = obs.obs_data_get_string(settings, "text")
	obs.obs_data_release(settings)
	obs.obs_source_release(source)
	return text
end

local function set_text(target_name, text)
	if target_name == "" then return end
	local source = obs.obs_get_source_by_name(target_name)
	if source == nil then return end
	local settings = obs.obs_data_create()
	obs.obs_data_set_string(settings, "text", text)
	obs.obs_source_update(source, settings)
	obs.obs_data_release(settings)
	obs.obs_source_release(source)
end

local function mirror(i)
	local source_name = sources[i]
	local target_name = targets[i]
	if source_name == "" or target_name == "" then return end

	local text = current_text(source_name)
	if text == nil or text == last_text[i] then return end
	last_text[i] = text

	set_text(target_name, text)
end

local function tick()
	for i = 1, PAIR_COUNT do
		mirror(i)
	end
end

-- ── OBS script lifecycle ────────────────────────────────────────────────────

function script_description()
	return "Copies the text of one source into another, several times per second - " ..
		"so a value like a player's name can be shown twice in one scene (e.g. once " ..
		"left-aligned, once right-aligned) with correct live updates."
end

function script_properties()
	local props = obs.obs_properties_create()
	for i = 1, PAIR_COUNT do
		obs.obs_properties_add_text(props, "source_" .. i, "Source text source " .. i, obs.OBS_TEXT_DEFAULT)
		obs.obs_properties_add_text(props, "target_" .. i, "Target text source " .. i, obs.OBS_TEXT_DEFAULT)
	end
	return props
end

function script_update(settings)
	for i = 1, PAIR_COUNT do
		sources[i] = obs.obs_data_get_string(settings, "source_" .. i)
		targets[i] = obs.obs_data_get_string(settings, "target_" .. i)
	end
	-- force a re-check on the next tick even if the source text itself hasn't changed
	last_text = {}
end

function script_load(settings)
	obs.timer_add(tick, POLL_INTERVAL_MS)
end

function script_unload()
	obs.timer_remove(tick)
end
