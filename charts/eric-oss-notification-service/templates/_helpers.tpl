{{/* vim: set filetype=mustache: */}}
{{/*
Create a map from ".Values.global" with defaults if missing in values file.
This hides defaults from values file.
*/}}
{{- define "eric-oss-notification-service.global" -}}
  {{- $globalDefaults := dict "security" (dict "tls" (dict "enabled" false)) -}}
  {{- $globalDefaults := merge $globalDefaults (dict "security" (dict "policyBinding" (dict "create" false))) -}}
  {{- $globalDefaults := merge $globalDefaults (dict "registry" (dict "url" "armdocker.rnd.ericsson.se")) -}}
  {{- $globalDefaults := merge $globalDefaults (dict "registry" (dict "imagePullPolicy" "IfNotPresent")) -}}
  {{- $globalDefaults := merge $globalDefaults (dict "nodeSelector" (dict)) -}}
  {{- if .Values.global -}}
    {{- mergeOverwrite $globalDefaults .Values.global | toJson -}}
  {{- else -}}
    {{- $globalDefaults | toJson -}}
  {{- end -}}
{{- end -}}

{{/*
Merged annotations for Default, which includes productInfo and config
*/}}
{{- define "eric-oss-notification-service.annotations" -}}
  {{- $productInfo := include "eric-oss-notification-service.product-info" . | fromYaml -}}
  {{- $config := include "eric-oss-notification-service.config-annotations" . | fromYaml -}}
  {{- include "eric-oss-notification-service.mergeAnnotations" (dict "location" .Template.Name "sources" (list $productInfo $config)) | trim }}
{{- end -}}

{{/*
Wrapper functions to set the contexts
*/}}
{{- define "eric-oss-notification-service.mergeAnnotations" -}}
    {{- include "eric-oss-notification-service.aggregatedMerge" (dict "context" "annotations" "location" .location "sources" .sources) }}
{{- end -}}
{{- define "eric-oss-notification-service.mergeLabels" -}}
    {{- include "eric-oss-notification-service.aggregatedMerge" (dict "context" "labels" "location" .location "sources" .sources) }}
{{- end -}}

{{/*
Expand the name of the chart.
*/}}
{{- define "eric-oss-notification-service.name" }}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create chart version as used by the chart label.
*/}}
{{- define "eric-oss-notification-service.version" }}
{{- printf "%s" .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "eric-oss-notification-service.fullname" }}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- $name | trunc 63 | trimSuffix "-" }}
{{/* Ericsson mandates the name defined in metadata should start with chart name. */}}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "eric-oss-notification-service.chart" }}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{/*
Create image repo path
*/}}
{{- define "eric-oss-notification-service.repoPath" }}
{{- if .Values.imageCredentials.repoPath }}
{{- print .Values.imageCredentials.repoPath "/" }}
{{- end }}
{{- end }}

{{/*
Create image pull policy
*/}}
{{- define "eric-oss-notification-service.registryImagePullPolicy" -}}
    {{- $global := fromJson (include "eric-oss-notification-service.global" .) -}}
    {{- if .Values.imageCredentials.registry.imagePullPolicy -}}
      {{- print .Values.imageCredentials.registry.imagePullPolicy -}}
    {{- else -}}
      {{- print $global.registry.imagePullPolicy -}}
    {{- end -}}
{{- end -}}

{{/*
Create image pull secrets
*/}}
{{- define "eric-oss-notification-service.pullSecrets" }}
{{- $pullSecret := "" }}
{{- if .Values.global }}
    {{- if .Values.global.pullSecret }}
        {{- $pullSecret = .Values.global.pullSecret }}
    {{- end }}
{{- end }}
{{- if .Values.imageCredentials }}
    {{- if .Values.imageCredentials.pullSecret }}
        {{- $pullSecret = .Values.imageCredentials.pullSecret }}
    {{- end }}
{{- end }}
{{- print $pullSecret }}
{{- end }}
{{/*
Timezone variable
*/}}
{{- define "eric-oss-notification-service.timezone" }}
{{- $timezone := "UTC" }}
{{- if .Values.global }}
    {{- if .Values.global.timezone }}
        {{- $timezone = .Values.global.timezone }}
    {{- end }}
{{- end }}
{{- print $timezone | quote }}
{{- end -}}

{{/*
Common labels
*/}}
{{- define "eric-oss-notification-service.common-labels" }}
helm.sh/chart: {{ include "eric-oss-notification-service.chart" . }}
app.kubernetes.io/name: {{ include "eric-oss-notification-service.name" . }}
app.kubernetes.io/version: {{ include "eric-oss-notification-service.version" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{/*
Return the fsgroup set via global parameter if it's set, otherwise 10000
*/}}
{{- define "eric-oss-notification-service.fsGroup.coordinated" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.fsGroup -}}
      {{- if .Values.global.fsGroup.manual -}}
        {{ .Values.global.fsGroup.manual }}
      {{- else -}}
        {{- if eq .Values.global.fsGroup.namespace true -}}
          # The 'default' defined in the Security Policy will be used.
        {{- else -}}
          10000
      {{- end -}}
    {{- end -}}
  {{- else -}}
    10000
  {{- end -}}
  {{- else -}}
    10000
  {{- end -}}
{{- end -}}

{{/*
Create a user defined label (DR-D1121-068, DR-D1121-060)
*/}}
{{ define "eric-oss-notification-service.config-labels" }}
  {{- $global := (.Values.global).labels -}}
  {{- $service := .Values.labels -}}
  {{- include "eric-oss-notification-service.mergeLabels" (dict "location" .Template.Name "sources" (list $global $service)) }}
{{- end }}

{{/*
Merged labels for Default, which includes Standard and Config
*/}}
{{- define "eric-oss-notification-service.labels" -}}
  {{- $common := include "eric-oss-notification-service.common-labels" . | fromYaml -}}
  {{- $config := include "eric-oss-notification-service.config-labels" . | fromYaml -}}
  {{- include "eric-oss-notification-service.mergeLabels" (dict "location" .Template.Name "sources" (list $common $config)) | trim }}
{{- end -}}

{{/*
Create a user defined annotation
*/}}
{{ define "eric-oss-notification-service.config-annotations" }}
{{- $global := (.Values.global).annotations -}}
{{- $service := .Values.annotations -}}
{{- include "eric-oss-notification-service.mergeAnnotations" (dict "location" .Template.Name "sources" (list $global $service)) }}
{{- end }}

{{/*
Create a user defined label (DR-D1121-068)

{{ define "eric-oss-notification-service.config-labels" }}
{{- if .Values.labels -}}
{{- range $name, $config := .Values.labels }}
{{ $name }}: {{ tpl $config $ }}
{{- end }}
{{- end }}
{{- end }}
*/}}

{{- define "eric-oss-notification-service.product-info" }}
ericsson.com/product-name: {{ (fromYaml (.Files.Get "eric-product-info.yaml")).productName | quote }}
ericsson.com/product-number: {{ (fromYaml (.Files.Get "eric-product-info.yaml")).productNumber | quote }}
ericsson.com/product-revision: {{regexReplaceAll "(.*)[+].*" .Chart.Version "${1}" }}
{{- end -}}

{{- define "eric-oss-notification-service.db-url" -}}
{{- if .Values.persistence.databaseUrl }}
    {{- .Values.persistence.databaseUrl -}}
{{- else -}}
jdbc:postgresql://{{.Values.persistence.host}}:5432/{{.Values.persistence.database}}
{{- end }}

{{- end }}

{{/*
Release name
*/}}
{{- define "eric-oss-notification-service.release.name" -}}
{{- default .Release.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Security policy reference
*/}}
{{- define "eric-oss-notification-service.securityPolicy.reference" -}}
{{- $global := fromJson (include "eric-oss-notification-service.global" .) -}}
{{- if $global.security.policyReferenceMap -}}
    {{ $mapped := index .Values "global" "security" "policyReferenceMap" "default-restricted-security-policy" }}
    {{- if $mapped -}}
        {{ $mapped }}
    {{- else -}}
      default-restricted-security-policy
    {{- end -}}
{{- else -}}
  default-restricted-security-policy
{{- end -}}
{{- end -}}

{{/*
Create container level annotations
*/}}
{{- define "eric-oss-notification-service.container-annotations" }}
{{- $appArmorValue := .Values.appArmorProfile.type -}}
    {{- if .Values.appArmorProfile -}}
        {{- if .Values.appArmorProfile.type -}}
            {{- if eq .Values.appArmorProfile.type "localhost" -}}
                {{- $appArmorValue = printf "%s/%s" .Values.appArmorProfile.type .Values.appArmorProfile.localhostProfile }}
            {{- end}}
matchLabels:
      app: {{ template "eric-oss-notification-service.name:" $ }}
container.apparmor.security.beta.kubernetes.io/app: {{ $appArmorValue | quote }}
        {{- end}}
    {{- end}}
{{- end}}

{{/*
Seccomp profile section (DR-1123-128)
*/}}
{{- define "eric-oss-notification-service.seccomp-profile" }}
    {{- if .Values.seccompProfile }}
      {{- if .Values.seccompProfile.type }}
          {{- if eq .Values.seccompProfile.type "Localhost" }}
              {{- if .Values.seccompProfile.localhostProfile }}
seccompProfile:
  type: {{ .Values.seccompProfile.type }}
  localhostProfile: {{ .Values.seccompProfile.localhostProfile }}
            {{- end }}
          {{- else }}
seccompProfile:
  type: {{ .Values.seccompProfile.type }}
          {{- end }}
        {{- end }}
      {{- end }}
{{- end }}

{{/*
The notification service repo path (DR-D1121-067)
*/}}
{{- define "eric-oss-notification-service-path" -}}
    {{- $productInfo := fromYaml (.Files.Get "eric-product-info.yaml") -}}
    {{- $registryUrl := $productInfo.images.ns.registry -}}
    {{- $repoPath := $productInfo.images.ns.repoPath -}}
    {{- $name := $productInfo.images.ns.name -}}
    {{- $tag := $productInfo.images.ns.tag -}}
    {{- if .Values.global -}}
        {{- if .Values.global.registry -}}
            {{- if .Values.global.registry.url -}}
                {{- $registryUrl = .Values.global.registry.url -}}
            {{- end -}}
            {{- if not (kindIs "invalid" .Values.global.registry.repoPath) -}}
                {{- $repoPath = .Values.global.registry.repoPath -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
    {{- if .Values.imageCredentials -}}
        {{- if .Values.imageCredentials.registry -}}
            {{- if .Values.imageCredentials.registry.url -}}
                    {{- $registryUrl = .Values.imageCredentials.registry.url -}}
            {{- end -}}
         {{- end -}}
    {{- end -}}
    {{- if not (kindIs "invalid" .Values.imageCredentials.repoPath) -}}
         {{- $repoPath = .Values.imageCredentials.repoPath -}}
    {{- end -}}
    {{- if $repoPath -}}
        {{- $repoPath = printf "%s/" $repoPath -}}
    {{- end -}}
    {{- printf "%s/%s%s:%s" $registryUrl $repoPath $name $tag -}}
{{- end -}}

Create a merged set of nodeSelectors from global and service level.
*/}}
{{ define "eric-oss-notification-service.nodeSelector" }}
  {{- $global := fromJson (include "eric-oss-notification-service.global" .) -}}
  {{- if .Values.nodeSelector -}}
    {{- range $key, $localValue := .Values.nodeSelector -}}
      {{- if hasKey $global.nodeSelector $key -}}
          {{- $globalValue := index $global.nodeSelector $key -}}
          {{- if ne $globalValue $localValue -}}
            {{- printf "nodeSelector \"%s\" is specified in both global (%s: %s) and service level (%s: %s) with differing values which is not allowed." $key $key $globalValue $key $localValue | fail -}}
          {{- end -}}
      {{- end -}}
    {{- end -}}
    {{- toYaml (merge $global.nodeSelector .Values.nodeSelector) | trim -}}
  {{- else -}}
    {{- toYaml $global.nodeSelector | trim -}}
  {{- end -}}
{{ end }}

{{/*
The name of the cluster role used during openshift deployments.
This helper is provided to allow use of the new global.security.privilegedPolicyClusterRoleName if set, otherwise
use the previous naming convention of <release_name>-allowed-use-privileged-policy for backwards compatibility.
*/}}
{{- define "eric-oss-notification-service.privileged.cluster.role.name" -}}
  {{- if hasKey (.Values.global) "security" -}}
    {{- if hasKey (.Values.global.security) "privilegedPolicyClusterRoleName" -}}
      {{ .Values.global.security.privilegedPolicyClusterRoleName }}
    {{- else -}}
      {{ template "eric-oss-notification-service.release.name" . }}-allowed-use-privileged-policy
    {{- end -}}
  {{- else -}}
    {{ template "eric-oss-notification-service.release.name" . }}-allowed-use-privileged-policy
  {{- end -}}
{{- end -}}

{{/*
IDUN-11334: DR-D470217-007-AD
This helper defines whether this service enter the Service Mesh or not.
It enters only if the global switch is enabled (default false).
*/}}
{{- define "eric-oss-notification-service.service-mesh-enabled" }}
  {{- $globalMeshEnabled := "false" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.serviceMesh -}}
        {{- $globalMeshEnabled = .Values.global.serviceMesh.enabled -}}
    {{- end -}}
  {{- end -}}
  {{- $globalMeshEnabled -}}
{{- end -}}

{{/*
IDUN-11334: DR-D470217-011
This helper defines the annotation which bring the service into the mesh.
*/}}
{{- define "eric-oss-notification-service.service-mesh-inject" }}
{{- if eq (include "eric-oss-notification-service.service-mesh-enabled" .) "true" }}
sidecar.istio.io/inject: "true"
{{- else }}
sidecar.istio.io/inject: "false"
{{- end }}
{{- end -}}

{{/*
This helper defines which out-mesh services will be reached by this one.
*/}}
{{- define "eric-oss-notification-service.service-mesh-ism2osm-labels" }}
{{- if eq (include "eric-oss-notification-service.service-mesh-enabled" .) "true" }}
  {{- if eq (include "eric-oss-notification-service.global-security-tls-enabled" .) "true" }}
eric-data-message-bus-kf-ism-access: "true"
eric-oss-notification-service-database-pg-ism-access: "true"
  {{- end }}
{{- end -}}
{{- end -}}

{{/*
IDUN-11334: GL-D470217-080-AD
This helper captures the service mesh version from the integration chart to
annotate the workloads so they are redeployed in case of service mesh upgrade.
*/}}
{{- define "eric-oss-notification-service.service-mesh-version" }}
{{- if eq (include "eric-oss-notification-service.service-mesh-enabled" .) "true" }}
  {{- if .Values.global -}}
    {{- if .Values.global.serviceMesh -}}
      {{- if .Values.global.serviceMesh.annotations -}}
        {{- if .Values.global.serviceMesh.annotations -}}
        {{- range $name, $config := .Values.global.serviceMesh.annotations }}
        {{- $name }}: {{ tpl $config $ | quote }}
        {{- end }}
        {{- end }}
      {{- end -}}
    {{- end -}}
  {{- end -}}
{{- end -}}
{{- end -}}

check global.security.tls.enabled since it is removed from values.yaml 
*/}}
{{- define "eric-oss-notification-service.global-security-tls-enabled" -}}
{{- if  .Values.global -}}
  {{- if  .Values.global.security -}}
    {{- if  .Values.global.security.tls -}}
       {{- .Values.global.security.tls.enabled | toString -}}
    {{- else -}}
       {{- "false" -}}
    {{- end -}}
  {{- else -}}
       {{- "false" -}}
  {{- end -}}
{{- else -}}
{{- "false" -}}
{{- end -}}
{{- end -}}

{{/*
Define kafka bootstrap server for Notification Service 
*/}}
{{- define "eric-oss-notification-service.kafka-bootstrap-server" -}}
{{- $kafkaBootstrapServer := "" -}}
{{- $serviceMesh := ( include "eric-oss-notification-service.service-mesh-enabled" . ) -}}
{{- $tls := ( include "eric-oss-notification-service.global-security-tls-enabled" . ) -}}
{{- if and (eq $serviceMesh "true") (eq $tls "true") -}}
    {{- $kafkaBootstrapServer = .Values.messaging.kafka.bootstrapServersTls -}}
{{ else }}
    {{- $kafkaBootstrapServer = .Values.messaging.kafka.bootstrapServers -}}
{{ end }}
{{- print $kafkaBootstrapServer -}}
{{- end -}}

{{/*
This helper defines the annotation for define service mesh volume
*/}}
{{- define "eric-oss-notification-service.service-mesh-volume" }}
{{- if and (eq (include "eric-oss-notification-service.service-mesh-enabled" .) "true") (eq (include "eric-oss-notification-service.global-security-tls-enabled" .) "true") }}
sidecar.istio.io/userVolume: '{"notification-service-certs-tls":{"secret":{"secretName":"eric-oss-notification-service-secret","optional":true}},"notification-service-database-pg-certs-tls":{"secret":{"secretName":"eric-oss-notification-service-database-pg-secret","optional":true}},"notification-service-certs-ca-tls":{"secret":{"secretName":"eric-sec-sip-tls-trusted-root-cert"}}}'
sidecar.istio.io/userVolumeMount: '{"notification-service-certs-tls":{"mountPath":"/etc/istio/tls/eric-data-message-bus-kf/","readOnly":true},"notification-service-database-pg-certs-tls":{"mountPath":"/etc/istio/tls/notification-service-database-pg/","readOnly":true},"notification-service-certs-ca-tls":{"mountPath":"/etc/istio/tls-ca","readOnly":true}}'
{{ end }}
{{- end -}}

{{/*
This helper defines the annotation for priorityClass DR-D1126-030
*/}}
{{- define "eric-oss-notification-service.pod-priority" -}}
{{- if .Values.podPriority -}}
    {{- .Values.podPriority.priorityClassName | toString -}}
{{- end -}}
{{- end -}}
{{/*
Create prometheus info
*/}}
{{- define "eric-oss-notification-service.prometheus" -}}
prometheus.io/path: {{ .Values.prometheus.path | quote }}
prometheus.io/port: {{ .Values.service.port | quote }}
prometheus.io/scrape: {{ .Values.prometheus.scrape | quote }}
{{- end -}}
{{/*
Define the log streaming method (DR-470222-010)
*/}}
{{- define "eric-oss-notification-service.streamingMethod" -}}
{{- $streamingMethod := "direct" -}}
{{- if .Values.global -}}
  {{- if .Values.global.log -}}
      {{- if .Values.global.log.streamingMethod -}}
        {{- $streamingMethod = .Values.global.log.streamingMethod }}
      {{- end -}}
  {{- end -}}
{{- end -}}
{{- if .Values.log -}}
  {{- if .Values.log.streamingMethod -}}
    {{- $streamingMethod = .Values.log.streamingMethod }}
  {{- end -}}
{{- end -}}
{{- print $streamingMethod -}}
{{- end -}}

{{/*
Define the label needed for reaching eric-log-transformer (DR-470222-010)
*/}}
{{- define "eric-oss-notification-service.directStreamingLabel" -}}
{{- $streamingMethod := (include "eric-oss-notification-service.streamingMethod" .) -}}
{{- if or (eq "direct" $streamingMethod) (eq "dual" $streamingMethod) }}
logger-communication-type: "direct"
{{- end -}}
{{- end -}}

{{/*
Define logging environment variables (DR-470222-010)
*/}}
{{ define "eric-oss-notification-service.loggingEnv" }}
{{- $streamingMethod := (include "eric-oss-notification-service.streamingMethod" .) -}}
{{- if or (eq "direct" $streamingMethod) (eq "dual" $streamingMethod) -}}
  {{- if eq "direct" $streamingMethod }}
- name: LOGBACK_CONFIG_FILE
  value: "classpath:logback-http.xml"
  {{- end }}
  {{- if eq "dual" $streamingMethod }}
- name: LOGBACK_CONFIG_FILE
  value: "classpath:logback-dual.xml"
  {{- end }}
- name: LOGSTASH_DESTINATION
  value: eric-log-transformer
- name: LOGSTASH_PORT
  value: "9080"
- name: POD_NAME
  valueFrom:
    fieldRef:
      fieldPath: metadata.name
- name: POD_UID
  valueFrom:
    fieldRef:
      fieldPath: metadata.uid
- name: CONTAINER_NAME
  value: eric-oss-notification-service
- name: NODE_NAME
  valueFrom:
    fieldRef:
      fieldPath: spec.nodeName
- name: NAMESPACE
  valueFrom:
    fieldRef:
      fieldPath: metadata.namespace
{{- else if eq $streamingMethod "indirect" }}
- name: LOGBACK_CONFIG_FILE
  value: "classpath:logback-json.xml"
{{- else }}
  {{- fail ".log.streamingMethod unknown" }}
{{- end -}}
{{ end }}

{{/*
This helper get the custom user used to connect to postgres DB instance.
*/}}
{{ define "eric-oss-notification-service.dbuser" }}
  {{- $secret := (lookup "v1" "Secret" .Release.Namespace "eric-eo-database-pg-secret") -}}
  {{- if $secret -}}
    {{ index $secret.data "custom-user" | b64dec | quote }}
  {{- else -}}
    {{- (randAlphaNum 16) | b64enc | quote -}}
  {{- end -}}
{{- end -}}

{{/*
    Define supplementalGroups (DR-D1123-135)
*/}}
{{- define "eric-oss-notification-service.supplementalGroups" -}}
  {{- $globalGroups := (list) -}}
  {{- if ( (((.Values).global).podSecurityContext).supplementalGroups) }}
    {{- $globalGroups = .Values.global.podSecurityContext.supplementalGroups -}}
  {{- end -}}
  {{- $localGroups := (list) -}}
  {{- if ( ((.Values).podSecurityContext).supplementalGroups) -}}
    {{- $localGroups = .Values.podSecurityContext.supplementalGroups -}}
  {{- end -}}
  {{- $mergedGroups := (list) -}}
  {{- if $globalGroups -}}
    {{- $mergedGroups = $globalGroups -}}
  {{- end -}}
  {{- if $localGroups -}}
    {{- $mergedGroups = concat $globalGroups $localGroups | uniq -}}
  {{- end -}}
  {{- if $mergedGroups -}}
    supplementalGroups: {{- toYaml $mergedGroups | nindent 8 -}}
  {{- end -}}
  {{- /*Do nothing if both global and local groups are not set */ -}}
{{- end -}}

{{/*
IDUN-79326: DR-D1123-134
This helper defines whether this service has to bind security policies to the service account using role binding if defined.
It enters only if security policies are defined at global.
*/}}
{{- define "eric-oss-notification-service.security-policies-defined" }}
  {{- $globalSecurityPoliciesDefined := "false" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.securityPolicy -}}
      {{- if ne .Values.global.securityPolicy.rolekind "" }}
        {{- $globalSecurityPoliciesDefined = "true" -}}
      {{- end -}}
    {{- end -}}
  {{- end -}}
  {{- $globalSecurityPoliciesDefined -}}
{{- end -}}