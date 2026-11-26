/* tslint:disable */
/* eslint-disable */

import { mapValues } from '../runtime'
/**
 * Column of a published OpenData CSV, as described by the manifest.
 * @export
 * @interface OpenDataColumnDto
 */
export interface OpenDataColumnDto {
  /**
   * Column header.
   * @type {string}
   * @memberof OpenDataColumnDto
   */
  name?: string
  /**
   * Share of rows where the column is filled, between 0 and 1.
   * @type {number}
   * @memberof OpenDataColumnDto
   */
  fillRate?: number
  /**
   * Unit of the values.
   * @type {string}
   * @memberof OpenDataColumnDto
   */
  unit?: string | null
  /**
   * Label of the column.
   * @type {string}
   * @memberof OpenDataColumnDto
   */
  description?: string | null
  /**
   * Origin of the values.
   * @type {string}
   * @memberof OpenDataColumnDto
   */
  source?: string | null
}

/**
 * Check if a given object implements the OpenDataColumnDto interface.
 */
export function instanceOfOpenDataColumnDto(
  value: object
): value is OpenDataColumnDto {
  return true
}

export function OpenDataColumnDtoFromJSON(json: any): OpenDataColumnDto {
  return OpenDataColumnDtoFromJSONTyped(json, false)
}

export function OpenDataColumnDtoFromJSONTyped(
  json: any,
  ignoreDiscriminator: boolean
): OpenDataColumnDto {
  if (json == null) {
    return json
  }
  return {
    name: json['name'] == null ? undefined : json['name'],
    fillRate: json['fillRate'] == null ? undefined : json['fillRate'],
    unit: json['unit'] == null ? undefined : json['unit'],
    description: json['description'] == null ? undefined : json['description'],
    source: json['source'] == null ? undefined : json['source'],
  }
}

export function OpenDataColumnDtoToJSON(json: any): OpenDataColumnDto {
  return OpenDataColumnDtoToJSONTyped(json, false)
}

export function OpenDataColumnDtoToJSONTyped(
  value?: OpenDataColumnDto | null,
  ignoreDiscriminator: boolean = false
): any {
  if (value == null) {
    return value
  }

  return {
    name: value['name'],
    fillRate: value['fillRate'],
    unit: value['unit'],
    description: value['description'],
    source: value['source'],
  }
}
