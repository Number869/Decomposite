package com.nxoim.decomposite.core.ios.navigation

import com.arkivanov.essenty.statekeeper.SerializableContainer
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.serialization.json.Json
import platform.Foundation.NSCoder
import platform.Foundation.NSString
import platform.Foundation.decodeTopLevelObjectOfClass
import platform.Foundation.encodeObject

@Suppress("unused") // Used in Swift
fun save(coder: NSCoder, state: SerializableContainer) {
    coder.encodeObject(
        `object` = json.encodeToString(SerializableContainer.serializer(), state),
        forKey = "state"
    )
}

@Suppress("unused") // Used in Swift
@OptIn(
    ExperimentalForeignApi::class,
    BetaInteropApi::class
)
fun restore(coder: NSCoder): SerializableContainer? {
    val string = coder.decodeTopLevelObjectOfClass(
        aClass = NSString,
        forKey = "state",
        error = null
    ) as String?

    return string
        ?.runCatching {
            json.decodeFromString(
                SerializableContainer.serializer(),
                string
            )
        }
        ?.getOrNull()
}


private val json = Json {
    allowStructuredMapKeys = true
}