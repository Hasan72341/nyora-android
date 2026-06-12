package com.nyora.hasan72341.mihon.parsers

/**
 * Opt-in marker for the app's native content-parser engine internal API.
 * Usages must be annotated with @OptIn(InternalParsersApi::class) or @InternalParsersApi.
 */
@RequiresOptIn(
	level = RequiresOptIn.Level.ERROR,
	message = "This is an internal parsers API and may change at any time.",
)
@Retention(AnnotationRetention.BINARY)
@Target(
	AnnotationTarget.CLASS,
	AnnotationTarget.FUNCTION,
	AnnotationTarget.PROPERTY,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.CONSTRUCTOR,
	AnnotationTarget.TYPEALIAS,
)
annotation class InternalParsersApi
