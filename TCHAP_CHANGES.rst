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