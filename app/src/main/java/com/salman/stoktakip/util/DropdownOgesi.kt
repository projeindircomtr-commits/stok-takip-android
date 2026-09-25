package com.salman.stoktakip.util

import java.io.Serializable

data class DropdownOgesi(val id: Int, val ad: String) : Serializable {
    override fun toString(): String = ad
}
