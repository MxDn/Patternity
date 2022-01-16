package com.mdm.accounting.batch.marketplace.annualtaxreturn.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.Ignore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class EcollabFieldSanitizerTest {

	private static final String SellerFiled = "SellerFiled";;
	private static final String AddressFiled = "AddressFiled";

	// @formatter:off
	private static final String[] disallowedCharForAddress = {
			//
			"!", "#", "$", "%", "&", "(", ")", "*", "+", ",", "/",
			":", ";", "<", "=",">", "?",
			"@",
			"[", "\\", "]", "^","_",
			"`",
			"{", "|", "}", "~",
			"¡", "¢", "£", "¤", "¥", "¦", "§", "¨", "©","ª", "¬","®", "¯",
			"°", "±", "²", "³", "´", "µ", "¶", "·", "¸", "¹", "º",
			"¼", "½", "¾", "¿",
			"Æ", "Ð", "×", "Ø", "×", "Ø", "Þ", "ß",
			"æ",
			"ð", "÷","ø", "þ" };
	private static final String[] disallowedCharForSeller = {
			//
			"!", "#", "$", "%","(", ")", "*", "+", ",", "/",
			":", ";", "<", "=",">", "?",
			"[", "\\", "]", "^","_",
			"`",
			"{", "|", "}", "~",
			"¡", "¢", "£", "¤", "¥", "¦", "§", "¨", "©","ª", "¬","®", "¯",
			"°", "±", "²", "³", "´", "µ", "¶", "·", "¸", "¹", "º",
			"¼", "½", "¾", "¿",
			"Æ", "Ð", "×", "Ø", "×", "Ø", "Þ", "ß",
			"æ",
			"ð", "÷","ø", "þ" };


	// @formatter:on
	private static Stream<String> paramsDisallowedCharForAddressTests() {
		return Stream.of(disallowedCharForAddress);
	}

	private static Stream<String> paramsDisallowedCharForSellerTests() {
		return Stream.of(disallowedCharForSeller);
	}
	/**
	 * Test parameters for replacements
	 * @return test parameters
	 */
	private static Stream<Arguments> paramsForReplaceCharInSllerFieldTests() {
		// Param 1 : value
		// Param 2 : expectedResult expected result

		// @formatter:off
		return Stream.of(Arguments.of("@","a"),
				Arguments.of("&", "et"),
				Arguments.of("@&","aet"),
				Arguments.of("& @","et a"),
				Arguments.of("Ab@lon","Abalon"));
		// @formatter:on
	}

	/**
	 * Test parameters for cleaning accents
	 * @return test parameters
	 */
	private static Stream<Arguments> paramsForSsanitiAccentsTests() {
		// Param 1 : value
		// Param 2 : expectedResult expected result

		// @formatter:off
		return Stream.of(Arguments.of(null,""),
				Arguments.of("", ""),
				Arguments.of("ÈÉÊË", "EEEE"),
				Arguments.of("ÌÍÎÏ", "IIII"),
				Arguments.of("Ñ", "N"),
				Arguments.of("ÒÓÔÕÖ", "OOOOO"),
				Arguments.of("ÙÚÛÜ ", "UUUU"),
				Arguments.of("Ý", "Y"),
				Arguments.of("àáâãäå ", "aaaaaa"),
				Arguments.of("èéêë", "eeee"),
				Arguments.of("ìíîï", "iiii"),
				Arguments.of("ð", ""),
				Arguments.of("ñ", "n"),
				Arguments.of("òóôõö", "ooooo"),
				Arguments.of("ÀÁÂÃÄÅ", "AAAAAA"));
		// @formatter:on
	}

	private EcollabFieldSanitizer ecollabFieldSsanitir = new EcollabFieldSanitizer(
			new String[] { SellerFiled }, new String[] { AddressFiled });

	@DisplayName("Replace accents by accentsless character in address field.")
	@ParameterizedTest(name = "Expected = {1} | Input = {0}")
	@MethodSource("paramsForSsanitiAccentsTests")
	void ssanitiAddressField_when_value_contains_accents_then_accents_should_be_Replace_by_accents_less_characters(
			String value, String expectedResult) {
		assertEquals(expectedResult,
				this.ecollabFieldSsanitir.sanitized(value, AddressFiled));
	}

	@DisplayName("Replace not allowed character by meaningful space in address field")
	@ParameterizedTest(name = "Expected = meaningful address | Input = meaningful{0}address")
	@MethodSource("paramsDisallowedCharForAddressTests")
	void ssanitiAddressField_when_value_contains_disallowed_char_then_disallowed_char_should_be_replace_by_space(
			String value) {
		assertEquals("meaningful address", this.ecollabFieldSsanitir
				.sanitized(String.format("meaningful %s address", value), AddressFiled));
	}

	@DisplayName("Remove not allowed position character in address field")
	@ParameterizedTest(name = "Expected = meaningful address | Input = {0}meaningful address{0}")
	@ValueSource(strings = { "'", " ", "-", ".", "''", "  ", "--", ".." })
	void ssanitiAddressField_when_value_contains_disallowed_position_char_then_char_should_be_removed(
			String value) {
		assertEquals("meaningful address",
				this.ecollabFieldSsanitir.sanitized(value + "meaningful address" + value,
						AddressFiled));
	}

	@DisplayName("Keep only meaningful space in address field")
	@ParameterizedTest(name = "Expected = meaningful space | Input = {0}")
	@ValueSource(strings = { "meaningful space", " meaningful space ",
	"meaningful   space" })
	void ssanitiAddressField_when_value_contains_spaces_then_only_meaningful_space_should_be_keep(
			String value) {
		assertEquals("meaningful space",
				this.ecollabFieldSsanitir.sanitized(value, AddressFiled));
	}

	@DisplayName("Replace @ and & by 'a' and 'et' in seller field.")
	@ParameterizedTest(name = "Expected = {1} | Input = {0}")
	@MethodSource("paramsForReplaceCharInSllerFieldTests")
	void ssanitiSellerField_when_value_arobase_or_ECommercial_accents_then_should_be_Replace_by_a_or_et(
			String value, String expectedResult) {
		assertEquals(expectedResult,
				this.ecollabFieldSsanitir.sanitized(value, SellerFiled));
	}
	@DisplayName("Replace not allowed character by space in address field")
	@ParameterizedTest
	@MethodSource("paramsDisallowedCharForSellerTests")
	void ssanitiSellerField_when_value_contains_disallowed_char_then_disallowed_char_should_be_replace_by_space(
			String value) {
		assertEquals("meaningful space", this.ecollabFieldSsanitir
				.sanitized(String.format("meaningful %s space", value), SellerFiled));
	}

	@DisplayName("Keep only meaningful space in seller field")
	@ParameterizedTest(name = "Expected = meaningful space | Input = {0}")
	@ValueSource(strings = { "meaningful space", " meaningful space ",
	"meaningful   space" })
	void ssanitiSellerField_when_value_contains_spaces_then_only_meaningful_space_should_be_keep(
			String value)
	{
		assertEquals("meaningful space",
				this.ecollabFieldSsanitir.sanitized(value, SellerFiled));
	}

	@Ignore("To be deleted later")
	void testAllChar()
	{
		for (int i = 1; i < 256; i++) {

			String result = this.ecollabFieldSsanitir.sanitized(String.valueOf((char) i),
					SellerFiled);
			System.out.print(
					String.format("|Hex:" + "0x%02x", i) + "|" + ((char) i) + "|" + result
							+ "|");
			if(i%16 ==0)
			{System.out.println("");
			}
		}

	}
}
