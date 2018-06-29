Changes in Tchap 1.0.2 (2018-06-29)
===================================================

Improvements:
 * Change the application id with "fr.gouv.tchap".
 * Update matrix-sdk.aar lib - build #1820 - Revision: 85a7423c23cbf82e1f447f81dc1ff4661884438d
 * Encrypt event content for invited members when some device id are available for them.
 * Create a new room and invite members : the disabled buttons must have an alpha #254
 * Contacts picker: Improve Tchap contacts display #261
 * Room creation: Do not prompt the user if the alias is already used #249

Bug Fixes:
 * Authentication screen: Improve keyboard handling #251
 * Home screen: enlarge clickable area of the tab (Conversations/Contacts) #268
 * "Inviter par mail": check whether an account is already known for the provided email #250

Changes in Tchap 1.0.1 (2018-06-26)
===================================================
 
Bug Fixes:
 * Select back on a recently joined room make the user leave the app #255
 * Unable to accept an invitation without giving consent #253
 * Discussion: some discussions are missing in the conversations list #252
 * Room summary : sender display name is wrong. #258

Changes in Tchap 1.0.0 (2018-06-25)
===================================================
 
Improvements:
 * Update matrix-sdk lib: build 1815 - Revision: b9d425adf430f05312697f5bc2f5c9dce9d1c912
 * Antivirus: Add MediaScan in the attachments handling #122 (Encrypted AES keys are not supported yet)
 * Authentication screen: remove Tchap icon, add ActionBar title #187
 * Room creation - Set Avatar, Name, Privacy and Participants #127
 * Contacts: new direct chat creation #176
 * Invitation des contacts: Add the button at the top of contacts list #173
 * Invitation des contacts: Update the non-tchap contacts list display #174
 * Invitation des contacts: Hide the created room used to invite a contact #175
 * Invitation des contacts: Check whether the contact can register before inviting him #184
 * Invitation des contacts: Update "inviter par mail" button #177
 * Burger menu: update design #191
 * New build flavor to include/exclude VoIP features and related code PR#202
 * Home screen: Remove the search icon and the menu icon from the ActionBar #188
 * Theme: Update Tchap colors #178
 * Change the public rooms access (Use the floating button) #196
 * Redesign headers and details screens for room activities #217
 * Home screen - Conversation View: Update design #190
 * Home screen - Contact View: remove connexion info, highlight contact domain #189
 * Tchap links: Update all the existing riot links #185
 * Hide radio button on menu #230
 * Nouveau changement de terminologie : les salons redeviennent des salons, et les dialogue des discussions #186
 * Disable permalink, remove matrix.to handling #193
 * Enlarge contact's list #246
 * Nouvelle Discussion: list only Tchap users #194
 
Bug Fixes:
 * Some non-tchap users are displayed in the Contacts list #181
 * Contact's list is not correct when inviting to a room #234
 * Focus when click on search icon #223

Changes in Tchap 0.1.8 (2018-05-30)
===================================================
 
Improvements:
 * Update matrix-sdk lib: build 1796 - Revision: 8732182a9c43adca7d6e372ea2f6f0375e6fa49f
 * Enable Kotlin, and upgrade gradle and build tools PR #158
 * Update okhttp to version 3.10 and retrofit to version 2.4 PR #158
 * Replace the bottom bar by a top bar #154
 * Remove Analytics tracking until Tchap defines its own Piwik/Matomo instance PR #167
 
Bug Fix:
 * adjust color and size of search hint PR #161

Changes in Tchap 0.1.7 (2018-05-04)
===================================================
 
Improvements:
 * matrix_sdk_version: 0.9.3 (5d401a1)
 * Change register/login sequence #112
 * Eliminate the preview step #113
 * Limitations on direct chat #114
 * Change room menu items #115
 * The rooms directories are not available for the E-users #125
 * Update room terminology #130
 * Change the room creation options #131
 * Contacts List: hide the non-tchap users #132
 * Contacts picker: the button "inviter des contacts" is renamed "inviter par email"
 * Remove the option "créer un salon" from the contacts picker #133
 * The user is not allowed to change his display name #134
 * Room directories: show the known federated directories #135
 * Start tchap on the room screen PR #144
 * Improve room summary PR #145
 
Bug Fix:
 * Can't acces room directory #82

Changes in Tchap 0.1.6 (2018-04-18)
===================================================
 
Improvement:
 * Update the tchap icons.
 * Update the MXID based on the email.
 
Bug Fix:
 * Change splash screen #120
 
Changes in Tchap 0.1.5 (2018-04-10)
===================================================
 
Improvements:
 * Open the existing direct chat on contact selection even if the contact has left it #103
 * Name a direct chat that has been left #103
 * Direct chat: invite again left member on new message #104
 * Conversations screen: re-enable favorites use (pinned rooms) #105
 * Search in the user directories is disabled for the users of the E-platform #108
 
Bug Fix:
 * Update IRC command handling (disable /nick and control /invite) #106

Changes in Tchap 0.1.4 (2018-04-06)
===================================================
 
Improvements:
 * Hide the current user from the Contacts list #95
 * Dinsic improve displayname (append the email domain) #99
 
Bug Fixes:
 * The email verification failed on device with background process limited #100
 * Reactivate register button when click to login button #97
 * Some contacts display a "null" display name #101

Changes in Tchap 0.1.3 (2018-04-04)
===================================================
 
Improvements:
 * Update matrix-sdk.aar lib (build 1762).
 * Factorization direct chat handling #77.
 * The MXID is based on the 3PID #89
 * Direct Chat Handling: Detect automatically the direct chats in which the user is invited by email #91
 * Restore the user directory section in the contacts when a search session is in progress #92.
 
Bug Fixes:
 * Crash sometime when try to access public rooms #86
 * Registration: Finalize correctly the account creation from email link #87
 * Contacts: duplicate items may appear after inviting a contacts by email #88
 * The contacts list is empty whereas the local contacts access is granted #90

Changes in Tchap 0.1.2 (2018-03-22)
===================================================
 
Improvement:
 * Update the known identity server names #76
 
Bug Fix:
 * Registration: the email field is changed on app resume #65

Changes in Tchap 0.1.1 (2018-03-16)
===================================================
 
Improvements:
 * Update matrix-sdk.aar lib (v0.9.1).
 * Update the tchap icons #30
 * Improve contact description #58
 * External bubble users are not allowed to create a room #47
 * Reorganise contacts and rooms panel contents
 * Complete email when no email #26
 * New Room creation banner #37
 * Hide "discussion directe" option #35
 * User Settings: remove email edition #41
 * Change the actions of the FAB (+) #36
 * Check the pending invites before creating new direct chat #44
 * Registration: Improve the servers selection #43
 
Bug Fixes:
 * Public room visibility #28
 * Correct badge count in contacts and rooms tab #56