package com.mdm.accounting.batch.marketplace.annualtaxreturn.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.encoder.org.apache.commons.lang3.RegExUtils;

/**
 * @author dejardin-m
 *
 */
@Slf4j
public final class EcollabFieldSanitizer {
	abstract class FieldSanitizer {

		/**
		 * Regex for unprintable char
		 */
		private static final String UNPRINTABLES = "\\p{C}";
		private static final String disallowedCharsPatternForSellerAndAdress = "[!#$%&()*+,\\/:;<=>?\\[\\\\\\\\]@^_`{|}~¡¢£¤¥¦§¨©ª¬®¯°±²³´µ¶·¸¹º¼½¾¿ÆÐ×Ø×ØÞßæð÷øþ]";
		private final Pattern replacementPattern;

		/**
		 * @param disallowedChars : Arrays of char that must be replaced
		 */
		FieldSanitizer(final String disallowedCharsPattern) {
			this.replacementPattern = Pattern.compile(disallowedCharsPattern);
		}

		/**
		 * @param value
		 * @return
		 */
		private String replaceByRegex(String value) {
			return this.replacementPattern.matcher(value).replaceAll(StringUtils.EMPTY);
		}

		private String sanitize(String value) {
			if (StringUtils.isBlank(value)) {
				return StringUtils.EMPTY;
			}

			// Order can have can change the returned value

			value = StringUtils.trim(value);

			value = RegExUtils.replaceAll(value, FieldSanitizer.UNPRINTABLES,
					StringUtils.EMPTY);

			value = specificSanitize(value);
			value = replaceByRegex(value);
			// value = replaceCharactersBySpace(value, this.disallowedChars);

			value = StringUtils.stripAccents(value);

			// Keep at end after all replacement
			return StringUtils.normalizeSpace(value);

		}

		/**
		 * Additional actions
		 * @param value to be sanitize
		 * @return sanitized value
		 */
		protected abstract String specificSanitize(String value);
	}

	class FieldSanitizerAddress extends FieldSanitizer {

		private final char[] specificPositionnedChars = { '\'', ' ', '-', '.' };

		FieldSanitizerAddress() {
			this(EcollabFieldSanitizer.this.disallowedCharsPatternForSellerAndAdress);
		}

		FieldSanitizerAddress(final String disallowedCharsPattern) {
			super(disallowedCharsPattern);
		}

		private String removeNotWellPositionnedCharacters(String value) {
			if (StringUtils.isBlank(value)) {
				return StringUtils.EMPTY;
			}
			while (ArrayUtils.contains(this.specificPositionnedChars,
					value.charAt(0))) {
				value = value.substring(1);
			}
			while (ArrayUtils.contains(this.specificPositionnedChars,
					value.charAt(value.length() - 1))) {
				value = value.substring(0, value.length() - 1);
			}
			return value;
		}

		@Override
		protected String specificSanitize(String value) {
			return removeNotWellPositionnedCharacters(value);
		}
	}

	class FieldSanitizerDefault extends FieldSanitizer{
		FieldSanitizerDefault() {
			this(StringUtils.EMPTY);
		}

		FieldSanitizerDefault(final String disallowedCharsPattern) {
			super(disallowedCharsPattern);
		}

		@Override
		protected String specificSanitize(String value) {
			return value;
		}

	}
class FieldSanitizerSeller extends FieldSanitizer {

	private final String[][] replacements = new String[][] {
		// @formatter:off
		{ "@", "a" },
		{ "&", "et" }
		// @formatter:on
	};

	FieldSanitizerSeller() {
		this(EcollabFieldSanitizer.this.disallowedCharsPatternForSellerAndAdress);
	}

	FieldSanitizerSeller(final String disallowedChars) {
		super(disallowedChars);
	}

	/**
	 * use replacements tables to sanitize value
	 * @param value to sanitize
	 * @return sanitized value
	 */
	private String doReplacements(String value) {
		if (StringUtils.isBlank(value)) {
			return StringUtils.EMPTY;
		}
		for (int i = 0; i < this.replacements.length; i++) {
			value = StringUtils.replaceAll(value, this.replacements[i][0],
					this.replacements[i][1]);
		}
		return value;
	}

	@Override
	protected String specificSanitize(String value) {
		return doReplacements(value);
	}
}
	private static final String OTHER_FIELDNAME = "Other";

	public final String disallowedCharsPatternForSellerAndAdress = null;

	/**
	 * FieldSanitizer for address fields
	 */
	private final FieldSanitizer fieldSanitizerAddress = new FieldSanitizerAddress();

	/**
	 * FieldSanitizer for seller fields
	 */
	private final FieldSanitizer fieldSanitizerSeller = new FieldSanitizerSeller();

	/**
	 * FieldSanitizer for other fields
	 */
	private final FieldSanitizer fieldSanitizerByDefault = new FieldSanitizerDefault();

	private final Map<String, FieldSanitizer> sanitizersByfieldName;

	public EcollabFieldSanitizer(final String[] sellerFieldNames,
			final String[] addressFieldNames) {

		this.sanitizersByfieldName = new HashMap<>();
		Arrays.stream(sellerFieldNames).forEach(
				field -> this.sanitizersByfieldName.put(field, this.fieldSanitizerSeller));

		Arrays.stream(addressFieldNames).forEach(
				field -> this.sanitizersByfieldName.put(field, this.fieldSanitizerAddress));

		this.sanitizersByfieldName.put(OTHER_FIELDNAME, this.fieldSanitizerByDefault);
	}

	/**
	 * @param value to sanitize
	 * @param fieldName
	 * @return sanitized value
	 */
	public String sanitized(final String value, final String fieldName) {
		var sanitizedValue = value;
		if (this.sanitizersByfieldName.containsKey(fieldName)) {
			sanitizedValue = this.sanitizersByfieldName.get(fieldName).sanitize(sanitizedValue);
		}
		else {
			sanitizedValue = this.sanitizersByfieldName.get(OTHER_FIELDNAME).sanitize(value);
		}

		if (!sanitizedValue.equals(value)) {

			log.warn("{} : {} sanitized to {}", fieldName, value, sanitizedValue);
		}
		return sanitizedValue;
	}
}
