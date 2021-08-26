package ca.uhn.fhir.jpa.starter.Util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParameterParser {
  public static String GetParameterFromString(String stringToSearch, String pattern)
  {
    Pattern p = Pattern.compile(pattern);
    Matcher m = p.matcher(stringToSearch);

    if (m.find())
    {
      return m.group(1);
    }

    return null;
  }
}
