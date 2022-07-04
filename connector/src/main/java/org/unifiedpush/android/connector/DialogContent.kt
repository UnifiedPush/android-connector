package org.unifiedpush.android.connector

data class DialogContent(
    val NoDistribTitle: String = "No distributor found",
    val NoDistribDialogMessage: String = "You need to install a distributor " +
            "for push notifications to work.\n" +
            "More information here:\n" +
            "https://unifiedpush.org/",
    val NoDistribOKButton: String = "OK",
    val NoDistribIgnoreButton: String = "Ignore",
    val ChooseTitle: String = "Choose a distributor"
)