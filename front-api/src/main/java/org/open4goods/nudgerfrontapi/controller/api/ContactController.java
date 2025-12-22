package org.open4goods.nudgerfrontapi.controller.api;

import org.open4goods.model.RolesConstants;
import org.open4goods.nudgerfrontapi.dto.contact.ContactRequestDto;
import org.open4goods.nudgerfrontapi.dto.contact.ContactResponseDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.ContactService;
import org.open4goods.nudgerfrontapi.utils.IpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * REST controller exposing the public contact form used by the Nudger frontend.
 */
@RestController
@RequestMapping("/contact")
@Validated
@PreAuthorize("hasAnyAuthority('" + RolesConstants.ROLE_FRONTEND + "', '" + RolesConstants.ROLE_EDITOR + "')")
@Tag(name = "Contact", description = "Public contact form")
public class ContactController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ContactController.class);

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    @Operation(
            summary = "Submit a contact message",
            description = "Verify captcha token and forward the message to the support mailbox. Supports optional template identifiers to prefill the subject and body.",
            parameters = {
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields (future use).",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    description = "Contact message payload.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ContactRequestDto.class))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Message forwarded",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ContactResponseDto.class))),
                    @ApiResponse(responseCode = "400", description = "Captcha verification failed",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ContactResponseDto.class))),
                    @ApiResponse(responseCode = "500", description = "Mail dispatch failed",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ContactResponseDto.class)))
            }
    )
    public ResponseEntity<ContactResponseDto> submit(@Valid @RequestBody ContactRequestDto request,
                                                     @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage,
                                                     HttpServletRequest httpRequest) {
        try {
            contactService.submit(request, IpUtils.getIp(httpRequest), domainLanguage);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noCache())
                    .header("X-Locale", domainLanguage.languageTag())
                    .body(new ContactResponseDto(true));
        } catch (SecurityException e) {
        	LOGGER.warn("Security exception while sending email",e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .cacheControl(CacheControl.noCache())
                    .header("X-Locale", domainLanguage.languageTag())
                    .body(new ContactResponseDto(false));
        } catch (Exception e) {
        	LOGGER.warn("Exception while sending email",e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .cacheControl(CacheControl.noCache())
                    .header("X-Locale", domainLanguage.languageTag())
                    .body(new ContactResponseDto(false));
        }
    }
}
