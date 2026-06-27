package com.portfello.data.db.converter

import androidx.room.TypeConverter
import com.portfello.data.db.entity.AssetType

class Converters {
    @TypeConverter
    fun fromAssetType(value: AssetType): String = value.name

    @TypeConverter
    fun toAssetType(value: String): AssetType = AssetType.valueOf(value)
}
