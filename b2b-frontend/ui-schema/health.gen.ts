// AUTO-GENERATED FILE - DO NOT EDIT

export type UiField = {
  key: string
  label: string
  component: 'text' | 'number' | 'switch' | 'json'
  required: boolean
}

export type UiSchemaEntity = {
  entity: string
  endpoint: string
  method: string
  fields: UiField[]
}

export const healthUiSchema: UiSchemaEntity = {
  "entity": "health",
  "endpoint": "/actuator/health",
  "method": "get",
  "fields": [
    {
      "key": "status",
      "label": "health.status",
      "component": "text",
      "required": true
    },
    {
      "key": "components",
      "label": "health.components",
      "component": "json",
      "required": false
    }
  ]
}
