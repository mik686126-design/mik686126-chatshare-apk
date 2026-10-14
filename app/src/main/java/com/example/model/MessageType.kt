package com.example.model

enum class MessageType {
    TEXT,
    IMAGE,
    DOCUMENT
}

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ
}

enum class DocumentType(val extension: String, val label: String) {
    PDF("pdf", "PDF"),
    DOC("docx", "DOC"),
    SHEET("xlsx", "XLS"),
    SLIDE("pptx", "PPT"),
    ZIP("zip", "ZIP"),
    IMAGE("img", "IMAGE"),
    TXT("txt", "TXT"),
    OTHER("file", "FILE");

    companion object {
        fun fromExtension(ext: String?): DocumentType {
            return when (ext?.lowercase()) {
                "pdf" -> PDF
                "doc", "docx" -> DOC
                "xls", "xlsx", "csv" -> SHEET
                "ppt", "pptx" -> SLIDE
                "zip", "rar", "7z", "tar", "gz" -> ZIP
                "jpg", "jpeg", "png", "webp", "gif" -> IMAGE
                "txt", "md", "json" -> TXT
                else -> OTHER
            }
        }
    }
}
