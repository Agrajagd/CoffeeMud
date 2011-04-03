package com.planet_ink.coffee_mud.core;
import java.util.*;

/* 
   Copyright 2000-2011 Bo Zimmerman

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
public class CMStrings
{
    private CMStrings(){super();}
    private static CMStrings inst=new CMStrings();
    public final static CMStrings instance(){return inst;}
    
    public final static String SPACES="                                                                                                                                          ";
    public final static String repeat(final String str1, final int times)
    {
        if(times<=0) return "";
        StringBuffer str=new StringBuffer("");
        for(int i=0;i<times;i++)
            str.append(str1);
        return str.toString();
    }
    
    public final static boolean isUpperCase(final String str) 
    {
    	for(int c=0;c<str.length();c++)
    		if(!Character.isUpperCase(str.charAt(c)))
    			return false;
    	return true;
    }
    
    public final static boolean isLowerCase(final String str) 
    {
    	for(int c=0;c<str.length();c++)
    		if(!Character.isLowerCase(str.charAt(c)))
    			return false;
    	return true;
    }
    
    public final static String endWithAPeriod(final String str)
    {
        if((str==null)||(str.length()==0)) return str;
        int x=str.length()-1;
        while((x>=0)
        &&((Character.isWhitespace(str.charAt(x)))
            ||((x>0)&&((str.charAt(x)!='^')&&(str.charAt(x-1)=='^')&&((--x)>=0))))) 
                x--;
        if(x<0) return str;
        if((str.charAt(x)=='.')||(str.charAt(x)=='!')||(str.charAt(x)=='?')) 
        	return str.trim()+" ";
        return str.substring(0,x+1)+". "+str.substring(x+1).trim();
    }
    
    public final static String bytesToStr(final byte[] b)
    { 
    	if(b==null) 
    		return "";
    	try
    	{
    		return new String(b,CMProps.getVar(CMProps.SYSTEM_CHARSETINPUT));
    	}
    	catch(Exception e)
    	{
    		return new String(b);
    	}
    }
    
    public final static byte[] strToBytes(final String str)
    { 
    	try
    	{ 
    		return str.getBytes(CMProps.getVar(CMProps.SYSTEM_CHARSETINPUT));
    	}
    	catch(Exception e)
    	{
    		return str.getBytes();
    	}
    }
    
    public final static boolean isVowel(final char c)
    { 
    	return (("aeiou").indexOf(Character.toLowerCase(c))>=0);
    }
    
    public final static String replaceAllofAny(final String str, final char[] theseChars, final char with)
    {
        if((str==null)
        ||(theseChars==null)
        ||(str.length()==0)
        ||(!containsAny(str,theseChars)))
            return str;
        final char[] newChars = str.toCharArray();
        for(int i=str.length()-1;i>=0;i--)
        	if(contains(theseChars,str.charAt(i)))
	        {
	        	newChars[i]=with;
	        }
    	return new String(newChars);
    }
    
    public final static String deleteAllofAny(final String str, final char[] theseChars)
    {
        if((str==null)
        ||(theseChars==null)
        ||(str.length()==0)
        ||(!containsAny(str,theseChars)))
            return str;
        final StringBuilder buf=new StringBuilder(str);
        for(int i=buf.length()-1;i>=0;i--)
        	if(contains(theseChars,buf.charAt(i)))
	        {
    			buf.deleteCharAt(i);
	        }
    	return buf.toString();
    }
    
    public final static String replaceAll(String str, final String thisStr, final String withThisStr)
    {
        if((str==null)
        ||(thisStr==null)
        ||(withThisStr==null)
        ||(str.length()==0)
        ||(thisStr.length()==0))
            return str;
        for(int i=str.length()-1;i>=0;i--)
            if(str.charAt(i)==thisStr.charAt(0))
                if(str.substring(i).startsWith(thisStr))
                    str=str.substring(0,i)+withThisStr+str.substring(i+thisStr.length());
        return str;
    }
    
    public final static String replaceWord(String str, final String thisStr, final String withThisStr)
    {
        if((str==null)
        ||(thisStr==null)
        ||(withThisStr==null)
        ||(str.length()==0)
        ||(thisStr.length()==0))
            return str;
        final String uppercaseWithThisStr=withThisStr.toUpperCase();
        for(int i=str.length()-1;i>=0;i--)
        {
            if((str.charAt(i)==thisStr.charAt(0))
            &&((i==0)||(!Character.isLetter(str.charAt(i-1)))))
                if((str.substring(i).toLowerCase().startsWith(thisStr.toLowerCase()))
                &&((str.length()==i+thisStr.length())||(!Character.isLetter(str.charAt(i+thisStr.length())))))
                {
                	String oldWord=str.substring(i,i+thisStr.length());
                	if(oldWord.toUpperCase().equals(oldWord)) 
                        str=str.substring(0,i)+uppercaseWithThisStr+str.substring(i+thisStr.length());
                	else
                	if(oldWord.toLowerCase().equals(oldWord))
                        str=str.substring(0,i)+uppercaseWithThisStr.toLowerCase()+str.substring(i+thisStr.length());
                	else
                	if((oldWord.length()>0)&&(Character.isUpperCase(oldWord.charAt(0)))) 
                        str=str.substring(0,i)+uppercaseWithThisStr.charAt(0)+uppercaseWithThisStr.substring(1).toLowerCase()+str.substring(i+thisStr.length());
                	else
                        str=str.substring(0,i)+uppercaseWithThisStr.toLowerCase()+str.substring(i+thisStr.length());
                }
        }
        return str;
    }
    
    public final static String replaceFirstWord(String str, final String thisStr, final String withThisStr)
    {
        if((str==null)
        ||(thisStr==null)
        ||(withThisStr==null)
        ||(str.length()==0)
        ||(thisStr.length()==0))
            return str;
        String uppercaseWithThisStr=withThisStr.toUpperCase();
        for(int i=str.length()-1;i>=0;i--)
        {
            if((str.charAt(i)==thisStr.charAt(0))
            &&((i==0)||(!Character.isLetter(str.charAt(i-1)))))
                if((str.substring(i).toLowerCase().startsWith(thisStr.toLowerCase()))
                &&((str.length()==i+thisStr.length())||(!Character.isLetter(str.charAt(i+thisStr.length())))))
                {
                	String oldWord=str.substring(i,i+thisStr.length());
                	if(oldWord.toUpperCase().equals(oldWord)) 
                        return str.substring(0,i)+uppercaseWithThisStr+str.substring(i+thisStr.length());
                	else
                	if(oldWord.toLowerCase().equals(oldWord))
                		return str.substring(0,i)+uppercaseWithThisStr.toLowerCase()+str.substring(i+thisStr.length());
                	else
                	if((oldWord.length()>0)&&(Character.isUpperCase(oldWord.charAt(0)))) 
                		return str.substring(0,i)+uppercaseWithThisStr.charAt(0)+uppercaseWithThisStr.substring(1).toLowerCase()+str.substring(i+thisStr.length());
                	else
                		return str.substring(0,i)+uppercaseWithThisStr.toLowerCase()+str.substring(i+thisStr.length());
                }
        }
        return str;
    }
    
    public final static String replaceFirst(String str, final String thisStr, final String withThisStr)
    {
        if((str==null)
        ||(thisStr==null)
        ||(withThisStr==null)
        ||(str.length()==0)
        ||(thisStr.length()==0))
            return str;
        for(int i=str.length()-1;i>=0;i--)
            if(str.charAt(i)==thisStr.charAt(0))
                if(str.substring(i).startsWith(thisStr))
                {
                    str=str.substring(0,i)+withThisStr+str.substring(i+thisStr.length());
                    return str;
                }
        return str;
    }
    
    public final static String capitalizeAndLower(final String name)
    {
        if((name==null)||(name.length()==0)) return "";
        char[] c=name.toCharArray();
        int i=0;
        for(;i<c.length;i++)
        {
            if(c[i]=='^')
                i++;
            else
            if(Character.isLetter(c[i]))
                break;
        }
        if(i<c.length)
            c[i]=Character.toUpperCase(c[i]);
        i++;
        for(;i<c.length;i++)
            if(!Character.isLowerCase(c[i]))
                c[i]=Character.toLowerCase(c[i]);
        return new String(c).trim();
    }
    
    public final static String capitalizeFirstLetter(final String name)
    {
        if((name==null)||(name.length()==0)) 
        	return "";
        char[] c=name.toCharArray();
        int i=0;
        for(;i<c.length;i++)
            if(c[i]=='^')
                i++;
            else
            if(Character.isLetter(c[i]))
                break;
        if(i<c.length)
            c[i]=Character.toUpperCase(c[i]);
        return new String(c).trim();
    }
    
    public final static String lastWordIn(final String thisStr)
    {
        int x=thisStr.lastIndexOf(' ');
        if(x>=0)
            return thisStr.substring(x+1);
        return thisStr;
    }
    
    public final static String getSayFromMessage(final String msg)
    {
        if(msg==null) return null;
        int start=msg.indexOf('\'');
        int end=msg.lastIndexOf('\'');
        if((start>0)&&(end>start))
            return msg.substring(start+1,end);
        return null;
    }
    public final static String substituteSayInMessage(final String affmsg, final String msg)
    {
        if(affmsg==null) return null;
        final int start=affmsg.indexOf('\'');
        final int end=affmsg.lastIndexOf('\'');
        if((start>0)&&(end>start))
            return affmsg.substring(0,start+1)+msg+affmsg.substring(end);
        return affmsg;
    }

    public final static boolean containsIgnoreCase(final String[] strs, final String str)
    {
    	if((str==null)||(strs==null)) return false;
    	for(int s=0;s<strs.length;s++)
    		if(strs[s].equalsIgnoreCase(str))
    			return true;
    	return false;
    }
    
    public final static boolean compareStringArrays(final String[] A1, final String[] A2)
    {
        if(((A1==null)||(A1.length==0))
        &&((A2==null)||(A2.length==0)))
            return true;
        if((A1==null)||(A2==null)) return false;
        if(A1.length!=A2.length) return false;
        for(int i=0;i<A1.length;i++)
        {
            boolean found=false;
            for(int i2=0;i2<A2.length;i2++)
                if(A1[i].equalsIgnoreCase(A2[i]))
                { found=true; break;}
            if(!found) return false;
        }
        return true;
    }
    
    public final static boolean contains(final String[] strs, final String str)
    {
    	if((str==null)||(strs==null)) return false;
    	for(int s=0;s<strs.length;s++)
    		if(strs[s].equals(str))
    			return true;
    	return false;
    }
    
    public final static boolean contains(final char[] anycs, final char c)
    {
    	for(char c1 : anycs)
    		if(c1==c)
    			return true;
    	return false;
    }
    
    public final static boolean containsAny(final String str, final char[] anycs)
    {
    	if((str==null)||(anycs==null)) return false;
    	for(int i=0;i<str.length();i++)
    		if(contains(anycs,str.charAt(i)))
    			return true;
    	return false;
    }
    
    public final static String removeColors(final String s)
    {
        if(s==null) return "";
        final StringBuffer str=new StringBuffer(s);
        int colorStart=-1;
        for(int i=0;i<str.length();i++)
        {
            switch(str.charAt(i))
            {
            case 'm':
                if(colorStart>=0)
                {
                    str.delete(colorStart,i+1);
                    colorStart=-1;
                }
                break;
            case (char)27: colorStart=i; break;
            case '^':
                if((i+1)<str.length())
                {
                    int tagStart=i;
                    char c=str.charAt(i+1);
                    if((c=='<')||(c=='&'))
                    {
                        i+=2;
                        while(i<(str.length()-1))
                        {
                            if(((c=='<')&&((str.charAt(i)!='^')||(str.charAt(i+1)!='>')))
                            ||((c=='&')&&(str.charAt(i)!=';')))
                            {
                                i++;
                                if(i>=(str.length()-1))
                                {
                                    i=tagStart;
                                    str.delete(i,i+2); 
                                    i--;
                                    break;
                                }
                            }
                            else
                            {
                                if(c=='<')
                                    str.delete(tagStart,i+2);
                                else
                                    str.delete(tagStart,i+1);
                                i=tagStart-1;
                                break;
                            }
                        }
                    }
                    else
                    {
                        str.delete(i,i+2); 
                        i--;
                    }
                }
                else
                {
                    str.delete(i,i+2); 
                    i--;
                }
                break;
            }
        }
        return str.toString();
    }
    
    public final static int lengthMinusColors(final String thisStr)
    {
        if(thisStr==null) 
        	return 0;
        int size=0;
        for(int i=0;i<thisStr.length();i++)
        {
            if(thisStr.charAt(i)=='^')
            {
                i++;
                if((i+1)<thisStr.length())
                {
                    int tagStart=i;
                    char c=thisStr.charAt(i);
                    if((c=='<')||(c=='&'))
	                    while(i<(thisStr.length()-1))
	                    {
	                        if(((c=='<')&&((thisStr.charAt(i)!='^')||(thisStr.charAt(i+1)!='>')))
	                        ||((c=='&')&&(thisStr.charAt(i)!=';')))
	                        {
	                            i++;
	                            if(i>=(thisStr.length()-1))
	                            {
	                                i=tagStart+1;
	                                break;
	                            }
	                        }
	                        else
	                        {
	                            i++;
	                            break;
	                        }
	                    }
                }
            }
            else
                size++;
        }
        return size;
    }
    
    public final static Hashtable<Object,Integer> makeNumericHash(final Object[] obj)
    {
    	Hashtable<Object,Integer> H=new Hashtable<Object,Integer>();
    	for(int i=0;i<obj.length;i++)
    		H.put(obj[i],Integer.valueOf(i));
    	return H;
    }
    
    public final static String padCenter(final String thisStr, final int thisMuch)
    {
    	final int lenMinusColors=lengthMinusColors(thisStr);
        if(lenMinusColors>thisMuch)
            return removeColors(thisStr).substring(0,thisMuch);
        final int size=(thisMuch-lenMinusColors)/2;
        int rest=thisMuch-lenMinusColors-size;
        if(rest<0) rest=0;
        return SPACES.substring(0,size)+thisStr+SPACES.substring(0,rest);
    }
    public final static String padLeft(final String thisStr, final int thisMuch)
    {
    	final int lenMinusColors=lengthMinusColors(thisStr);
        if(lenMinusColors>thisMuch)
            return removeColors(thisStr).substring(0,thisMuch);
        return SPACES.substring(0,thisMuch-lenMinusColors)+thisStr;
    }
    public final static String padLeft(final String thisStr, final String colorPrefix, final int thisMuch)
    {
    	final int lenMinusColors=lengthMinusColors(thisStr);
        if(lenMinusColors>thisMuch)
            return colorPrefix+removeColors(thisStr).substring(0,thisMuch);
        return SPACES.substring(0,thisMuch-lenMinusColors)+colorPrefix+thisStr;
    }
    public final static String padRight(final String thisStr, final int thisMuch)
    {
    	final int lenMinusColors=lengthMinusColors(thisStr);
        if(lenMinusColors>thisMuch)
            return removeColors(thisStr).substring(0,thisMuch);
        if(thisMuch-lenMinusColors >= SPACES.length())
            return thisStr+SPACES;
        return thisStr+SPACES.substring(0,thisMuch-lenMinusColors);
    }
    public final static String limit(final String thisStr, final int thisMuch)
    {
    	final int lenMinusColors=lengthMinusColors(thisStr);
        if(lenMinusColors>thisMuch)
            return removeColors(thisStr).substring(0,thisMuch);
        return thisStr;
    }
    public final static String padRight(final String thisStr, final String colorSuffix, final int thisMuch)
    {
    	final int lenMinusColors=lengthMinusColors(thisStr);
        if(lenMinusColors>thisMuch)
            return removeColors(thisStr).substring(0,thisMuch)+colorSuffix;
        if(thisMuch-lenMinusColors >= SPACES.length())
            return thisStr+colorSuffix+SPACES;
        return thisStr+colorSuffix+SPACES.substring(0,thisMuch-lenMinusColors);
    }
    public final static String padRightPreserve(final String thisStr, final int thisMuch)
    {
    	final int lenMinusColors=lengthMinusColors(thisStr);
        if(lenMinusColors>thisMuch)
            return removeColors(thisStr);
        if(thisMuch-lenMinusColors >= SPACES.length())
            return thisStr+SPACES;
        return thisStr+SPACES.substring(0,thisMuch-lenMinusColors);
    }
    public final static String centerPreserve(final String thisStr, final int thisMuch)
    {
    	final int lenMinusColors=lengthMinusColors(thisStr);
        if(lenMinusColors>thisMuch)
            return removeColors(thisStr);
        final int left=(thisMuch-lenMinusColors)/2;
        final int right=((left+left+lenMinusColors)<thisMuch)?left+1:left;
        if(thisMuch-lenMinusColors >= SPACES.length())
            return thisStr+SPACES;
        return SPACES.substring(0,left)+thisStr+SPACES.substring(0,right);
    }
    public final static String padLeftPreserve(final String thisStr, final int thisMuch)
    {
    	final int lenMinusColors=lengthMinusColors(thisStr);
        if(lenMinusColors>thisMuch)
            return removeColors(thisStr);
        return SPACES.substring(0,thisMuch-lenMinusColors)+thisStr;
    }
    
    public final static String sameCase(final String str, final char c)
    {
        if(Character.isUpperCase(c))
            return str.toUpperCase();
        return str.toLowerCase();
    }

	// states: 0 = done after this one,-1 = done a char ago,-2 = eat & same state,-99 = error,
	// chars: 254 = digit, 253 = letter, 252 = digitNO0, 255=eof
	private static final int[][]	STRING_EXP_SM	= { { -1 }, // 0 == done after this one, 1 == real first state
			{ ' ', -2, '=', 2, '>', 4, '<', 5, '!', 2, '(', 0, ')', 0, '\"', 3, '+', 0, '-', 0, '*', 0, '/', 0, '&', 6, '?',0, '|', 7, '\'', 8, '`', 9, '$', 10, 253, 12, 252, 13, '0', 15, 255, 255, -99 }, // 1
			{ '=', 0, -1 }, // 2 -- starts with =
			{ '\"', 0, 255, -99, 3 }, // 3 -- starts with "
			{ '=', 0, '>', 0, -1 }, // 4 -- starts with <
			{ '=', 0, '<', 0, -1 }, // 5 -- starts with >
			{ '&', 0, -1 }, // 6 -- starts with &
			{ '|', 0, -1 }, // 7 -- starts with |
			{ '\'', 0, 255, -99, 8 }, // 8 -- starts with '
			{ '`', 0, 255, -99, 9 }, // 9 -- starts with `
			{ 253, 11, '_', 11, -99 }, // 10 == starts with $
			{ 253, 11, 254, 11, '_', 11, 255, -1, -1 }, // 11=starts $Letter
			{ 253, 12, 255, -1, -1 },				// 12=starts with letter
			{ 254, 13, '.', 14, -1}, // 13=starts with a digit
			{ 254, 14, '.', -99, -1}, // 14=continues a digit
			{ 254, -99, '.', 14, -1} // 15=starts with a 0
	};

	private static class StringExpToken
	{
		public int		type	= -1;
		public String	value	= "";
		public double 	numValue  = 0.0;

		public final static StringExpToken token(final int type, final String value) throws Exception
		{
			final StringExpToken token = new StringExpToken();
			token.type = type;
			token.value = value;
			if((value.length()>0)&&(Character.isDigit(value.charAt(0))))
				token.numValue = Double.parseDouble(value);
			return token;
		}
		private StringExpToken() { }
	}

	private static StringExpToken nextToken(final List<StringExpToken> tokens, final int[] index) {
		if(index[0]>=tokens.size()) return null;
		return (StringExpToken)tokens.get(index[0]++);
	}
	
	private static final int	STRING_EXP_TOKEN_EVALUATOR	= 1;
	private static final int	STRING_EXP_TOKEN_OPENPAREN	= 2;
	private static final int	STRING_EXP_TOKEN_CLOSEPAREN	= 3;
	private static final int	STRING_EXP_TOKEN_WORD		= 4;
	private static final int	STRING_EXP_TOKEN_STRCONST	= 5;
	private static final int	STRING_EXP_TOKEN_COMBINER	= 6;
	private static final int	STRING_EXP_TOKEN_NOT		= 7;
	private static final int	STRING_EXP_TOKEN_NUMCONST	= 8;
	private static final int	STRING_EXP_TOKEN_UKNCONST	= 9;

	private static StringExpToken makeTokenType(String token, final Map<String,Object> variables, final boolean emptyVars) throws Exception
	{
		if ((token == null)||(token.length()==0))
			return null;
		if (token.startsWith("\""))
			return StringExpToken.token(STRING_EXP_TOKEN_STRCONST, token.substring(1, token.length() - 1));
		if (token.startsWith("\'"))
			return StringExpToken.token(STRING_EXP_TOKEN_STRCONST, token.substring(1, token.length() - 1));
		if (token.startsWith("`"))
			return StringExpToken.token(STRING_EXP_TOKEN_STRCONST, token.substring(1, token.length() - 1));
		if (token.equals("("))
			return StringExpToken.token(STRING_EXP_TOKEN_OPENPAREN, token);
		if (token.equals(")"))
			return StringExpToken.token(STRING_EXP_TOKEN_CLOSEPAREN, token);
		if (token.equalsIgnoreCase("IN"))
			return StringExpToken.token(STRING_EXP_TOKEN_EVALUATOR, token);
		if (token.equals("+")||token.equals("-")||token.equals("*")||token.equals("/")||token.equals("?"))
			return StringExpToken.token(STRING_EXP_TOKEN_COMBINER, token);
		if (token.equals("!")||token.equalsIgnoreCase("NOT"))
			return StringExpToken.token(STRING_EXP_TOKEN_NOT, token);
		if(Character.isDigit(token.charAt(0)))
			return StringExpToken.token(STRING_EXP_TOKEN_NUMCONST, token);
		if (token.startsWith("$"))
		{
			token = token.substring(1);
			Object value = variables.get(token);
			if(!(value instanceof String))
				value = variables.get(token.toUpperCase().trim());
			if((value == null)&&(emptyVars))
				value="";
			else
			if(!(value instanceof String))
				throw new Exception("Undefined variable found: $" + token);
			if((value.toString().length()>0)&&(!CMath.isNumber(value.toString())))
				return StringExpToken.token(STRING_EXP_TOKEN_STRCONST, value.toString());
			return StringExpToken.token(STRING_EXP_TOKEN_UKNCONST, value.toString());
		}
		if ((token.charAt(0) == '_') || (Character.isLetterOrDigit(token.charAt(0))) || (token.charAt(0) == '|') || (token.charAt(0) == '&'))
			return StringExpToken.token(STRING_EXP_TOKEN_WORD, token);
		return StringExpToken.token(STRING_EXP_TOKEN_EVALUATOR, token);
	}

	private static StringExpToken nextStringToken(final String expression, final int[] index, final Map<String,Object> variables, final boolean emptyVars) throws Exception
	{
		int[] stateBlock = STRING_EXP_SM[1];
		final StringBuffer token = new StringBuffer("");
		while (index[0] < expression.length())
		{
			char c = expression.charAt(index[0]);
			int nextState = stateBlock[stateBlock.length - 1];
			boolean match = false;
			for (int x = 0; x < stateBlock.length - 1; x += 2)
			{
				switch (stateBlock[x])
				{
					case 254:
						match = Character.isDigit(c);
						break;
					case 252:
						match = Character.isDigit(c)&&(c!='0');
						break;
					case 253:
						match = Character.isLetter(c);
						break;
					case 255:
						break; // nope, not yet
					default:
						match = (c == stateBlock[x]);
						break;
				}
				if (match)
				{
					nextState = stateBlock[x + 1];
					break;
				}
			}
			switch (nextState)
			{
				case 255:
					return null;
				case -99:
					throw new Exception("Illegal character in expression: " + c);
				case -2:
					index[0]++;
					break;
				case -1:
					return makeTokenType(token.toString(), variables, emptyVars);
				case 0:
				{
					token.append(c);
					index[0]++;
					return makeTokenType(token.toString(), variables, emptyVars);
				}
				default:
				{
					token.append(c);
					index[0]++;
					stateBlock = STRING_EXP_SM[nextState];
					break;
				}
			}
		}
		int finalState = stateBlock[stateBlock.length - 1];
		for (int x = 0; x < stateBlock.length - 1; x += 2)
			if (stateBlock[x] == 255)
			{
				finalState = stateBlock[x + 1];
				break;
			}
		switch (finalState)
		{
			case -99:
				throw new Exception("Expression ended prematurely");
			case -1:
			case 0:
				return makeTokenType(token.toString(), variables, emptyVars);
			default:
				return null;
		}
	}

	/*
	 * case STRING_EXP_TOKEN_EVALUATOR: case STRING_EXP_TOKEN_OPENPAREN: case STRING_EXP_TOKEN_CLOSEPAREN: case STRING_EXP_TOKEN_WORD: case
	 * STRING_EXP_TOKEN_CONST: case STRING_EXP_TOKEN_COMBINER:
	 */
	public final static String matchSimpleConst(final List<StringExpToken> tokens, final int[] index, final Map<String,Object> variables) throws Exception
	{
		final int[] i = (int[]) index.clone();
		StringExpToken token = nextToken(tokens, i);
		if (token == null)
			return null;
		if((token.type != STRING_EXP_TOKEN_STRCONST)
		&& (token.type != STRING_EXP_TOKEN_UKNCONST))
			return null;
		index[0] = i[0];
		return token.value;
	}

	public final static Double matchSimpleNumber(final List<StringExpToken> tokens, final int[] index, final Map<String,Object> variables) throws Exception
	{
		final int[] i = (int[]) index.clone();
		final StringExpToken token = nextToken(tokens, i);
		if (token == null)
			return null;
		if((token.type != STRING_EXP_TOKEN_NUMCONST)
		&& (token.type != STRING_EXP_TOKEN_UKNCONST))
			return null;
		index[0] = i[0];
		return Double.valueOf(token.numValue);
	}
	
	public final static String matchCombinedString(final List<StringExpToken> tokens, final int[] index, final Map<String,Object> variables) throws Exception
	{
		int[] i = (int[]) index.clone();
		StringExpToken token = nextToken(tokens, i);
		if (token == null)
			return null;
		if (token.type == STRING_EXP_TOKEN_OPENPAREN)
		{
			final String testInside = matchCombinedString(tokens, i, variables);
			if (testInside != null)
			{
				token = nextToken(tokens, i);
				if (token.type != STRING_EXP_TOKEN_CLOSEPAREN)
					return null;
				index[0] = i[0];
				return testInside;
			}
		}
		i = (int[]) index.clone();
		final String leftValue = matchSimpleConst(tokens, i, variables);
		if (leftValue == null)
			return null;
		final int[] i2 = (int[]) i.clone();
		token = nextToken(tokens, i2);
		if ((token == null) || (token.type != STRING_EXP_TOKEN_COMBINER))
		{
			index[0] = i[0];
			return leftValue;
		}
		if(!token.value.equals("+")) 
			throw new Exception("Can't combine a string using '"+token.value+"'");
		i[0] = i2[0];
		final String rightValue = matchCombinedString(tokens, i, variables);
		if (rightValue == null)
			return null;
		index[0] = i[0];
		return leftValue + rightValue;
	}

	public final static Double matchCombinedNum(final List<StringExpToken> tokens, final int[] index, final Map<String,Object> variables) throws Exception
	{
		int[] i = (int[]) index.clone();
		StringExpToken token = nextToken(tokens, i);
		if (token == null)
			return null;
		if (token.type == STRING_EXP_TOKEN_OPENPAREN)
		{
			final Double testInside = matchCombinedNum(tokens, i, variables);
			if (testInside != null)
			{
				token = nextToken(tokens, i);
				if (token.type != STRING_EXP_TOKEN_CLOSEPAREN)
					return null;
				index[0] = i[0];
				return testInside;
			}
		}
		i = (int[]) index.clone();
		final Double leftValue = matchSimpleNumber(tokens, i, variables);
		if (leftValue == null)
			return null;
		final int[] i2 = (int[]) i.clone();
		token = nextToken(tokens, i2);
		if ((token == null) || (token.type != STRING_EXP_TOKEN_COMBINER))
		{
			index[0] = i[0];
			return leftValue;
		}
		i[0] = i2[0];
		final Double rightValue = matchCombinedNum(tokens, i, variables);
		if (rightValue == null)
			return null;
		index[0] = i[0];
		if(token.value.equals("+"))
		{
			index[0] = i[0];
			return Double.valueOf(leftValue.doubleValue() + rightValue.doubleValue());
		}
		else
		if(token.value.equals("-")) 
		{
			index[0] = i[0];
			return Double.valueOf(leftValue.doubleValue() - rightValue.doubleValue());
		}
		else
		if(token.value.equals("*")) 
		{
			index[0] = i[0];
			return Double.valueOf(leftValue.doubleValue() * rightValue.doubleValue());
		}
		else
		if(token.value.equals("/")) 
		{
			index[0] = i[0];
			return Double.valueOf(leftValue.doubleValue() / rightValue.doubleValue());
		}
		else
		if(token.value.equals("?")) 
		{
			index[0] = i[0];
			return Double.valueOf(Math.round((Math.random() * (rightValue.doubleValue()-leftValue.doubleValue())) + leftValue.doubleValue())) ;
		}
		else
			throw new Exception("Unknown math combiner "+token.value);
	}
	
	public final static Boolean matchStringEvaluation(final List<StringExpToken> tokens, final int[] index, final Map<String,Object> variables) throws Exception
	{
		int[] i = (int[]) index.clone();
		StringExpToken token = nextToken(tokens, i);
		if (token == null)
			return null;
		if(token.type == STRING_EXP_TOKEN_NOT)
		{
			final Boolean testInside = matchExpression(tokens, i, variables);
			if (testInside != null)
			{
				index[0] = i[0];
				return new Boolean(!testInside.booleanValue());
			}
		}
		else
		if (token.type == STRING_EXP_TOKEN_OPENPAREN)
		{
			final Boolean testInside = matchStringEvaluation(tokens, i, variables);
			if (testInside != null)
			{
				token = nextToken(tokens, i);
				if (token.type != STRING_EXP_TOKEN_CLOSEPAREN)
					return null;
				index[0] = i[0];
				return testInside;
			}
		}
		i = (int[]) index.clone();
		final String leftValue = matchCombinedString(tokens, i, variables);
		if (leftValue == null)
			return null;
		token = nextToken(tokens, i);
		if (token == null)
			return null;
		if (token.type != STRING_EXP_TOKEN_EVALUATOR)
			return null;
		final String rightValue = matchCombinedString(tokens, i, variables);
		if (rightValue == null)
			return null;
		final int compare = leftValue.compareToIgnoreCase(rightValue);
		final Boolean result;
		if (token.value.equals(">"))
			result = new Boolean(compare > 0);
		else if (token.value.equals(">="))
			result = new Boolean(compare >= 0);
		else if (token.value.equals("<"))
			result = new Boolean(compare < 0);
		else if (token.value.equals("<="))
			result = new Boolean(compare <= 0);
		else if (token.value.equals("="))
			result = new Boolean(compare == 0);
		else if (token.value.equals("!="))
			result = new Boolean(compare != 0);
		else if (token.value.equals("<>"))
			result = new Boolean(compare != 0);
		else if (token.value.equals("><"))
			result = new Boolean(compare != 0);
		else
		if (token.value.equalsIgnoreCase("IN"))
			result = new Boolean(rightValue.toUpperCase().indexOf(leftValue.toUpperCase())>=0);
		else
			return null;
		index[0] = i[0];
		return result;
	}

	public final static Boolean matchNumEvaluation(final List<StringExpToken> tokens, final int[] index, final Map<String,Object> variables) throws Exception
	{
		int[] i = (int[]) index.clone();
		StringExpToken token = nextToken(tokens, i);
		if (token == null)
			return null;
		if(token.type == STRING_EXP_TOKEN_NOT)
		{
			final Boolean testInside = matchExpression(tokens, i, variables);
			if (testInside != null)
			{
				index[0] = i[0];
				return new Boolean(!testInside.booleanValue());
			}
		}
		else
		if (token.type == STRING_EXP_TOKEN_OPENPAREN)
		{
			final Boolean testInside = matchNumEvaluation(tokens, i, variables);
			if (testInside != null)
			{
				token = nextToken(tokens, i);
				if (token.type != STRING_EXP_TOKEN_CLOSEPAREN)
					return null;
				index[0] = i[0];
				return testInside;
			}
		}
		i = (int[]) index.clone();
		final Double leftValue = matchCombinedNum(tokens, i, variables);
		if (leftValue == null)
			return null;
		token = nextToken(tokens, i);
		if (token == null)
			return null;
		if (token.type != STRING_EXP_TOKEN_EVALUATOR)
			return null;
		final Double rightValue = matchCombinedNum(tokens, i, variables);
		if (rightValue == null)
			return null;
		final Boolean result;
		if (token.value.equals(">"))
			result = new Boolean(leftValue.doubleValue() > rightValue.doubleValue());
		else if (token.value.equals(">="))
			result = new Boolean(leftValue.doubleValue() >= rightValue.doubleValue());
		else if (token.value.equals("<"))
			result = new Boolean(leftValue.doubleValue() < rightValue.doubleValue());
		else if (token.value.equals("<="))
			result = new Boolean(leftValue.doubleValue() <= rightValue.doubleValue());
		else if (token.value.equals("="))
			result = new Boolean(leftValue.doubleValue() == rightValue.doubleValue());
		else if (token.value.equals("!="))
			result = new Boolean(leftValue.doubleValue() != rightValue.doubleValue());
		else if (token.value.equals("<>"))
			result = new Boolean(leftValue.doubleValue() != rightValue.doubleValue());
		else if (token.value.equals("><"))
			result = new Boolean(leftValue.doubleValue() != rightValue.doubleValue());
		else
		if (token.value.equalsIgnoreCase("IN"))
			throw new Exception("Can't use IN operator on numbers.");
		else
			return null;
		index[0] = i[0];
		return result;
	}
	
	public final static Boolean matchExpression(final List<StringExpToken> tokens, final int[] index, final Map<String,Object> variables) throws Exception
	{
		int[] i = (int[]) index.clone();
		StringExpToken token = nextToken(tokens, i);
		if (token == null)
			return null;
		Boolean leftExpression = null;
		if(token.type == STRING_EXP_TOKEN_NOT)
		{
			final Boolean testInside = matchExpression(tokens, i, variables);
			if (testInside != null)
			{
				index[0] = i[0];
				leftExpression = new Boolean(!testInside.booleanValue());
			}
		}
		else
		if (token.type == STRING_EXP_TOKEN_OPENPAREN)
		{
			final Boolean testInside = matchExpression(tokens, i, variables);
			if (testInside != null)
			{
				token = nextToken(tokens, i);
				if (token.type != STRING_EXP_TOKEN_CLOSEPAREN)
					return null;
				index[0] = i[0];
				leftExpression = testInside;
			}
		}
		if(leftExpression == null)
		{
			i = (int[]) index.clone();
			leftExpression = matchStringEvaluation(tokens, i, variables);
			if(leftExpression == null) leftExpression = matchNumEvaluation(tokens, i, variables);
		}
		if (leftExpression == null) return null;
		final int[] i2 = (int[]) i.clone();
		token = nextToken(tokens, i2);
		if ((token == null) || (token.type != STRING_EXP_TOKEN_WORD))
		{
			index[0] = i[0];
			return leftExpression;
		}
		i[0] = i2[0];
		final Boolean rightExpression = matchExpression(tokens, i, variables);
		if (rightExpression == null)
			return null;
		final Boolean result;
		if (token.value.equalsIgnoreCase("AND"))
			result = new Boolean(leftExpression.booleanValue() && rightExpression.booleanValue());
		else if (token.value.startsWith("&"))
			result = new Boolean(leftExpression.booleanValue() && rightExpression.booleanValue());
		else if (token.value.startsWith("|"))
			result = new Boolean(leftExpression.booleanValue() || rightExpression.booleanValue());
		else if (token.value.equalsIgnoreCase("OR"))
			result = new Boolean(leftExpression.booleanValue() || rightExpression.booleanValue());
		else if (token.value.equalsIgnoreCase("XOR"))
			result = new Boolean(leftExpression.booleanValue() != rightExpression.booleanValue());
		else
			throw new Exception("Parse Exception: Illegal expression evaluation combiner: " + token.value);
		index[0] = i[0];
		return result;
	}

	public final static boolean parseStringExpression(final String expression, final Map<String,Object> variables, final boolean emptyVarsOK) throws Exception
	{
		final Vector<StringExpToken> tokens = new Vector<StringExpToken>();
		int[] i = { 0 };
		StringExpToken token = nextStringToken(expression,i,variables, emptyVarsOK);
		while(token != null) {
			tokens.addElement(token);
			token = nextStringToken(expression,i,variables, emptyVarsOK);
		}
		if(tokens.size()==0) return true;
		i = new int[]{ 0 };
		final Boolean value = matchExpression(tokens, i, variables);
		if (value == null) throw new Exception("Parse error on following statement: " + expression);
		return value.booleanValue();
	}
	
}
