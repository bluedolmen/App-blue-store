package org.bluedolmen.alfresco.marketplace.modulebuilder;

import java.util.List;

import org.alfresco.repo.template.BaseTemplateProcessorExtension;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
/**
 * @author bpajot
 * 
 * Custom FreeMarker Template for properties value escaping.
 * <p>
 * Render Properties file values escaping unicode characters, and doubling single quotes.<br>
 * <p>
 * Usage: propsescape(String value)
 */
public class PropertiesEscapeTemplateExtension extends BaseTemplateProcessorExtension implements TemplateMethodModelEx {

	@Override
	public Object exec(List args) throws TemplateModelException {
		
		if (args.size() != 1) {
			throw new IllegalArgumentException("The methods needs a single string argument");
		}
		
        final Object arg0 = args.get(0);
        
        if (!(arg0 instanceof TemplateScalarModel)) {
        	throw new IllegalArgumentException("The provided argument has to be a string");
        }

        final String theString = ((TemplateScalarModel)arg0).getAsString();
        return saveConvert(theString, false, true);

	}
	
	/**
	 * This is actually extracted from the Properties file source from Oracle
	 * 
	 * @param theString
	 * @param escapeSpace
	 * @param escapeUnicode
	 * @return
	 */
	private String saveConvert(String theString, boolean escapeSpace, boolean escapeUnicode) {
		
		final int len = theString.length();
		int bufLen = len * 2;
		if (bufLen < 0) {
			bufLen = Integer.MAX_VALUE;
		}
		final StringBuffer outBuffer = new StringBuffer(bufLen);

		for (int x = 0; x < len; x++) {
			
			char aChar = theString.charAt(x);
			// Handle common case first, selecting largest block that
			// avoids the specials below
			if ((aChar > 61) && (aChar < 127)) {
				if (aChar == '\\') {
					outBuffer.append('\\');
					outBuffer.append('\\');
					continue;
				}
				outBuffer.append(aChar);
				continue;
			}
			switch (aChar) {
			case ' ':
				if (x == 0 || escapeSpace)
					outBuffer.append('\\');
				outBuffer.append(' ');
				break;
			case '\t':
				outBuffer.append('\\');
				outBuffer.append('t');
				break;
			case '\n':
				outBuffer.append('\\');
				outBuffer.append('n');
				break;
			case '\r':
				outBuffer.append('\\');
				outBuffer.append('r');
				break;
			case '\f':
				outBuffer.append('\\');
				outBuffer.append('f');
				break;
			case '=': // Fall through
			case ':': // Fall through
			case '#': // Fall through
			case '!':
				outBuffer.append('\\');
				outBuffer.append(aChar);
				break;
			default:
				if (((aChar < 0x0020) || (aChar > 0x007e)) & escapeUnicode) {
					outBuffer.append('\\');
					outBuffer.append('u');
					outBuffer.append(toHex((aChar >> 12) & 0xF));
					outBuffer.append(toHex((aChar >> 8) & 0xF));
					outBuffer.append(toHex((aChar >> 4) & 0xF));
					outBuffer.append(toHex(aChar & 0xF));
				} else {
					outBuffer.append(aChar);
				}
			}
		}
		return outBuffer.toString();
	}
	
    private static char toHex(int nibble) {
        return hexDigit[(nibble & 0xF)];
    }
    
    /** A table of hex digits */
    private static final char[] hexDigit = {
        '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'
    };

}
