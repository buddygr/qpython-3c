package com.quseit.util;

public class StringUtils {
	public static String addSlashes(String txt)
    {
        if (null != txt)
        {
            txt = txt.replace("\\", "\\\\") ;
            txt = txt.replace("\'", "\\\'") ;
            //txt = txt.replace(" ", "\\ ") ;

        }

        return txt ;
    }


}
