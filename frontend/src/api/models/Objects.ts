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
import type { ObjectSummary } from './ObjectSummary';
import {
    ObjectSummaryFromJSON,
    ObjectSummaryFromJSONTyped,
    ObjectSummaryToJSON,
    ObjectSummaryToJSONTyped,
} from './ObjectSummary';
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
 * @interface Objects
 */
export interface Objects {
    /**
     * 
     * @type {Array<Link>}
     * @memberof Objects
     */
    links?: Array<Link>;
    /**
     * 
     * @type {Array<ObjectSummary>}
     * @memberof Objects
     */
    objectSummaries?: Array<ObjectSummary>;
}

/**
 * Check if a given object implements the Objects interface.
 */
export function instanceOfObjects(value: object): value is Objects {
    return true;
}

export function ObjectsFromJSON(json: any): Objects {
    return ObjectsFromJSONTyped(json, false);
}

export function ObjectsFromJSONTyped(json: any, ignoreDiscriminator: boolean): Objects {
    if (json == null) {
        return json;
    }
    return {
        
        'links': json['links'] == null ? undefined : ((json['links'] as Array<any>).map(LinkFromJSON)),
        'objectSummaries': json['objectSummaries'] == null ? undefined : ((json['objectSummaries'] as Array<any>).map(ObjectSummaryFromJSON)),
    };
}

export function ObjectsToJSON(json: any): Objects {
    return ObjectsToJSONTyped(json, false);
}

export function ObjectsToJSONTyped(value?: Objects | null, ignoreDiscriminator: boolean = false): any {
    if (value == null) {
        return value;
    }

    return {
        
        'links': value['links'] == null ? undefined : ((value['links'] as Array<any>).map(LinkToJSON)),
        'objectSummaries': value['objectSummaries'] == null ? undefined : ((value['objectSummaries'] as Array<any>).map(ObjectSummaryToJSON)),
    };
}

