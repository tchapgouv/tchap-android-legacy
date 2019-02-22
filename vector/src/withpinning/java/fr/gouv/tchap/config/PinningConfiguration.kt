/*
 * Copyright 2018 New Vector Ltd
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

package fr.gouv.tchap.config

// Pinning is Enabled
const val ENABLE_CERTIFICATE_PINNING = true

// Put the list of fingerprint here.
// Example of value: "07:89:7A:A0:30:82:99:95:E6:17:5D:1F:34:5D:8D:0C:67:82:63:1C:1F:57:20:75:42:91:F7:8B:28:03:54:A2"
val CERTIFICATE_FINGERPRINT_LIST = listOf<String>("90:45:FA:C4:7B:B4:8D:62:50:5F:19:EC:4E:EB:95:30:A2:B7:82:8D:E1:B9:CD:DB:18:4E:A4:93:DE:70:F3:B1")
