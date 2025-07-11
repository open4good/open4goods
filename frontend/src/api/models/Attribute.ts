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
import type { Link } from './Link';
import {
    LinkFromJSON,
    LinkFromJSONTyped,
    LinkToJSON,
    LinkToJSONTyped,
} from './Link';

/**
 * 
 * @export
 * @interface Attribute
 */
export interface Attribute {
    /**
     * 
     * @type {Array<Link>}
     * @memberof Attribute
     */
    links?: Array<Link>;
    /**
     * 
     * @type {string}
     * @memberof Attribute
     */
    name?: string;
    /**
     * 
     * @type {string}
     * @memberof Attribute
     */
    value?: string;
}

/**
 * Check if a given object implements the Attribute interface.
 */
export function instanceOfAttribute(value: object): value is Attribute {
    return true;
}

export function AttributeFromJSON(json: any): Attribute {
    return AttributeFromJSONTyped(json, false);
}

export function AttributeFromJSONTyped(json: any, ignoreDiscriminator: boolean): Attribute {
    if (json == null) {
        return json;
    }
    return {
        
        'links': json['links'] == null ? undefined : ((json['links'] as Array<any>).map(LinkFromJSON)),
        'name': json['name'] == null ? undefined : json['name'],
        'value': json['value'] == null ? undefined : json['value'],
    };
}

export function AttributeToJSON(json: any): Attribute {
    return AttributeToJSONTyped(json, false);
}

export function AttributeToJSONTyped(value?: Attribute | null, ignoreDiscriminator: boolean = false): any {
    if (value == null) {
        return value;
    }

    return {
        
        'links': value['links'] == null ? undefined : ((value['links'] as Array<any>).map(LinkToJSON)),
        'name': value['name'],
        'value': value['value'],
    };
}

