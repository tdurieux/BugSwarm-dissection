/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 *
 * Code generated by Microsoft (R) AutoRest Code Generator.
 */

package com.microsoft.azure.management.graphrbac.implementation;

import java.util.List;
import com.microsoft.azure.management.graphrbac.KeyCredential;
import com.microsoft.azure.management.graphrbac.PasswordCredential;

/**
 * Request parameters for updating an existing application.
 */
public class ApplicationUpdateParametersInner {
    /**
     * Application display name.
     */
    private String displayName;

    /**
     * Application homepage.
     */
    private String homepage;

    /**
     * Application Uris.
     */
    private List<String> identifierUris;

    /**
     * Application reply Urls.
     */
    private List<String> replyUrls;

    /**
     * Gets or sets the list of KeyCredential objects.
     */
    private List<KeyCredential> keyCredentials;

    /**
     * Gets or sets the list of PasswordCredential objects.
     */
    private List<PasswordCredential> passwordCredentials;

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
     * @return the ApplicationUpdateParametersInner object itself.
     */
    public ApplicationUpdateParametersInner withDisplayName(String displayName) {
        this.displayName = displayName;
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
     * @return the ApplicationUpdateParametersInner object itself.
     */
    public ApplicationUpdateParametersInner withHomepage(String homepage) {
        this.homepage = homepage;
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
     * @return the ApplicationUpdateParametersInner object itself.
     */
    public ApplicationUpdateParametersInner withIdentifierUris(List<String> identifierUris) {
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
     * @return the ApplicationUpdateParametersInner object itself.
     */
    public ApplicationUpdateParametersInner withReplyUrls(List<String> replyUrls) {
        this.replyUrls = replyUrls;
        return this;
    }

    /**
     * Get the keyCredentials value.
     *
     * @return the keyCredentials value
     */
    public List<KeyCredential> keyCredentials() {
        return this.keyCredentials;
    }

    /**
     * Set the keyCredentials value.
     *
     * @param keyCredentials the keyCredentials value to set
     * @return the ApplicationUpdateParametersInner object itself.
     */
    public ApplicationUpdateParametersInner withKeyCredentials(List<KeyCredential> keyCredentials) {
        this.keyCredentials = keyCredentials;
        return this;
    }

    /**
     * Get the passwordCredentials value.
     *
     * @return the passwordCredentials value
     */
    public List<PasswordCredential> passwordCredentials() {
        return this.passwordCredentials;
    }

    /**
     * Set the passwordCredentials value.
     *
     * @param passwordCredentials the passwordCredentials value to set
     * @return the ApplicationUpdateParametersInner object itself.
     */
    public ApplicationUpdateParametersInner withPasswordCredentials(List<PasswordCredential> passwordCredentials) {
        this.passwordCredentials = passwordCredentials;
        return this;
    }

}
