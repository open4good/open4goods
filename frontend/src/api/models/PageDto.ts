/* tslint:disable */
/* eslint-disable */
/**
 * OpenAPI
 * OpenAPI
 *
 * The version of the OpenAPI document: 0.0.1
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import { mapValues } from '../runtime';
import type { PageMetaDto } from './PageMetaDto';
import {
    PageMetaDtoFromJSON,
    PageMetaDtoFromJSONTyped,
    PageMetaDtoToJSON,
    PageMetaDtoToJSONTyped,
} from './PageMetaDto';

/**
 * 
 * @export
 * @interface PageDto
 */
export interface PageDto {
    /**
     * Pagination metadata
     * @type {PageMetaDto}
     * @memberof PageDto
     */
    page?: PageMetaDto;
    /**
     * Current page content
     * @type {Array<any>}
     * @memberof PageDto
     */
    data?: Array<any>;
}

/**
 * Check if a given object implements the PageDto interface.
 */
export function instanceOfPageDto(value: object): value is PageDto {
    return true;
}

export function PageDtoFromJSON(json: any): PageDto {
    return PageDtoFromJSONTyped(json, false);
}

export function PageDtoFromJSONTyped(json: any, ignoreDiscriminator: boolean): PageDto {
    if (json == null) {
        return json;
    }
    return {
        
        'page': json['page'] == null ? undefined : PageMetaDtoFromJSON(json['page']),
        'data': json['data'] == null ? undefined : json['data'],
    };
}

export function PageDtoToJSON(json: any): PageDto {
    return PageDtoToJSONTyped(json, false);
}

export function PageDtoToJSONTyped(value?: PageDto | null, ignoreDiscriminator: boolean = false): any {
    if (value == null) {
        return value;
    }

    return {
        
        'page': PageMetaDtoToJSON(value['page']),
        'data': value['data'],
    };
}

