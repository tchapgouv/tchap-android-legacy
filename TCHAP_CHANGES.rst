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