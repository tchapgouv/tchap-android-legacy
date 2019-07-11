/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.gouv.tchap.sdk.rest.model;

import com.google.gson.annotations.SerializedName;

/**
 * Class to contain a password policy.
 */
public class PasswordPolicy {
    // Minimum accepted length for a password.
    @SerializedName("m.minimum_length")
    public Integer minLength;

    // Whether a password must contain at least one digit.
    @SerializedName("m.require_digit")
    public Boolean isDigitRequired;

    // Whether a password must contain at least one symbol.
    @SerializedName("m.require_symbol")
    public Boolean isSymbolRequired;

    // Whether a password must contain at least one uppercase letter.
    @SerializedName("m.require_uppercase")
    public Boolean isUppercaseRequired;

    // Whether a password must contain at least one lowercase letter.
    @SerializedName("m.require_lowercase")
    public Boolean isLowercaseRequired;
}
