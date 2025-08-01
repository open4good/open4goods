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
 * @interface ProductResourcesDto
 */
export interface ProductResourcesDto {
    /**
     * Image URLs
     * @type {Array<string>}
     * @memberof ProductResourcesDto
     */
    images?: Array<string>;
    /**
     * Video URLs
     * @type {Array<string>}
     * @memberof ProductResourcesDto
     */
    videos?: Array<string>;
    /**
     * PDF URLs
     * @type {Array<string>}
     * @memberof ProductResourcesDto
     */
    pdfs?: Array<string>;
    /**
     * Cover image URL
     * @type {string}
     * @memberof ProductResourcesDto
     */
    coverPath?: string;
}

/**
 * Check if a given object implements the ProductResourcesDto interface.
 */
export function instanceOfProductResourcesDto(value: object): value is ProductResourcesDto {
    return true;
}

export function ProductResourcesDtoFromJSON(json: any): ProductResourcesDto {
    return ProductResourcesDtoFromJSONTyped(json, false);
}

export function ProductResourcesDtoFromJSONTyped(json: any, ignoreDiscriminator: boolean): ProductResourcesDto {
    if (json == null) {
        return json;
    }
    return {
        
        'images': json['images'] == null ? undefined : json['images'],
        'videos': json['videos'] == null ? undefined : json['videos'],
        'pdfs': json['pdfs'] == null ? undefined : json['pdfs'],
        'coverPath': json['coverPath'] == null ? undefined : json['coverPath'],
    };
}

export function ProductResourcesDtoToJSON(json: any): ProductResourcesDto {
    return ProductResourcesDtoToJSONTyped(json, false);
}

export function ProductResourcesDtoToJSONTyped(value?: ProductResourcesDto | null, ignoreDiscriminator: boolean = false): any {
    if (value == null) {
        return value;
    }

    return {
        
        'images': value['images'],
        'videos': value['videos'],
        'pdfs': value['pdfs'],
        'coverPath': value['coverPath'],
    };
}

