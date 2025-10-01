package org.open4goods.nudgerfrontapi.dto.xwiki;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO representing a flattened view of an XWiki {@code FullPage}.
 *
 * @param htmlContent                rendered HTML content of the page
 * @param id                         technical identifier of the XWiki document
 * @param fullName                   fully qualified document name
 * @param wiki                       wiki identifier hosting the page
 * @param space                      hierarchical space containing the document
 * @param name                       document local name
 * @param title                      current display title of the page
 * @param rawTitle                   raw title before localisation or processing
 * @param parent                     identifier of the parent document
 * @param parentId                   identifier of the parent document without wiki prefix
 * @param version                    human readable version string
 * @param author                     technical author reference
 * @param authorName                 display name of the author
 * @param xwikiRelativeUrl           relative URL to the page rendered by XWiki
 * @param xwikiAbsoluteUrl           absolute URL to the page rendered by XWiki
 * @param syntax                     syntax used to author the page content
 * @param language                   current language of the document
 * @param majorVersion               major version number
 * @param minorVersion               minor version number
 * @param hidden                     whether the page is hidden from listings
 * @param created                    ISO timestamp representing document creation date
 * @param creator                    technical reference of the page creator
 * @param creatorName                display name of the page creator
 * @param modified                   ISO timestamp representing the last modification date
 * @param modifier                   technical reference of the last modifier
 * @param modifierName               display name of the last modifier
 * @param originalMetadataAuthor     technical reference that provided the original metadata
 * @param originalMetadataAuthorName display name for the original metadata author
 * @param layout                     CMS specific layout identifier
 * @param pageTitle                  preferred title to display on the frontend
 * @param metaTitle                  SEO meta title
 * @param width                      layout width directive
 * @param metaDescription            SEO meta description
 * @param editLink                   direct edit link for the page in XWiki
 */
public record FullPageDto(
        @Schema(description = "Rendered HTML content of the page.")
        String htmlContent,

        @Schema(description = "Technical identifier of the XWiki document.", example = "xwiki:webpages.default.legal-notice.WebHome")
        String id,

        @Schema(description = "Fully qualified document name.", example = "webpages.default.legal-notice.WebHome")
        String fullName,

        @Schema(description = "Wiki identifier hosting the page.", example = "xwiki")
        String wiki,

        @Schema(description = "Hierarchical space containing the document.", example = "webpages.default.legal-notice")
        String space,

        @Schema(description = "Document local name.", example = "WebHome")
        String name,

        @Schema(description = "Current display title of the page.", example = "legal-notice")
        String title,

        @Schema(description = "Raw title before localisation or processing.", example = "legal-notice")
        String rawTitle,

        @Schema(description = "Identifier of the parent document.", example = "xwiki:webpages.default.legal-notice.WebHome")
        String parent,

        @Schema(description = "Identifier of the parent document without wiki prefix.", example = "xwiki:webpages.default.legal-notice.WebHome")
        String parentId,

        @Schema(description = "Human readable version string.", example = "2.1")
        String version,

        @Schema(description = "Technical author reference.", example = "XWiki.Goulven", nullable = true)
        String author,

        @Schema(description = "Display name of the author.", nullable = true)
        String authorName,

        @Schema(description = "Relative URL to the page rendered by XWiki.", format = "uri", example = "https://wiki.nudger.fr/bin/view/webpages/default/legal-notice/")
        String xwikiRelativeUrl,

        @Schema(description = "Absolute URL to the page rendered by XWiki.", format = "uri", example = "https://wiki.nudger.fr/bin/view/webpages/default/legal-notice/")
        String xwikiAbsoluteUrl,

        @Schema(description = "Syntax used to author the page content.", example = "xwiki/2.1")
        String syntax,

        @Schema(description = "Current language of the document.", example = "fr")
        String language,

        @Schema(description = "Major version number.", example = "2")
        Integer majorVersion,

        @Schema(description = "Minor version number.", example = "1")
        Integer minorVersion,

        @Schema(description = "Whether the page is hidden from listings.", example = "false")
        boolean hidden,

        @Schema(description = "ISO timestamp representing document creation date.", format = "date-time", example = "2024-08-08T13:13:55.000+00:00")
        String created,

        @Schema(description = "Technical reference of the page creator.", example = "XWiki.o4g", nullable = true)
        String creator,

        @Schema(description = "Display name of the page creator.", nullable = true)
        String creatorName,

        @Schema(description = "ISO timestamp representing the last modification date.", format = "date-time", example = "2024-08-08T13:13:55.000+00:00")
        String modified,

        @Schema(description = "Technical reference of the last modifier.", example = "XWiki.o4g", nullable = true)
        String modifier,

        @Schema(description = "Display name of the last modifier.", nullable = true)
        String modifierName,

        @Schema(description = "Technical reference that provided the original metadata.", example = "xwiki:XWiki.Goulven", nullable = true)
        String originalMetadataAuthor,

        @Schema(description = "Display name for the original metadata author.", nullable = true)
        String originalMetadataAuthorName,

        @Schema(description = "CMS specific layout identifier.", example = "layout3", nullable = true)
        String layout,

        @Schema(description = "Preferred title to display on the frontend.", example = "Les mentions légales et les CGU de Nudger", nullable = true)
        String pageTitle,

        @Schema(description = "SEO meta title.", example = "Mentions légales | Nudger", nullable = true)
        String metaTitle,

        @Schema(description = "Layout width directive.", allowableValues = {"container", "container-fluid", "container-semi-fluid"}, example = "container-semi-fluid", nullable = true)
        String width,

        @Schema(description = "SEO meta description.", example = "Les mentions légales de Nudger", nullable = true)
        String metaDescription,

        @Schema(description = "Direct edit link for the page in XWiki.", format = "uri", example = "https://wiki.nudger.fr/bin/edit/webpages/default/legal-notice/WebHome", nullable = true)
        String editLink) {
}
