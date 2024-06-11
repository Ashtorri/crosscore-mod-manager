package top.laoxin.modmanager.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.App
import top.laoxin.modmanager.R
import top.laoxin.modmanager.bean.GameInfo
import top.laoxin.modmanager.constant.GameInfoConstant
import top.laoxin.modmanager.data.UserPreferencesRepository
import top.laoxin.modmanager.data.backups.BackupRepository
import top.laoxin.modmanager.data.mods.ModRepository
import top.laoxin.modmanager.tools.ModTools
import top.laoxin.modmanager.tools.ToastUtils

class SettingViewModel(
    private val backupRepository: BackupRepository,
    private val modRepository: ModRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as App)
                SettingViewModel(
                    application.container.backupRepository,
                    application.container.modRepository,
                    application.userPreferencesRepository,
                )
            }
        }
        var gameInfoJob: Job? = null
    }

    private val _uiState = MutableStateFlow(SettingUiState())
    val uiState = _uiState.asStateFlow()
    var _gameInfo = GameInfoConstant.gameInfoList[0]

    init {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferencesRepository.getPreferenceFlow("SELECTED_GAME", 0).collect {
                _gameInfo = GameInfoConstant.gameInfoList[it]
            }

        }
        getGameInfo()
    }

    // 删除所有备份
    fun deleteAllBackups() {
        gameInfoJob?.cancel()
        setDeleteBackupDialog(false)
        gameInfoJob = viewModelScope.launch(Dispatchers.IO) {
            modRepository.getEnableMods(_gameInfo.packageName).collect {
                if (it.isNotEmpty()) {
                    // 如果有mod开启则提示
                    withContext(Dispatchers.Main) {
                        ToastUtils.longCall(R.string.toast_del_buckup_when_mod_enable)
                    }
                    this@launch.cancel()
                } else {
                    val delBackupFile: Boolean = ModTools.deleteBackupFiles(_gameInfo)
                    if (delBackupFile) {
                        backupRepository.deleteByGamePackageName(_gameInfo.packageName)
                        withContext(Dispatchers.Main) {
                            ToastUtils.longCall(App.get().getString(
                                R.string.toast_del_buckup_success,
                                _gameInfo.gameName,
                            ))
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            ToastUtils.longCall(
                                App.get().getString(
                                    R.string.toast_del_buckup_filed,
                                    _gameInfo.gameName,
                                )
                            )
                        }
                    }
                }
            }
            gameInfoJob?.cancel()
        }

    }

    // 设置删除备份对话框
    fun setDeleteBackupDialog(open: Boolean) {
        _uiState.value = _uiState.value.copy(deleteBackupDialog = open)
    }

    fun deleteCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val delCache = ModTools.deleteCache()
            if (delCache) {
                withContext(Dispatchers.Main) {
                    ToastUtils.longCall(R.string.toast_del_cache_success)
                }
            } else {
                withContext(Dispatchers.Main) {
                    ToastUtils.longCall(R.string.toast_del_cache_filed)
                }
            }
        }
    }

    fun openUrl(context: Context, url: String) {
        val urlIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(url)
        )
        context.startActivity(urlIntent)
    }

    fun deleteTemp() {
        viewModelScope.launch(Dispatchers.IO) {
            val delTemp: Boolean = ModTools.deleteTempFile()
            if (delTemp) {
                withContext(Dispatchers.Main) {
                    ToastUtils.longCall(R.string.toast_del_temp_success)
                }
            } else {
                withContext(Dispatchers.Main) {
                    ToastUtils.longCall(R.string.toast_del_temp_filed)
                }
            }
        }
    }

    fun showAcknowledgments(b: Boolean) {
        _uiState.value = _uiState.value.copy(showAcknowledgments = b)
    }

    fun showSwitchGame(b: Boolean) {
        _uiState.value = _uiState.value.copy(showSwitchGame = b)
    }

    // 设置游戏服务器
    fun setGameServer(serviceName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferencesRepository.savePreference("GAME_SERVICE", serviceName)
        }
    }

    // 设置安装位置
    fun setInstallPath(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferencesRepository.savePreference("INSTALL_PATH", path)
        }
    }

    // 设置gameInfoList
    fun setGameInfoList(gameInfoList: List<GameInfo>) {
        _uiState.value = _uiState.value.copy(gameInfoList = gameInfoList)
    }

    // 通过名称软件包名获取软件包信息
    private fun getGameInfo() {
        setGameInfoList(GameInfoConstant.gameInfoList)
    }

    //设置游戏信息
    fun setGameInfo(gameInfo: GameInfo) {
        try {
            App.get().packageManager.getPackageInfo(gameInfo.packageName, 0)
            viewModelScope.launch(Dispatchers.IO) {
                userPreferencesRepository.savePreference(
                    "SELECTED_GAME",
                    GameInfoConstant.gameInfoList.indexOf(gameInfo)
                )
                userPreferencesRepository.savePreference(
                    "SCAN_DIRECTORY_MODS",
                    false
                )
                withContext(Dispatchers.Main) {
                    ToastUtils.longCall(
                        App.get().getString(
                            R.string.toast_setect_game_success,
                            gameInfo.gameName,
                            gameInfo.serviceName
                        )
                    )
                    showSwitchGame(false)
                }

            }
        } catch (e: Exception) {
            ToastUtils.longCall(
                App.get().getString(
                    R.string.toast_set_game_info_failed,
                    gameInfo.gameName,
                    gameInfo.serviceName
                )
            )

            e.printStackTrace()
        }

    }

    fun flashGameConfig() {
        gameInfoJob?.cancel()
        gameInfoJob = viewModelScope.launch {
             userPreferencesRepository.getPreferenceFlow("SELECTED_DIRECTORY", ModTools.DOWNLOAD_MOD_PATH).collectLatest{
                 ModTools.readGameConfig(ModTools.ROOT_PATH + it)
                 ModTools.updateGameConfig()
                Log.d("设置测试","切换目录执行")
                 gameInfoJob?.cancel()
            }

        }
    }
}