--[[
club_crest_switcher.lua

Watches two text sources (the club names shown by YAPBS's OBS LiveScore
endpoints, e.g. "club1"/"club2") and switches two local image sources to the
matching crest file whenever the text changes.

Why this exists: the app used to also push crest images over the network as
binary data. That's no longer necessary - the club logos never have to leave
the phone at all. This script matches purely on the club NAME text (which the
app already sends as plain text) against .png files an OBS operator drops into
a local folder, once, per club. No image transfer, no per-session setup.

SETUP (one-time, in OBS):
  1. Tools > Scripts > "+" > select this file.
  2. In the script's own panel, fill in:
       - Club 1 text source   (the OBS text source bound to /club1)
       - Club 2 text source   (the OBS text source bound to /club2)
       - Club 1 image source  (an Image Source you've added to the scene)
       - Club 2 image source  (an Image Source you've added to the scene)
       - Crest folder         (a local folder, e.g. C:\OBS\crests)
  3. In that folder, add one .png per club, named after the club's name in
     "slug" form: lowercase, spaces/punctuation -> "-". Examples:
       "PBC Musterstadt"  -> pbc-musterstadt.png
       "PBC Bremen"  -> pbc-bremen.png
     (Run script_slug("Your Club Name") in the OBS Script Log to check the
     exact filename it expects, if unsure.)
  4. Optional: add a "no-crest.png" (or "default.png") in the same folder -
     shown whenever a club has no matching file (e.g. no club set, or a typo).

That's it - no network transfer, no per-match setup. Add a new club by adding
one .png file.
]]

obs = obslua

-- ── configurable via the script's OBS panel ────────────────────────────────
local club1_text_name  = ""
local club2_text_name  = ""
local club1_image_name = ""
local club2_image_name = ""
local crest_folder     = ""

local POLL_INTERVAL_MS = 750

-- last-seen text per slot, so we only touch the image source on an actual change
local last_text = { "", "" }

-- Lua's string.lower() and its %w pattern class only understand ASCII, so German umlauts
-- would otherwise fall into the generic non-alphanumeric collapse below and get replaced by a
-- plain "-" (e.g. "Schönberg" -> "sch-nberg"), which operators don't expect. Transliterate them
-- explicitly first, both cases, so the result matches the common German convention instead.
local UMLAUT_REPLACEMENTS = {
	["ä"] = "ae", ["Ä"] = "ae",
	["ö"] = "oe", ["Ö"] = "oe",
	["ü"] = "ue", ["Ü"] = "ue",
	["ß"] = "ss",
}

local function slugify(name)
	if name == nil then return "" end
	local s = name:lower()
	for from, to in pairs(UMLAUT_REPLACEMENTS) do
		s = s:gsub(from, to)
	end
	s = s:gsub("[^%w]+", "-")   -- any run of non-alphanumeric chars -> one dash
	s = s:gsub("^%-+", ""):gsub("%-+$", "")
	return s
end

-- exposed so an operator can sanity-check a filename from the OBS Script Log
-- (Tools > Scripts > this script > "Club 1 text source" field is not needed for this -
-- just eyeball the naming rule above; this function exists mainly for that doc comment).
function script_slug(name)
	obs.script_log(obs.LOG_INFO, "'" .. tostring(name) .. "' -> '" .. slugify(name) .. ".png'")
end

local function file_exists(path)
	local f = io.open(path, "rb")
	if f ~= nil then f:close(); return true end
	return false
end

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

local function set_image(image_source_name, file_path)
	if image_source_name == "" then return end
	local source = obs.obs_get_source_by_name(image_source_name)
	if source == nil then return end
	local settings = obs.obs_data_create()
	obs.obs_data_set_string(settings, "file", file_path)
	obs.obs_source_update(source, settings)
	obs.obs_data_release(settings)
	obs.obs_source_release(source)
end

local function apply_crest(slot, text_source_name, image_source_name)
	local text = current_text(text_source_name)
	if text == nil or text == last_text[slot] then return end
	last_text[slot] = text

	-- A player with no club is expected, not an error - don't try to match a ".png" for empty
	-- text, and don't warn about it. Still fall through to no-crest.png/default.png if present.
	local is_blank = text:match("^%s*$") ~= nil
	local candidates = {}
	if not is_blank then
		table.insert(candidates, crest_folder .. "/" .. slugify(text) .. ".png")
	end
	table.insert(candidates, crest_folder .. "/no-crest.png")
	table.insert(candidates, crest_folder .. "/default.png")

	for _, path in ipairs(candidates) do
		if file_exists(path) then
			set_image(image_source_name, path)
			return
		end
	end
	if is_blank then return end
	-- No match and no fallback file present - leave the image source as-is, but log it so an
	-- operator onboarding many clubs can see exactly which filename was expected and missing,
	-- without having to guess or reproduce it by hand.
	obs.script_log(obs.LOG_WARNING, "No crest file for club '" .. text .. "' - expected " .. candidates[1])
end

local function tick()
	apply_crest(1, club1_text_name, club1_image_name)
	apply_crest(2, club2_text_name, club2_image_name)
end

-- ── OBS script lifecycle ────────────────────────────────────────────────────

function script_description()
	return "Switches two image sources to a local crest .png based on two text sources' " ..
		"current club-name text. See the comment at the top of this file for one-time setup."
end

function script_properties()
	local props = obs.obs_properties_create()
	obs.obs_properties_add_text(props, "club1_text_name", "Club 1 text source", obs.OBS_TEXT_DEFAULT)
	obs.obs_properties_add_text(props, "club2_text_name", "Club 2 text source", obs.OBS_TEXT_DEFAULT)
	obs.obs_properties_add_text(props, "club1_image_name", "Club 1 image source", obs.OBS_TEXT_DEFAULT)
	obs.obs_properties_add_text(props, "club2_image_name", "Club 2 image source", obs.OBS_TEXT_DEFAULT)
	obs.obs_properties_add_path(props, "crest_folder", "Crest folder", obs.OBS_PATH_DIRECTORY, nil, nil)
	return props
end

function script_update(settings)
	club1_text_name  = obs.obs_data_get_string(settings, "club1_text_name")
	club2_text_name  = obs.obs_data_get_string(settings, "club2_text_name")
	club1_image_name = obs.obs_data_get_string(settings, "club1_image_name")
	club2_image_name = obs.obs_data_get_string(settings, "club2_image_name")
	crest_folder     = obs.obs_data_get_string(settings, "crest_folder")
	-- force a re-check on the next tick even if the club name text itself hasn't changed
	-- (e.g. right after picking a different image source in the panel)
	last_text = { nil, nil }
end

function script_load(settings)
	obs.timer_add(tick, POLL_INTERVAL_MS)
end

function script_unload()
	obs.timer_remove(tick)
end
