/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 *
 * Code generated by Microsoft (R) AutoRest Code Generator.
 */

package com.microsoft.azure.management.graphrbac.implementation;

import java.util.List;

/**
 * Active Directory user information.
 */
public class ApplicationInner {
    /**
     * Gets or sets object Id.
     */
    private String objectId;

    /**
     * Gets or sets object type.
     */
    private String objectType;

    /**
     * Gets or sets application Id.
     */
    private String appId;

    /**
     * Gets or sets application permissions.
     */
    private List<String> appPermissions;

    /**
     * Indicates if the application will be available to other tenants.
     */
    private Boolean availableToOtherTenants;

    /**
     * Gets or sets the displayName.
     */
    private String displayName;

    /**
     * Gets or sets the application identifier Uris.
     */
    private List<String> identifierUris;

    /**
     * Gets or sets the application reply Urls.
     */
    private List<String> replyUrls;

    /**
     * Application homepage.
     */
    private String homepage;

    /**
     * Get the objectId value.
     *
     * @return the objectId value
     */
    public String objectId() {
        return this.objectId;
    }

    /**
     * Set the objectId value.
     *
     * @param objectId the objectId value to set
     * @return the ApplicationInner object itself.
     */
    public ApplicationInner withObjectId(String objectId) {
        this.objectId = objectId;
        return this;
    }

    /**
     * Get the objectType value.
     *
     * @return the objectType value
     */
    public String objectType() {
        return this.objectType;
    }

    /**
     * Set the objectType value.
     *
     * @param objectType the objectType value to set
     * @return the ApplicationInner object itself.
     */
    public ApplicationInner withObjectType(String objectType) {
        this.objectType = objectType;
        return this;
    }

    /**
     * Get the appId value.
     *
     * @return the appId value
     */
    public String appId() {
        return this.appId;
    }

    /**
     * Set the appId value.
     *
     * @param appId the appId value to set
     * @return the ApplicationInner object itself.
     */
    public ApplicationInner withAppId(String appId) {
        this.appId = appId;
        return this;
    }

    /**
     * Get the appPermissions value.
     *
     * @return the appPermissions value
     */
    public List<String> appPermissions() {
        return this.appPermissions;
    }

    /**
     * Set the appPermissions value.
     *
     * @param appPermissions the appPermissions value to set
     * @return the ApplicationInner object itself.
     */
    public ApplicationInner withAppPermissions(List<String> appPermissions) {
        this.appPermissions = appPermissions;
        return this;
    }

    /**
     * Get the availableToOtherTenants value.
     *
     * @return the availableToOtherTenants value
     */
    public Boolean availableToOtherTenants() {
        return this.availableToOtherTenants;
    }

    /**
     * Set the availableToOtherTenants value.
     *
     * @param availableToOtherTenants the availableToOtherTenants value to set
     * @return the ApplicationInner object itself.
     */
    public ApplicationInner withAvailableToOtherTenants(Boolean availableToOtherTenants) {
        this.availableToOtherTenants = availableToOtherTenants;
        return this;
    }

    /**
     * Get the displayName value.
     *
     * @return the displayName value
     */
    public String displayName() {
        return this.displayName;
    }

    /**
     * Set the displayName value.
     *
     * @param displayName the displayName value to set
     * @return the ApplicationInner object itself.
     */
    public ApplicationInner withDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    /**
     * Get the identifierUris value.
     *
     * @return the identifierUris value
     */
    public List<String> identifierUris() {
        return this.identifierUris;
    }

    /**
     * Set the identifierUris value.
     *
     * @param identifierUris the identifierUris value to set
     * @return the ApplicationInner object itself.
     */
    public ApplicationInner withIdentifierUris(List<String> identifierUris) {
        this.identifierUris = identifierUris;
        return this;
    }

    /**
     * Get the replyUrls value.
     *
     * @return the replyUrls value
     */
    public List<String> replyUrls() {
        return this.replyUrls;
    }

    /**
     * Set the replyUrls value.
     *
     * @param replyUrls the replyUrls value to set
     * @return the ApplicationInner object itself.
     */
    public ApplicationInner withReplyUrls(List<String> replyUrls) {
        this.replyUrls = replyUrls;
        return this;
    }

    /**
     * Get the homepage value.
     *
     * @return the homepage value
     */
    public String homepage() {
        return this.homepage;
    }

    /**
     * Set the homepage value.
     *
     * @param homepage the homepage value to set
     * @return the ApplicationInner object itself.
     */
    public ApplicationInner withHomepage(String homepage) {
        this.homepage = homepage;
        return this;
    }

}
