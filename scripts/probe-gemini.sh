#!/usr/bin/env bash
# Probes the real Gemini API with YOUR key to settle the open questions of specs 002/003:
#   - which models are available to your key,
#   - whether Opus-in-OGG (what Aura records) is accepted as audio,
#   - how speakers and timestamps come back, and how long it takes.
# Nothing here is sent anywhere except to Google, and the uploaded file is deleted at the end.
#
#   GEMINI_API_KEY=... scripts/probe-gemini.sh list-models
#   GEMINI_API_KEY=... scripts/probe-gemini.sh transcribe recording.ogg [model]
#
# Env: MIME (override the guessed type), LANGS (language hint, default "es"), GEMINI_BASE_URL (tests).
# Needs: bash, curl, python3.
set -euo pipefail

: "${GEMINI_API_KEY:?Set GEMINI_API_KEY (https://aistudio.google.com/apikey)}"
BASE="${GEMINI_BASE_URL:-https://generativelanguage.googleapis.com}"
cmd="${1:-}"

api() { # api METHOD PATH [curl args...]  -> body on stdout, HTTP status on fd 3
  local method="$1" path="$2"; shift 2
  curl -sS -X "$method" "$BASE$path" -H "x-goog-api-key: $GEMINI_API_KEY" "$@"
}

case "$cmd" in
  list-models)
    api GET "/v1beta/models?pageSize=200" | python3 -c '
import json,sys
d=json.load(sys.stdin)
if "error" in d: sys.exit("API error: %s" % d["error"].get("message", d["error"]))
for m in d.get("models", []):
    methods=m.get("supportedGenerationMethods", [])
    if "generateContent" in methods:
        print("%-42s %s" % (m["name"].split("/",1)[1], m.get("displayName","")))'
    ;;

  transcribe)
    file="${2:?usage: probe-gemini.sh transcribe <audio file> [model]}"
    model="${3:-${MODEL:-gemini-2.5-flash}}"
    [ -f "$file" ] || { echo "No such file: $file" >&2; exit 1; }
    mime="${MIME:-}"
    if [ -z "$mime" ]; then
      case "${file##*.}" in
        ogg|opus|oga) mime=audio/ogg ;; wav) mime=audio/wav ;; mp3) mime=audio/mp3 ;;
        flac) mime=audio/flac ;; aac|m4a) mime=audio/aac ;; *) mime=audio/ogg ;;
      esac
    fi
    bytes="$(wc -c < "$file")"
    echo "File: $file ($bytes bytes), type $mime, model $model"
    tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' EXIT

    # 1) Files API, resumable upload (start, then upload+finalize)
    curl -sS -D "$tmp/h" -o /dev/null "$BASE/upload/v1beta/files" -H "x-goog-api-key: $GEMINI_API_KEY" \
      -H "X-Goog-Upload-Protocol: resumable" -H "X-Goog-Upload-Command: start" \
      -H "X-Goog-Upload-Header-Content-Length: $bytes" -H "X-Goog-Upload-Header-Content-Type: $mime" \
      -H "Content-Type: application/json" -d '{"file":{"display_name":"aura-probe"}}'
    upload_url="$(grep -i '^x-goog-upload-url:' "$tmp/h" | head -1 | cut -d' ' -f2 | tr -d '\r')"
    [ -n "$upload_url" ] || { echo "The upload did not start:"; cat "$tmp/h"; exit 1; }
    curl -sS "$upload_url" -H "Content-Length: $bytes" -H "X-Goog-Upload-Offset: 0" \
      -H "X-Goog-Upload-Command: upload, finalize" --data-binary "@$file" > "$tmp/file.json"
    read -r name uri < <(python3 -c '
import json,sys
f=json.load(open(sys.argv[1])).get("file") or sys.exit("Upload failed: "+open(sys.argv[1]).read())
print(f["name"], f["uri"])' "$tmp/file.json")
    echo "Uploaded as $name"
    trap 'api DELETE "/v1beta/$name" >/dev/null 2>&1 || true; rm -rf "$tmp"' EXIT

    # 2) wait until the file is ACTIVE
    for _ in $(seq 1 30); do
      state="$(api GET "/v1beta/$name" | python3 -c 'import json,sys;print(json.load(sys.stdin).get("state","?"))')"
      [ "$state" = "ACTIVE" ] && break
      [ "$state" = "FAILED" ] && { echo "The file was rejected (state FAILED): format not supported?"; exit 1; }
      sleep 2
    done

    # 3) transcription with speakers, as JSON
    python3 - "$uri" "$mime" "${LANGS:-es}" > "$tmp/request.json" <<'PY'
import json,sys
uri,mime,lang=sys.argv[1:4]
prompt=("Transcribe this audio verbatim. The main language is '%s' (others may appear). "
        "Identify who speaks. Label speakers SPEAKER_00, SPEAKER_01, ... in order of first appearance. "
        "Give start and end of each segment in seconds from the start of the audio. "
        "Do not summarise or translate. Answer with JSON only." % lang)
schema={"type":"OBJECT","required":["language","segments"],"properties":{
  "language":{"type":"STRING"},
  "segments":{"type":"ARRAY","items":{"type":"OBJECT","required":["speaker","start","end","text"],"properties":{
    "speaker":{"type":"STRING"},"start":{"type":"NUMBER"},"end":{"type":"NUMBER"},"text":{"type":"STRING"}}}}}}
print(json.dumps({"contents":[{"role":"user","parts":[{"text":prompt},{"file_data":{"mime_type":mime,"file_uri":uri}}]}],
  "generationConfig":{"temperature":0,"responseMimeType":"application/json","responseSchema":schema}}))
PY
    start=$(date +%s)
    code="$(curl -sS -o "$tmp/response.json" -w '%{http_code}' -X POST "$BASE/v1beta/models/$model:generateContent" \
      -H "x-goog-api-key: $GEMINI_API_KEY" -H "Content-Type: application/json" -d "@$tmp/request.json")"
    echo "generateContent: HTTP $code in $(( $(date +%s) - start )) s"
    python3 - "$tmp/response.json" <<'PY'
import json,sys
d=json.load(open(sys.argv[1]))
if "error" in d:
    e=d["error"]; print("ERROR %s: %s" % (e.get("code"), e.get("message"))); sys.exit(1)
c=d["candidates"][0]; print("finishReason:", c.get("finishReason"), "| usage:", d.get("usageMetadata"))
text=c["content"]["parts"][0]["text"]
try:
    r=json.loads(text)
except Exception as ex:
    print("The model did not return valid JSON (%s). Start of the answer:\n%s" % (ex, text[:800])); sys.exit(1)
segs=r.get("segments", [])
print("language:", r.get("language"), "| segments:", len(segs), "| speakers:", sorted({s["speaker"] for s in segs}))
if segs: print("covers: %.1f s to %.1f s" % (segs[0]["start"], segs[-1]["end"]))
for s in segs[:5]: print("  [%6.1f-%6.1f] %s: %s" % (s["start"], s["end"], s["speaker"], s["text"][:90]))
PY
    ;;

  *)
    sed -n '2,13p' "$0" | sed 's/^# \{0,1\}//'
    exit 2
    ;;
esac
