package top.laoxin.modmanager.bean

import android.os.Parcel
import android.os.Parcelable

data class GameInfo(
    val gameName: String,
    val serviceName: String,
    val packageName: String,
    val gamePath: String,
    val modSavePath: String = "",
    val antiHarmonyFile : String = "",
    val antiHarmonyContent : String = "",
    val gameFilePath : List<String>,
    val version : String,
    val modType : List<String>,
    val isGameFileRepeat : Boolean = true

) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.createStringArrayList()!!,
        parcel.readString()!!,
        parcel.createStringArrayList()!!,
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(gameName)
        parcel.writeString(serviceName)
        parcel.writeString(packageName)
        parcel.writeString(gamePath)
        parcel.writeString(modSavePath)
        parcel.writeString(antiHarmonyFile)
        parcel.writeString(antiHarmonyContent)
        parcel.writeStringList(gameFilePath)
        parcel.writeString(version)
        parcel.writeStringList(modType)
        parcel.writeByte(if (isGameFileRepeat) 1 else 0)

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<GameInfo> {
        override fun createFromParcel(parcel: Parcel): GameInfo {
            return GameInfo(parcel)
        }

        override fun newArray(size: Int): Array<GameInfo?> {
            return arrayOfNulls(size)
        }
    }
}