package com.quseit.util;

import java.util.ArrayList;

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

    public static void argvParse(String argString, ArrayList<String> argArray){
        if (argString == null) return;
        argString = argString.trim();
        int l = argString.length();
        if(l == 0) return;
        int i = 0,//参数起始点
            j = 0;//参数终止点
        char c,d;
        StringBuilder sb = new StringBuilder();
        while(j<l) {
            c = argString.charAt(j);
            if (c==' '){
                sb.append(argString,i,j);
                argArray.add(sb.toString());
                j++;
                while(j<l && argString.charAt(j)==' ')
                    j++;
                i=j;
                sb = new StringBuilder();
            } else if (c=='"' || c=='\'') {
                sb.append(argString, i, j);
                j++;
                i = j;
                if (j >= l)
                    continue;
                d = argString.charAt(j);
                while (d != c) {
                    if (d == '\\' && c == '"' && j < l - 1) {
                        sb.append(argString, i, j);
                        j++;
                        sb.append(argString, j, j + 1);
                        j++;
                        i = j;
                    } else j++;
                    d = argString.charAt(j);
                }
                sb.append(argString, i, j);
                j++;
                i = j;
            } else {
                j++;
            }
        }
        sb.append(argString,i,l);
        argArray.add(sb.toString());
    }

}
