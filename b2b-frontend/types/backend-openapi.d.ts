export interface paths {
    "/api/v1/billing/stripe/webhook": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["webhook"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/products/{gtin}/price": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Get product price facet
         * @description Retrieves the price facet and aggregate offers for a product using its GTIN. Requires a valid API key. Billed only if fresh offers exist.
         */
        get: operations["getProductPrice"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
}
export type webhooks = Record<string, never>;
export interface components {
    schemas: {
        /**
         * @description Metering details for one response facet.
         * @default null
         */
        B2bFacetMeta: {
            /**
             * @description Facet identifier.
             * @default
             * @example price
             */
            id: string;
            /**
             * Format: int64
             * @description Credit price for this facet.
             * @default
             * @example 1
             */
            credits: number;
            /**
             * @description Whether this facet was returned in the response.
             * @default false
             * @example true
             */
            served: boolean;
            /**
             * @description Whether serving this facet consumed credits.
             * @default false
             * @example true
             */
            billable: boolean;
        };
        /**
         * @description Coverage status for one requested response facet.
         * @default null
         */
        B2bCoverageMeta: {
            /**
             * @description Facet identifier.
             * @default
             * @example price
             */
            id: string;
            /**
             * @description Whether the product has enough data for this facet.
             * @default false
             * @example true
             */
            covered: boolean;
        };
        /**
         * @description Metadata returned with every successful Product Data API response.
         * @default null
         */
        B2bMeta: {
            /**
             * @description Request identifier for support and idempotency correlation.
             * @default
             * @example pdreq_01JZ7V8N9P4K6T2QW3E5R7Y8U9
             */
            requestId: string;
            /**
             * Format: date-time
             * @description Response generation timestamp.
             * @default
             * @example 2026-06-15T10:15:30Z
             */
            timestamp: string;
            /**
             * @description Resolved response language.
             * @default
             * @example en
             */
            language: string;
            /**
             * Format: int64
             * @description Credits consumed by this request.
             * @default
             * @example 1
             */
            creditsConsumed: number;
            /**
             * Format: int64
             * @description Remaining organization credit balance.
             * @default
             * @example 249
             */
            creditsRemaining: number;
            /**
             * @description Whether this request consumed credits.
             * @default false
             * @example true
             */
            billable: boolean;
            /**
             * Format: int32
             * @description Freshness window applied by the endpoint, in days.
             * @default
             * @example 30
             */
            freshnessDays: number;
            /**
             * Format: int64
             * @description Server-side response time in milliseconds.
             * @default
             * @example 42
             */
            responseTimeMs: number;
            /** @description Per-facet metering metadata. */
            facets?: components["schemas"]["B2bFacetMeta"][];
            /** @description Per-facet coverage metadata. */
            coverage?: components["schemas"]["B2bCoverageMeta"][];
        };
        /**
         * @description Standard envelope for successful Product Data API responses.
         * @default null
         */
        B2bResponse: {
            /**
             * @description Endpoint-specific sanitized payload.
             * @default
             */
            data: unknown;
            /**
             * @description Response metadata and metering details.
             * @default
             */
            meta: components["schemas"]["B2bMeta"];
        };
        ProblemDetail: {
            /** Format: uri */
            type?: string;
            title?: string;
            /** Format: int32 */
            status?: number;
            detail?: string;
            /** Format: uri */
            instance?: string;
            properties?: {
                [key: string]: unknown;
            };
        };
    };
    responses: never;
    parameters: never;
    requestBodies: never;
    headers: never;
    pathItems: never;
}
export type $defs = Record<string, never>;
export interface operations {
    webhook: {
        parameters: {
            query?: never;
            header: {
                "Stripe-Signature": string;
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": string;
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getProductPrice: {
        parameters: {
            query?: {
                /**
                 * @description Locale language for text/display names (e.g. 'en', 'fr')
                 * @example en
                 */
                language?: string;
            };
            header?: never;
            path: {
                /**
                 * @description Barcode identifier (GTIN-8, GTIN-12, GTIN-13, or GTIN-14)
                 * @example 0885909950805
                 */
                gtin: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Product price details retrieved successfully. */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["B2bResponse"];
                };
            };
            /** @description Invalid GTIN checksum/format or parameters. */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Missing, invalid, or revoked API key. */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Insufficient credits for the request. */
            402: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Product not found. */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Rate limit exceeded. */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Unexpected internal server error. */
            500: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
}
