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

package fr.gouv.tchap.model;

import java.util.Date;

import fr.gouv.tchap.media.AntiVirusScanStatus;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class MediaScan extends RealmObject {

    @PrimaryKey
    private String url;
    // The current scan status.
    private String antiVirusScanStatus = AntiVirusScanStatus.UNKNOWN.toString();
    // The potential information returned by the anti-virus scanner.
    private String antiVirusScanInfo = null;
    // The last update date.
    private Date antiVirusScanDate = null;

    public String getUrl() { return  url; }
    public void setUrl(String url) { this.url = url; }

    public AntiVirusScanStatus getAntiVirusScanStatus() {
        return AntiVirusScanStatus.valueOf(antiVirusScanStatus);
    }
    public void setAntiVirusScanStatus(AntiVirusScanStatus antiVirusScanStatus) {
        this.antiVirusScanStatus = antiVirusScanStatus.toString();
    }

    public String getAntiVirusScanInfo() {
        return antiVirusScanInfo;
    }
    public void setAntiVirusScanInfo(String info) {
        this.antiVirusScanInfo = info;
    }

    public Date getAntiVirusScanDate() {
        return antiVirusScanDate;
    }
    public void setAntiVirusScanDate(Date date) {
        this.antiVirusScanDate = date;
    }
}
