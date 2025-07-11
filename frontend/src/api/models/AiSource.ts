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
/**
 * 
 * @export
 * @interface AiSource
 */
export interface AiSource {
    /**
     * 
     * @type {number}
     * @memberof AiSource
     */
    number: number;
    /**
     * 
     * @type {string}
     * @memberof AiSource
     */
    name: string;
    /**
     * 
     * @type {string}
     * @memberof AiSource
     */
    description: string;
    /**
     * 
     * @type {string}
     * @memberof AiSource
     */
    url: string;
}

/**
 * Check if a given object implements the AiSource interface.
 */
export function instanceOfAiSource(value: object): value is AiSource {
    if (!('number' in value) || value['number'] === undefined) return false;
    if (!('name' in value) || value['name'] === undefined) return false;
    if (!('description' in value) || value['description'] === undefined) return false;
    if (!('url' in value) || value['url'] === undefined) return false;
    return true;
}

export function AiSourceFromJSON(json: any): AiSource {
    return AiSourceFromJSONTyped(json, false);
}

export function AiSourceFromJSONTyped(json: any, ignoreDiscriminator: boolean): AiSource {
    if (json == null) {
        return json;
    }
    return {
        
        'number': json['number'],
        'name': json['name'],
        'description': json['description'],
        'url': json['url'],
    };
}

export function AiSourceToJSON(json: any): AiSource {
    return AiSourceToJSONTyped(json, false);
}

export function AiSourceToJSONTyped(value?: AiSource | null, ignoreDiscriminator: boolean = false): any {
    if (value == null) {
        return value;
    }

    return {
        
        'number': value['number'],
        'name': value['name'],
        'description': value['description'],
        'url': value['url'],
    };
}

