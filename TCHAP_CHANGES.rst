Changes in Tchap 1.2.3 (2021-11-19)
===================================================

Bug Fix:
 * The connexion to the server fails on Network error on Android 5.0 and 5.1 (Lollipop) devices.
 
Changes in Tchap 1.2.2 (2021-08-27)
===================================================

Features/Improvements:
 * Make certificates checking mode more flexible 
 
Changes in Tchap 1.2.1 (2021-04-02)
===================================================

Bug Fix:
 * Some last message are missing in the rooms list (PR #707)

Changes in Tchap 1.2.0 (2021-03-24)
===================================================

Improvements:
 * Update matrix-sdk.aar libs - Revision: 68165b6ddbbea51524b5c7e6192d4ae4a0083ad3 [68165b6d]
 * [FDroid] Prompt the user about the Battery optim if this is not already done #646
 * Public room creation: improve UI for agent.agent users #698
 * [Room retention] Support unlimited room history (PR #700)
 * Update the version used to trigger a clear cache during the application update (PR #700)
 * Room members: gray out the expired users #556

Bug Fixes:
 * Crash: java.lang.NullPointerException: at org.matrix.androidsdk.data.RoomState.getMembersAsync #680
 * Crash: isTaskCorrupted failed on java.lang.IllegalArgumentException #694

Changes in Tchap 1.1.1 (2020-12-18)
===================================================

Improvements:
 * Enable the room access by link

Bug Fixes:
 * Server notice rooms (Tchap info) are hidden in the rooms list #692

Changes in Tchap 1.1.0 (2020-11-22)
===================================================

Improvement:
 * Update the version used to trigger a clear cache during the application update (PR #676)

Changes in Tchap 1.0.49 (2020-11-18)
===================================================

Improvements:
 * Update matrix-sdk.aar libs - Revision: 184214f2eeed351801b18052f39e22fbb6ed3b3a [184214f2e]
 * [Notifications] Hide the unsupported "mention only" option for the encrypted rooms #643
 * [Notifications] Add the full display name of the sender when the user click on them #629
 * Force a cache clearing to force an update of the stored RoomAccountData
 * The room access by link is enabled only on Btchap for the moment

Bug Fixes:
 * Crash: on new favorite message #670
 * Crash: java.lang.NullPointerException at org.matrix.androidsdk.data.RoomAccountData.handleEvent #671
 * Attachments: an empty bubble is displayed for the infected attachments #637
 * Multiple activity instances #665
 * Crash: Activity:im.vector.activity.VectorHomeActivity, Exception: java.lang.NullPointerException #668
 * Crash: java.lang.StackOverflowError (PushManager.registerToThirdPartyServerRecursive) #672
 * Notifications fail because BingRulesManager is not initialized #677
 
Changes in Tchap 1.0.48 (2020-10-28)
===================================================

Improvements:
 * Increase targetSdk to 29 #609
 * Private rooms: turn on the option to join on room’s link #573
 * Room preview: support the preview on shared room link #612
 * Apply the new room creation design #604
 * Apply the new design on the room avatar and name #648
 * [Room creation] Do not override the power levels anymore #632
 * [Room access] Improve the wordings related to the room link access #645
 * Rename "Salon public" with "Salon forum" #649
 * [Room alias] Harden the room aliases #644

Bug Fixes:
 * Tchap crashes on invalid access token #631

Changes in Tchap 1.0.47_2 (2020-11-16)
===================================================

Bug Fix:
 * [Android 10] permission denied to open a pdf #683

Changes in Tchap 1.0.47_1 (2020-11-11)
===================================================

Improvement:
 * Increase targetSdk to 29 #609
 * Improve the prominent disclosure, user consent and privacy policy which discloses
 the access, collection, use or sharing of users' contact list information.

Changes in Tchap 1.0.47 (2020-10-02)
===================================================

Bug Fixes:
 * Configure correctly activity launchmode PR #630
 * Fix searchview cursor color PR #633
 * fix crash loop back off on invalid access token #636
 * [Expired Account] Tchap is stuck on the splash screen whereas the account is renewed #639
 
Changes in Tchap 1.0.46 (2020-09-26)
===================================================

Improvements:
 * Update matrix-sdk.aar libs - Revision: 3d3c42c5a55bcc4e80d268a6eace93eb1dfc0247 [3d3c42c5a]
   Note: targetSdkVersion has been updated in matrix-sdk to Android 10 (API 29).
 * Simplify the notifications handling #605

Bug Fixes:
 * LoginActivity: block the UI when the loading wheel is running
 * java.lang.NullPointerException (RoomViewHolder.java:197) #611
 * matrix-sdk: Tchap crashes when it checks user presence #603
 * matrix-sdk: Fix a crash on the crypto code (potentially a race condition)

Changes in Tchap 1.0.45 (2020-08-14)
===================================================

Improvements:
 * Enable the favorite messages #571
 * Turn on the permalink option #555
 * Update matrix-sdk.aar libs - Revision: 31d8a991d1b73cdf62db3b81f8b05d4566c3e879 [31d8a991d]
 * Room creation: remove special character from the room link (#599)
 * Improve email validation catch up (PR #602)

Bug Fix:
 * Fix java.lang.NullPointerException at org.matrix.androidsdk.data.RoomAccountData.handleEvent

Changes in Tchap 1.0.44 (2020-06-25)
===================================================

Bug Fix:
 * The notification sound is missing for the first one after backgrounding the app (PR #598)

Changes in Tchap 1.0.43 (2020-06-25)
===================================================
Improvement:
 * Update matrix-sdk.aar libs - Revision: c4509211c2e28b5ae4f2b052c8c9f2bb049fed85 [c4509211c]

Bug Fixes:
 * The sound is missing on some notifications (PR #597)
 * Killed Tchap misses some notifications

Changes in Tchap 1.0.42 (2020-06-16)
===================================================

Bug Fixes:
  * Some messages are stuck on decryption failure whereas the keys have been shared #594
  * Quick reply: Fix issue with Toast display (PR #593)

Changes in Tchap 1.0.41 (2020-06-12)
===================================================

Improvement:
 * Improve the notifications display on devices running API level < 24 (PR #591)

Bug Fixes:
  * Quick Reply: Handle correctly the failure on unknown devices (PR #592)
  * Rooms list: The room names with [ ] are not displayed correctly
 
Changes in Tchap 1.0.40 (2020-06-11)
===================================================

Improvement:
 * Fdroid: force the realtime sync mode by default #590

Changes in Tchap 1.0.39 (2020-06-08)
===================================================

Improvements:
 * Device verification: update wordings and remove IP address displayed in the incoming demand.
 * Room creation - The public rooms are not federated by default (They are restricted to the user's Homeserver).

Changes in Tchap 1.0.38 (2020-06-04)
===================================================

Improvements:
 * Update matrix-sdk.aar libs - Revision: 5fc3aa71a7f4adeeb38c3c5915819cb6c5bd42e9 [5fc3aa71a]
 * Room history: apply the design with bubbles #572
 * Rebase onto vector-im/riot-android (PR #589):
    - Get all changes from Riot 0.8.21 to Riot 0.9.8
    - Fix Crash / potential NPE after logout (#3367)
 * Disable key backup in the rebase version of the code #583
 * Enable the device verification based on emojis string
 * Updage gradle version
 * Update the messages displayed during the request of a token by email #577

Bug Fixes:
 * [Device verification] Only half of the key sharing requests are handled after verification #587
 * Change history_visibility when a room is removed from the rooms directory #558

Changes in Tchap 1.0.37 (2020-03-05)
===================================================

 * Revert - Room members: gray out the expired users #556
 
Changes in Tchap 1.0.36 (2020-02-05)
===================================================

Improvement:
 * Order the room members by considering admin(s) first #568
 * Room members: gray out the expired users #556
 
Changes in Tchap 1.0.35 (2020-01-16)
===================================================

Improvement:
 * Improve the warning dialog displayed before creating an external account
 * Update the known instances list #567

Changes in Tchap 1.0.34 (2019-12-17)
===================================================

Improvement:
 * Configure per-room retention period for messages #524 - Enabled only on Pre-prod.
 * Update wording on limit exceeded error #557
 * Adjust F-Droid parameters #565
 * Clean the Tchap-secure application #564
 
Bug Fix:
 * App crashes during `computeDisplayNameFromUserId` #560

Changes in Tchap 1.0.33 (2019-11-25)
===================================================

Improvement:
 * Manage a minimum client version #493

Changes in Tchap 1.0.32 (2019-11-01)
===================================================

Improvements:
 * Update the pinned certificates list
 * Limit Tchap to Android 5.0 Lollipop (API 21) and higher #549
 
Bug Fixes:
 * [Account Validity] Handle the renewal url in the Tchap application #546
 * Update stored connection configuration before using them PR #551

Changes in Tchap 1.0.31 (2019-09-26)
===================================================

Only one change:
 * Force a cache clearing to handle correctly the recent changes.

Changes in Tchap 1.0.30 (2019-09-19)
===================================================

Improvements:
 * Update matrix-sdk.aar libs - Revision:b525955b38fe359717d856679c270d1b824f7b5e [b525955b]
 * Handle the strong password policy forced by the server #465
 * Room creation: allow or not the external users to join the room #474
 * Add a marker to indicate whether or not a room can be joined by external users #475
 * The room admin is able to open the room to the external users #476
 * Room members: invite new members by their email address #483
 * Room members: remove the external users from the picker when they are not allowed to join #484
 * Discussion creation: Add the room access rule value: ".direct" #491
 * Allow the user to send a new invite to an external email address #499
 * Remove the URL preview option from the user's settings #527
 * Room Members: Allow to revoke 3pid invites PR #530
 * Pin the new agent.externe certificate
 * Add room access info in the Room title #536
 * Prompt the user before creating an external account #525

Bug Fixes:
 * Do not use by default a member avatar for the room avatar #528
 * The external users are not able to send messages in an encrypted room #539
 * the displayname of an external is sometime incorrect #511

Changes in Tchap 1.0.29 (2019-09-01)
===================================================

Improvements:
 * Force the email address in lower case #514
 * Prod: pin the certificat of the external instance
 * Fix notification problem when the WAKE_LOCK permission is not granted PR #390

Bug Fix:
 * Room member completion: Do not display the matrix identifier #357

Changes in Tchap 1.0.28 (2019-08-07)
===================================================

Improvements:
 * Prompt the last room admin before letting him leave the room #496
 * Update matrix-sdk.aar libs - Revision:3b808f63d44bbf9d68a204b56cc607c34b47d964 [3b808f63] (v0.9.26)
 * Include 64bits version of the libraries in the APK PR #506

Bug Fix:
 * Improve accessibility service notifications #448

Changes in Tchap 1.0.27 (2019-07-05)
===================================================

Improvements:
 * Prompt external users before displaying their email in user directory #482
 * Update matrix-sdk.aar libs - Revision:3e3cd0a4ad6c36cff3bd925b916a9c611a656853 [3e3cd0a4] PR #490

Bug Fix:
 * Bug Fix - Blink of the expired account dialog #487
 * Rooms list: Fix the rooms order when the user decided to ignore join/leave events #485

Changes in Tchap 1.0.26 (2019-06-18)
===================================================

Improvements:
 * Enable the proxy lookup use on Prod
 * The external users can now be hidden from the users directory search, show the option in settings #477
 * Support the account validity error #461

Bug Fix:
 * Discussion left by the other member are named "Salon vide" #451

Changes in Tchap 1.0.25 (2019-05-23)
===================================================

Improvements:
 * Push notifications: remove the option "confidentialité réduite" #466
 * Retrait du firebase analytics #468
 * Support proxy lookup #471
 * Update matrix-sdk.aar libs - Revision: 21569865d463481ac656b6eb06f62494ff9f6412 [21569865] PR #472

Changes in Tchap 1.0.24 (2019-05-03)
===================================================

Improvements:
 * Configure the application for the external users.
 * on F-Droid version, the default sync delay is increased to 1 minute.
 
Bug Fixes:
 * Registration - Infinite loading wheel on unauthorized email #459

Changes in Tchap 1.0.23 (2019-04-23)
===================================================

Improvements:
 * Increase the minimum password length to 8 #463
 * Settings: Remove the phone number option #462
 * Update matrix-sdk.aar lib - Revision: bdae4c5d479a5992b8d4ec70cfb80a475a92143f
 
Bug Fixes:
 * Security fix: remove obsolete and buggy ContentProvider which could allow a malicious local app to compromise account data. Many thanks to Julien Thomas (twitter.com/@julien_thomas) from Protektoid Project (https://protektoid.com) for identifying this and responsibly disclosing it.
 * zoom-out on image causes crash #441

Changes in Tchap 1.0.22 (2019-03-22)
===================================================

Improvements:
 * Warn the user about the remote logout in case of a password change #439
 
Bug Fixes:
 * The app icon badges is buggy #440
 * Bug report sending fails on a certificate error.

Changes in Tchap 1.0.21 (2019-03-15)
===================================================

Improvements:
 * Do not allow system certificates in apk built with pinning PR #453
 * Block invite to a deactivated account user #444
 
Bug Fixes:
 * FCM service is not working #449
 * Parameters: infinite loading wheel on avatar update #454
 * Problem with deactivated/reactivated accounts #438

Changes in Tchap 1.0.20 (2019-03-12)
===================================================

Improvements:
 * Trust the user CAs in apk built without pinning #445
 * Update TAC url #442

Changes in Tchap 1.0.19 (2019-02-22)
===================================================

Improvements:
 * Enable Certificate pinning for the "agent" target #367
 * Private Room creation: change history visibility to "invited" #425
 * Power level: a room member must be moderator to invite #426
 * Keys sharing: remove the verification option #422
 * Settings: hide membership events by default #423
 * Adjust wording on bug report #432
 * Fix an unexpected warning when the Camera permission is requested #436
 
Bug Fixes:
 * Antivirus scan: outgoing attachments are considered infected by mistake #433
 * Two discussions is created when the user presses "enter" on an external keyboard #435
 * Failed to send a video captured by the native camera.

Changes in Tchap 1.0.18 (2019-02-06)
===================================================

Improvement:
 * Registration: remove the polling mechanism on email validation #417

Changes in Tchap 1.0.17 (2019-01-25)
===================================================

Improvements:
 * Adjust some points on Android project configuration PR #404
 * Display the padlock in dark red color for the protected target #414
 
Bug Fix:
 * The Terms And Conditions are not available anymore (PR #412).

Changes in Tchap 1.0.15 (2019-01-11)
===================================================

Improvements:
 * Configure Android project to build the different application version #396
 * Enable bug report, and rage shake #394
 * Improve registration process #401
 
Bug Fixes:
 * Tchap auto joined a public room which allows the preview #403
 * Room creation: the actions on the public option toggle are ignored #397

Changes in Tchap 1.0.14 (2018-12-17)
===================================================

Improvements:
 * Update Tchap logo for the protected infra.
 
Bug Fix:
 * Public rooms list: a wrong domain is displayed.

Changes in Tchap 1.0.13 (2018-12-12)
===================================================

Bug Fix:
 * Public room creation: wrong domain is displayed #395

Changes in Tchap 1.0.12 (2018-11-29)
===================================================

Improvements:
 * Update matrix-sdk.aar lib - build 1953 - Revision:e07635053dede93f2f23f586310b012a0a59b6b1

Bug Fixes:
 * Remove the warning on unknown devices when a call is placed #393
 * I'm not allow to send message in a new joined room #392
 * Rooms members: members who left are listed with the actual members #391
 * Matrix Content Scanner: Update the stored server public key (riot-android PR 400)

Changes in Tchap 1.0.11 (2018-11-22)
===================================================

Improvements:
 * Update matrix-sdk.aar lib - build 1950 - Revision:b39da507f6f61e617c8164b2adcbf013ec0f3135
 * User Profile: add an option to hide the user from users directory search (#385)
 * Certificate pinning (#367):
    - A new flavor dimension has been added "pinning"
    - A configuration file has been added to handle potential fingerprints
    - The user is prevented from accepting unknown certificates
 * Replace "chat.xxx.gouv.fr" url with ""matrix.xxx.gouv.fr" (#384)
 * Room history: Apply the right tint on padlock in encrypted room.
 * Rebase from vector-im/riot-android:
    Features:
     - Enable Lazy Loading by default, if the hs supports it
     - Add RTL support (2376, 2271)
	 
	Improvements:
     - Remove double negations from settings and update descriptions (2723)
     - Handle missing or bad parameter in slash command
     - Support specifying kick and ban message (2164)
     - Add image transparency and fix issues with gifs in the media viewer (2731)
     - Ability to crop profile picture before setting (2598)
     - Add a setting of the room's info area visibility.
	 
   Other changes:
     - Locales management has been moved to a dedicated file

	Bugfix:
     - Improve `/markdown` command (2673)
     - Fix Permalinks and registration issue (2689)
     - Mention from read receipts list doesn't work (656)
     - Fix issue when scrolling file list in room details (2702)
     - Align switch camera button to parent in landscape mode (2704)
 
Bug Fixes:
 * Registration: Tchap launch fails when the user clicks on the email link (#386)

Changes in Tchap 1.0.10 (2018-10-30)
===================================================

Improvements:
 * Update matrix-sdk.aar lib - build 1932 - Revision:7050323fa65ed32a301c3cd4fc25dbee60636c00
 * Show the keyboard by default on event selection.
 * Rebase from vector-im/riot-android:
	Improvements:
	 - Improve certificate pinning management (PR matrix-android-sdk 375)
	 - Use LocalBroadcastManager when applicable (2595)
	 - Tapping on profile picture in sidebar opens settings page (2597)

	Bugfix:
	 - When exporting E2E keys, it isn't clear that you are creating a new password (2626)
	 - Reply get's lost when moving app in background and back (2581)
	 - Android 8: crash on device Boot (2615)
	 - Avoid creation of Gson object (2608)
	 - Inline code breaks in reply messages (2531)
 
Bug Fixes:
 * Notifications are disabled on Fdroid after application update #381
 * Modify transparent logo and update sources to use new file #380

Other:
 * Disable local file encryption until "Unexpected error on app resume:..."(#383) is fixed.
 
Changes in Tchap 1.0.9 (2018-10-02)
===================================================

Improvements:
 * Update matrix-sdk.aar lib - build 1905 - Revision: 941bfe7f7586dc101a39ac9588be0b9b1e2a35dc
 
Bug Fix:
 * Add retro-compatibility for Android < 20 to be able to negociate a TLS session.

Changes in Tchap 1.0.8 (2018-09-28)
===================================================

Improvements:
 * Configure accepted TLS cipher suites #369
 * Protect local data in Tchap #366
 * Forbid screenshots of sensitive content #206
 * Miscellaneous hardening features #242
 * Public rooms: configure the federation #129
 * Improve room creation UI: the room creation is allowed even if no member is selected #377
 * A public room may become private #368
 * Improve "reply to" option #371
 * Update matrix-sdk.aar lib - build 1903 - Revision: b4bfc0750d43ec8a7a1ea1814cc626e1c46f7e0d
 * Rebase from vector-im/riot-android:
	 Improvements:
	  - Minor changes to toolbar style and other UI elements (2529)
	  - Improvements to dialogs, video messages, and the previewer activity (2583)
	  - Improve intent to open document (2544)
	  - Avoid useless dialog for permission (2331)
	  - Improve wording when exporting keys (2289)
	  - Upgrade lib libphonenumber from v8.0.1 to 8.9.12
	  - Upgrade Google firebase libs
 
	 Bugfix:
	 - Fix crash when opening file with external application (2573)
	 - Fix issue on settings: unable to rename current device if it has no name (2174)
	 - Allow anyone to add local alias and to try to delete local alias (1033)
	 - Fix issue on "Resend all" action (2569)
	 - Fix messages vanishing when resending them (2508)
	 - Remove delay for / completion (2576)
	 - Handle `\/` at the beginning of a message to send a message starting with `/` (658)
	 - Escape nicknames starting with a forward slash `/` in mentions (2146)
	 - Improve management of Push feature
	 - MatrixError mResourceLimitExceededError is now managed in MxDataHandler (vector-im/riot-android#2547 point 2)
 
Bug Fixes:
 * Bad wording on "+" Menu #370
 * Room Settings: the matrix id is displayed for the banned users PR #376
 * Public Rooms: Disable the pagination, display all available rooms

Changes in Tchap 1.0.7 (2018-09-04)
===================================================

Improvements:
 * Update matrix-sdk.aar lib - build 1875 - Revision: ccf12449b8f09b06a7a8f501b9d7a382270b2305
 * Rebase from vector-im/riot-android #364
 
Bug Fixes:
 * Public Rooms: the loading wheel is missing #362
 * A discussion is considered as a salon after I left and joined it again #356

Changes in Tchap 1.0.6 (2018-08-07)
===================================================

Bug Fixes:
 * On joining public room for the first time: terms and conditions error #347
 * The app crashes when a user deletes his account #351
 * After a user is excluded from a room, he can still view it in the list of conversation #349
 * DinsicUtils: the method `isFromFrenchGov()` is not relevant anymore #149
 * Settings - Keys export/import dialog: the button label is not readable #358

Changes in Tchap 1.0.5 (2018-07-13)
===================================================

Improvement:
 * Fix some problems found by FindBugs PR #248
 * Updating the margin of the room sending message layout PR #343
 * Removing the option to create a room shortcut on the phone's home screen PR #344

Bug Fixes:
 * Multiple invites sent when I try to start a conversation #345
 * Search in invite contacts screen : do not display user directory section #195

Changes in Tchap 1.0.4 (2018-07-11)
===================================================

Improvement:
 * Disable temporarily the notification listener check PR #339

Changes in Tchap 1.0.3 (2018-07-10)
===================================================

Improvements:
 * Update matrix-sdk.aar lib - build 1835 - Revision: d9644895fdc5ad3af563fbadc8f0f82ae6e0f919
 * Update OLM lib to version 2.3.0.
 * Antivirus: Encrypt AES keys sent to the antivirus server #122
 * Hide the "inviter des contacts dans Tchap" button #285
 * Public rooms: turn on the preview by default #262
 * Encrypt event content for invited members #322
 * Add support for the scanning and downloading of unencrypted thumbnails #278
 * Rewritten camera + pick files to use standard Android API (thx to @af-anssi) PR #212
 * Detect accessibility service (thx to @af-anssi) PR #209
 * Room history: update the design of the text input #267
 * Room history: remove the display of the state events (history access, encryption) #266
 * Authentication screen: waiting screen when sign in #300
 * Authentication screen: restore the forgot password option #216
 * Authentication screen: change discover strategy #299
 * Authentication screen: wording when sign in #298
 * FDroid: change pull parameters #301
 * Room invite: apply the hexagon shape on avatar for a "salon" #283
 * Remove the option "Effacer" on the membership event in the room history #310
 * delete piwic link #291
 * Display the discussions invites in the Conversations tab #288
 * Home screen: dismiss the potential search session when a new activity is started #265
 * Room creation: highlight the caution for public rooms #215
 * Update the search bar display #271
 * Code cleaning: Remove useless code in the login activity PR #329
 * Updates icons for medias and VoIP #332
 * Hide the "inviter des contacts dans Tchap" button #285

Bug Fixes:
 * My first public "salon" is displayed like a discussion #284
 * Some discussions are displayed like a salon (and conversely) in the rooms search result #279
 * Creation of an infinity of rooms #305
 * The display name of some users is missing #309
 * Put the section header title in lower case #328
 * Handle correctly unauthorized email during registration PR #308
 * Handle correctly unreachable contacts PR #280
 * Nouveau salon: media permissions (permission.CAMERA,...) are not checked correctly #282
 * Unable to give my consent when I reject a room invite #281
 * Disable Rageshake detection #293
 * Fix crash with media (images/videos) on Android 4.x #333
 * An unread badge is displayed on Contact tab by mistake PR #337

Changes in Tchap 1.0.2 (2018-06-29)
===================================================

Improvements:
 * Change the application id with "fr.gouv.tchap".
 * Update matrix-sdk.aar lib - build 1820 - Revision: 85a7423c23cbf82e1f447f81dc1ff4661884438d
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