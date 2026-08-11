{{/*
Common labels
*/}}
{{- define "fanzone.labels" -}}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
{{- end }}

{{/*
Service image
*/}}
{{- define "fanzone.image" -}}
{{ .Values.global.image.registry }}/{{ .Chart.Name }}:{{ .Values.global.image.tag }}
{{- end }}
