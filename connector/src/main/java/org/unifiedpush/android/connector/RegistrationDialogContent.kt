package org.unifiedpush.android.connector

data class RegistrationDialogContent(
    val noDistributorDialog: NoDistributorDialog = NoDistributorDialog(),
    val chooseDialog: ChooseDialog = ChooseDialog()
)

data class NoDistributorDialog(
    var title: String = "No distributor found",
    var message: String = "You need to install a distributor " +
            "for push notifications to work.\n" +
            "For more information, visit\n" +
            "https://unifiedpush.org/",
    var okButton: String = "OK",
    var ignoreButton: String = "Ignore",
)

data class ChooseDialog(
    var title: String = "Choose a distributor"
)