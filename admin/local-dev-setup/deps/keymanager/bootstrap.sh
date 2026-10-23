#!/usr/bin/env bash
# Entrypoint of the local keymanager container.
# Starts keymanager without the auth adapter (the image normally downloads it at start), so its
# APIs are open to admin-service. On first start, creates the ROOT, KERNEL and KERNEL:SIGN keys.
set -euo pipefail

KM_URL="http://127.0.0.1:8088/v1/keymanager"
READY_MARKER=/tmp/.km-ready

log() { echo "keymanager-bootstrap: $*"; }

generate_master_key() {
  local app_id="$1" ref_id="${2:-}"
  local now; now="$(date -u +%Y-%m-%dT%H:%M:%S.000Z)"
  log "generateMasterKey ${app_id}${ref_id:+:$ref_id}"
  local body
  body="$(curl -sS -X POST "${KM_URL}/generateMasterKey/CERTIFICATE" -H "Content-Type: application/json" \
    -d "{\"id\":\"mosip.keymanager.generate\",\"version\":\"v1\",\"requesttime\":\"${now}\",\"request\":{\"applicationId\":\"${app_id}\",\"referenceId\":\"${ref_id}\",\"force\":false,\"commonName\":\"www.mosip.io\",\"organizationUnit\":\"MOSIP-TECH-CENTER\",\"organization\":\"IITB\",\"location\":\"BANGALORE\",\"state\":\"KA\",\"country\":\"IN\"}}")"
  echo "$body" | grep -q '"certificate"' || { log "ERROR: ${body:0:500}"; exit 1; }
}

sign_key_ready() {
  curl -fsS "${KM_URL}/getCertificate?applicationId=KERNEL&referenceId=SIGN" 2>/dev/null | grep -q '"certificate"'
}

rm -f "$READY_MARKER"
mkdir -p /home/mosip/keys

log "starting keymanager"
java -XX:+UseZGC -Dfile.encoding=UTF-8 \
  -Dspring.cloud.config.uri=http://config-server:51000/config \
  -Dspring.cloud.config.label=master \
  -Dspring.cloud.config.name=keymanager,kernel \
  -Dspring.profiles.active=default,local \
  -jar ./kernel-keymanager-service.jar &
KM_PID=$!

log "waiting for health"
for _ in $(seq 1 120); do
  curl -fs "${KM_URL}/actuator/health" >/dev/null 2>&1 && break
  kill -0 "$KM_PID" 2>/dev/null || { log "ERROR: keymanager exited"; wait "$KM_PID"; exit 1; }
  sleep 2
done
curl -fs "${KM_URL}/actuator/health" >/dev/null || { log "ERROR: health timeout"; exit 1; }

if sign_key_ready; then
  log "keys already present"
else
  generate_master_key ROOT
  generate_master_key KERNEL
  generate_master_key KERNEL SIGN
  sign_key_ready || { log "ERROR: KERNEL:SIGN certificate not available"; exit 1; }
fi

touch "$READY_MARKER"
log "ready"
wait "$KM_PID"
