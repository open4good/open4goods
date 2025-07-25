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
 * @interface BlogTagDto
 */
export interface BlogTagDto {
    /**
     * Tag name
     * @type {string}
     * @memberof BlogTagDto
     */
    name?: string;
    /**
     * Number of posts having this tag
     * @type {number}
     * @memberof BlogTagDto
     */
    count?: number;
}

/**
 * Check if a given object implements the BlogTagDto interface.
 */
export function instanceOfBlogTagDto(value: object): value is BlogTagDto {
    return true;
}

export function BlogTagDtoFromJSON(json: any): BlogTagDto {
    return BlogTagDtoFromJSONTyped(json, false);
}

export function BlogTagDtoFromJSONTyped(json: any, ignoreDiscriminator: boolean): BlogTagDto {
    if (json == null) {
        return json;
    }
    return {
        
        'name': json['name'] == null ? undefined : json['name'],
        'count': json['count'] == null ? undefined : json['count'],
    };
}

export function BlogTagDtoToJSON(json: any): BlogTagDto {
    return BlogTagDtoToJSONTyped(json, false);
}

export function BlogTagDtoToJSONTyped(value?: BlogTagDto | null, ignoreDiscriminator: boolean = false): any {
    if (value == null) {
        return value;
    }

    return {
        
        'name': value['name'],
        'count': value['count'],
    };
}

