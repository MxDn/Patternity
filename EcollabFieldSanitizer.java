package com.mdm.accounting.batch.marketplace.annualtaxreturn.utils; 

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * @author dejardin-m
 *
 */
@Slf4j
public final class EcollabFieldSanitizer {

	private static final String OTHER_FIELDNAME = "Other";
	private static final Pattern unprintablePattern = Pattern.compile("\\p{C}");
	private static final Pattern disallowedCharsRegex = Pattern
			.compile("[\\\\!#$%&()*+,\\/:;<=>?\\[\\]@^_`{|}~¡¢£¤¥¦§¨©ª¬®¯°±²³´µ¶·¸¹º¼½¾¿ÆÐ×ØÞßæð÷øþ]");

	private static final Pattern positionnedCharsRegex = Pattern.compile("([ \\\\\\-]{1})([ \\\\\\-.]{1})|([.]{1})([\\\\\\-]{1})|(^[ \\\\\\-.])|([ \\\\\\-.]$)");
	  
	private static final String[][] replacements = new String[][] {
		// @formatter:off
		{ "@", "a" },
		{ "&", "et" }
		// @formatter:on
	};

	

	private static final UnaryOperator<String> removeUnprintables = value -> unprintablePattern.matcher(value)
			.replaceAll(StringUtils.EMPTY);
	
	private static final UnaryOperator<String> replaceDisallowedCharsBySpace = value -> disallowedCharsRegex
			.matcher(value).replaceAll(StringUtils.EMPTY);

	private static final UnaryOperator<String> stripAccentsAndNormalizeSpace = value -> StringUtils
			.normalizeSpace(StringUtils.stripAccents(value));

	private static final UnaryOperator<String> doReplacements = value -> {
		if (StringUtils.isBlank(value)) {
			return StringUtils.EMPTY;
		}
		for (int i = 0; i < replacements.length; i++) {
			value =Pattern.compile(replacements[i][0]).matcher(value).replaceAll(replacements[i][1]); 
		}
		return value;
	};

	private static final UnaryOperator<String> removeNotWellPositionnedCharacters = value -> { 
		if (StringUtils.isBlank(value)) {
			return StringUtils.EMPTY;
		}
		while (positionnedCharsRegex.matcher(value).find()) {
			value = positionnedCharsRegex.matcher(value).replaceAll(StringUtils.SPACE);
		}
		return value;
	};

	private static final Function<String, String> sanitizingFunctionsDefault = stripAccentsAndNormalizeSpace
			.compose(removeUnprintables);

	private static final Function<String, String> sanitizingFunctionsAddressFields = stripAccentsAndNormalizeSpace.compose(
			removeNotWellPositionnedCharacters.compose(replaceDisallowedCharsBySpace.compose(removeUnprintables)));

	private static final Function<String, String> sanitizingFunctionsSellerFields = stripAccentsAndNormalizeSpace
			.compose(replaceDisallowedCharsBySpace.compose(doReplacements.compose(removeUnprintables)));

	private final Map<String, Function<String, String>> sanitizingFunctionsByfieldNames;

	public EcollabFieldSanitizer(final String[] sellerFieldNames, final String[] addressFieldNames) {

		sanitizingFunctionsByfieldNames = new HashMap<>();
		
		Arrays.stream(sellerFieldNames).forEach(field -> this.sanitizingFunctionsByfieldNames.put(field,
				EcollabFieldSanitizer.sanitizingFunctionsSellerFields));

		Arrays.stream(addressFieldNames).forEach(field -> this.sanitizingFunctionsByfieldNames.put(field,
				EcollabFieldSanitizer.sanitizingFunctionsAddressFields));

		this.sanitizingFunctionsByfieldNames.put(OTHER_FIELDNAME, EcollabFieldSanitizer.sanitizingFunctionsDefault);

	}

	/**
	 * @param value     to sanitize
	 * @param fieldName
	 * @return sanitized value
	 */
	public String sanitized(final String value, final String fieldName) {
		if (StringUtils.isBlank(value)) {
			return StringUtils.EMPTY;
		}
		
 		var sanitizingFunctions = this.sanitizingFunctionsByfieldNames.get(OTHER_FIELDNAME);
		if (this.sanitizingFunctionsByfieldNames.containsKey(fieldName)) {
			sanitizingFunctions = this.sanitizingFunctionsByfieldNames.get(fieldName);
		}  
		
		String sanitizedValue = sanitizingFunctions.apply(value);
		if (!sanitizedValue.equals(value)) {

			// log.warn("{} : {} sanitized to {}", fieldName, value, sanitizedValue);
		}
		return sanitizedValue;
	}
}
