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
import type { SortObject } from './SortObject';
import {
    SortObjectFromJSON,
    SortObjectFromJSONTyped,
    SortObjectToJSON,
    SortObjectToJSONTyped,
} from './SortObject';

/**
 * 
 * @export
 * @interface PageableObject
 */
export interface PageableObject {
    /**
     * 
     * @type {number}
     * @memberof PageableObject
     */
    offset?: number;
    /**
     * 
     * @type {SortObject}
     * @memberof PageableObject
     */
    sort?: SortObject;
    /**
     * 
     * @type {boolean}
     * @memberof PageableObject
     */
    paged?: boolean;
    /**
     * 
     * @type {number}
     * @memberof PageableObject
     */
    pageNumber?: number;
    /**
     * 
     * @type {number}
     * @memberof PageableObject
     */
    pageSize?: number;
    /**
     * 
     * @type {boolean}
     * @memberof PageableObject
     */
    unpaged?: boolean;
}

/**
 * Check if a given object implements the PageableObject interface.
 */
export function instanceOfPageableObject(value: object): value is PageableObject {
    return true;
}

export function PageableObjectFromJSON(json: any): PageableObject {
    return PageableObjectFromJSONTyped(json, false);
}

export function PageableObjectFromJSONTyped(json: any, ignoreDiscriminator: boolean): PageableObject {
    if (json == null) {
        return json;
    }
    return {
        
        'offset': json['offset'] == null ? undefined : json['offset'],
        'sort': json['sort'] == null ? undefined : SortObjectFromJSON(json['sort']),
        'paged': json['paged'] == null ? undefined : json['paged'],
        'pageNumber': json['pageNumber'] == null ? undefined : json['pageNumber'],
        'pageSize': json['pageSize'] == null ? undefined : json['pageSize'],
        'unpaged': json['unpaged'] == null ? undefined : json['unpaged'],
    };
}

export function PageableObjectToJSON(json: any): PageableObject {
    return PageableObjectToJSONTyped(json, false);
}

export function PageableObjectToJSONTyped(value?: PageableObject | null, ignoreDiscriminator: boolean = false): any {
    if (value == null) {
        return value;
    }

    return {
        
        'offset': value['offset'],
        'sort': SortObjectToJSON(value['sort']),
        'paged': value['paged'],
        'pageNumber': value['pageNumber'],
        'pageSize': value['pageSize'],
        'unpaged': value['unpaged'],
    };
}

