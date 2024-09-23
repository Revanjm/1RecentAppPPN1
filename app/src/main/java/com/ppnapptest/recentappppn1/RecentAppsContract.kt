package com.ppnapptest.recentappppn1

import android.provider.BaseColumns

object RecentAppsContract {

    object RecentAppsEntry : BaseColumns {
        const val TABLE_NAME = "recent_apps"
        const val COLUMN_NAME_PACKAGE_NAME = "package_name"
    }
}