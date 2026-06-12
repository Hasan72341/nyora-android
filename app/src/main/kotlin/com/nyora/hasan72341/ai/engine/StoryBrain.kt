package com.nyora.hasan72341.ai.engine

import com.nyora.hasan72341.ai.data.ChatMessage
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class CharacterProfile(
	val originalName: String,
	val targetName: String,
	val gender: String,
	val tone: String // e.g. "Polite", "Rude", "Old-fashioned"
)

@Serializable
data class ChapterNarrative(
	val summary: String,
	val characters: List<CharacterProfile>
)

/**
 * Google-level Narrative Engine: Extracts and maintains character consistency across images.
 */
@Singleton
class StoryBrain @Inject constructor() {
	private var currentNarrative: ChapterNarrative? = null

	fun getNarrativeContext(): String {
		val narrative = currentNarrative ?: return "Initial pages of the chapter."
		return """
			Chapter Context: ${narrative.summary}
			Characters Identified:
			${narrative.characters.joinToString("\n") { "- ${it.originalName} (${it.targetName}): ${it.gender}, ${it.tone} style" }}
		""".trimIndent()
	}

	fun updateNarrative(newNarrative: ChapterNarrative) {
		// In a production app, we would merge profiles intelligently
		currentNarrative = newNarrative
	}

	fun clear() {
		currentNarrative = null
	}
}
