/*
 * Copyright (c) 2002-2013, Mairie de Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.util.beanvalidation;

import static fr.paris.lutece.portal.service.i18n.I18nService.getLocalizedString;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.validation.ConstraintViolation;

/**
 * ValidationError
 */
public class ValidationError
{
    private static final Pattern PATTERN_LOCALIZED_KEY = Pattern.compile( "#i18n\\{(.*?)\\}" );
    private static final String VALUE1 = "value,regexp,min";
    private static final String VALUE2 = "max";

    private Locale _locale;
    private ConstraintViolation _constraintViolation;
    
    /**
     * Constructor
     * @param cv The constraint violation
     * @param locale  The locale
     */
    public ValidationError( ConstraintViolation cv , Locale locale )
    {
        _constraintViolation = cv;
        _locale = locale;
    }
    
    /**
     * Return the error message
     * @return The error message
     */
    public String getMessage()
    {
        String strMessage = _constraintViolation.getMessage();
        Matcher matcher = PATTERN_LOCALIZED_KEY.matcher( strMessage );

        if ( matcher.find(  ) )
        {
            StringBuffer sb = new StringBuffer(  );

            do
            {
                matcher.appendReplacement( sb, getLocalizedString( matcher.group( 1 ), _locale ) );
            }
            while ( matcher.find(  ) );

            matcher.appendTail( sb );
            strMessage = sb.toString(  );
        }
        
        String strValue1 = "";
        String strValue2 = "";
        Map<String,Object> mapAttributes = _constraintViolation.getConstraintDescriptor().getAttributes();
        for( String strKey : mapAttributes.keySet() )
        {
            if( VALUE1.contains(strKey))
            {
                strValue1 = getValue( mapAttributes.get(strKey));
            }
            else if( VALUE2.contains(strKey))
            {
                strValue2 = getValue( mapAttributes.get(strKey));
            }
        }
        strMessage = MessageFormat.format( strMessage, _constraintViolation.getPropertyPath() , _constraintViolation.getInvalidValue() , strValue1 , strValue2);
        
        return strMessage;
       
    }

    /**
     * Convert an unkown type value to a String value
     * @param value The valus
     * @return The value as a String
     */
    private String getValue(Object value)
    {
        if( value instanceof Integer )
        {
            return Integer.toString( (Integer) value );
        }
        if( value instanceof Long )
        {
            return Long.toString( (Long) value );
        }
        return (String) value;
    }
}
