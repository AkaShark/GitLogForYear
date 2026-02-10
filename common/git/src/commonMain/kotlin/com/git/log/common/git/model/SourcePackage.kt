package com.git.log.common.git.model

data class SourcePackage(
    val tempDir: String,
    val representedObjects: List<SourceRegistrationData>
) {
    companion object {
        fun create(sources: List<SourceRegistrationData>, tempDir: String): SourcePackage {
            return SourcePackage(tempDir, sources)
        }
    }
}