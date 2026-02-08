package com.git.log.common.tool.inter

import java.awt.Desktop
import java.net.URI

actual fun openUri(uri: String) {
    Desktop.getDesktop().browse(URI.create(uri))
}