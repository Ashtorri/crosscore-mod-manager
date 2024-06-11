package top.laoxin.modmanager.ui.viewmodel

data class UserPreferencesState (
    var scanQQDirectory: Boolean = false,
    var selectedDirectory: String = "未选择",
    val scanDownload: Boolean = false,
    val installPath : String = "",
    val gameService : String = "",
    val selectedGameIndex : Int = 0,
    val scanDirectoryMods : Boolean = false,
)