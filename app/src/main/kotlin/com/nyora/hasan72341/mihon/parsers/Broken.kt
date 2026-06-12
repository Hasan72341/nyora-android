package com.nyora.hasan72341.mihon.parsers

/**
 * Annotate [ContentParser] implementation to mark this parser as broken instead of removing it
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Broken(

	/**
	 * Reason why this parser is broken
	 */
	val message: String = "",
)
