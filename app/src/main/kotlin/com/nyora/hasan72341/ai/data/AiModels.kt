package com.nyora.hasan72341.ai.data

import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
	val model: String,
	val messages: List<ChatMessage>,
	val temperature: Float = 0.3f,
	val response_format: ResponseFormat? = null
)

@Serializable
data class ResponseFormat(val type: String)

@Serializable
data class ChatMessage(
	val role: String,
	val content: String
)

@Serializable
data class ChatResponse(
	val choices: List<ChatChoice>
)

@Serializable
data class ChatChoice(
	val message: ChatMessage
)

// --- Chapter-wise Structured Data ---

@Serializable
data class ChapterTranslationRequest(
	val context: String = "Manga Chapter Translation",
	val source_lang: String,
	val target_lang: String,
	val dialogues: List<DialogueBlock>
)

@Serializable
data class DialogueBlock(
	val id: String, // format: "pageIndex_bubbleIndex"
	val original: String,
	val mt_draft: String
)

@Serializable
data class ChapterTranslationResponse(
	val reasoning: String? = null,
	val translations: List<TranslatedDialogue>
)

@Serializable
data class TranslatedDialogue(
	val id: String,
	val translated: String
)
