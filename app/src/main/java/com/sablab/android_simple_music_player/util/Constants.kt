package com.sablab.android_simple_music_player.util

class Constants {

    companion object {

        // Intent Constants
        const val MUSIC_DATA = "musicData"
        const val COMMAND_DATA: String = "ServiceCommand"
        const val ACTION_PLAYER = "com.sablab.android_simple_music_player.util.ACTION_PLAYER"

        // Notification Constants
        const val channelID = "music_player_notification_channel_id"
        const val foregroundServiceNotificationTitle = "Music Player"
        const val foregroundIntentServiceNotificationTitle = "My Foreground Intent Service"
        const val notificationChannelName = "Music Player Service Channel"

        // Job scheduler Constants
        const val jobId = 123

        val allMusics = mutableSetOf<String>()
    }

}