/**
 *
 * Copyright 1997-2005 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */


package ucar.unidata.util;


import java.util.regex.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import java.util.regex.*;



/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class WikiUtil {

    private Hashtable properties;

    public WikiUtil() {
    }

    public WikiUtil(Hashtable properties) {
        this.properties = properties;
    }

    public void putProperty(Object key, Object value) {
        if(properties == null) properties = new Hashtable();
        properties.put(key,value);
    }

    public Object getProperty(Object key) {
        if(properties==null) return null;
        return properties.get(key);
    }

    public static interface WikiPageHandler {
        public String getWikiLink(WikiUtil wikiUtil, String name, String label);
        public String getWikiPropertyValue(WikiUtil wikiUtil, String property);
    }

    public String getPropertyValue(String property) {
        return null;
    }

    public   String wikify(String s, WikiPageHandler handler) {
        s = s.replace("\\\\[","_BRACKETOPEN_");

        s = s.replaceAll("'''''([^']+)'''''","<b><i>$1</i></b>");
        s = s.replaceAll("'''([^']+)'''","<b>$1</b>");
        s = s.replaceAll("''([^']+)''","<i>$1</i>");
        Pattern pattern;
        Matcher matcher;

        pattern  =Pattern.compile("\\[\\[([^\\]|]+)\\|?([^\\]]*)\\]\\]");
        matcher  =pattern.matcher(s);
        while(matcher.find()) {
            String name = matcher.group(1);
            String label = matcher.group(2);
            int start = matcher.start(0);
            int end =matcher.end(0);
            String link;
            if(handler == null) {
                if(label.trim().length()==0) label = name;
                link = "<a href=\"" + name +"\">" + label +"</a>";
            } else {
                link  = handler.getWikiLink(this, name, label);
            }
            s = s.substring(0,start) +link + s.substring(end);
            matcher =pattern.matcher(s);
        }

        int cnt = 0;
        pattern  =Pattern.compile("\\[([^\\]]+)\\]");
        matcher  =pattern.matcher(s);
        while(matcher.find()) {
            String name = matcher.group(1).trim();
            int idx = name.indexOf(" ");
            int start = matcher.start(0);
            int end =matcher.end(0);
            if(idx>0) {
                String label = name.substring(idx);
                name = name.substring(0,idx);
                s = s.substring(0,start) +"<a href=\"" + name +"\">" + label +"</a>" + s.substring(end);
            } else {
                cnt++;
                s = s.substring(0,start) +"<a href=\"" + name +"\">_BRACKETOPEN_" + cnt +"_BRACKETCLOSE_</a>" + s.substring(end);
            }
            matcher =pattern.matcher(s);
        }

       



        List headings = new ArrayList();
        pattern  =Pattern.compile("(?m)^\\s*(==+)([^=]+)(==+)\\s*$");
        matcher  =pattern.matcher(s);
        while(matcher.find()) {
            String prefix = matcher.group(1).trim();
            String label = matcher.group(2).trim();
            //            System.err.println("MATCH " + prefix + ":" + label);
            int start = matcher.start(0);
            int end =matcher.end(0);
            int level = prefix.length()-1;
            String value ="<a name=\"" + label +"\"></a><div class=\"wiki-h" + level +"\">" + label +"</div>";
            if(level==1)
                value = value+"<hr class=\"wiki-hr\">";
            headings.add(new Object[]{new Integer(level),label});
            s = s.substring(0,start)  + value+ s.substring(end);
            matcher =pattern.matcher(s);
        }





        StringBuffer buff = new StringBuffer();
        int ulCnt=0;
        int olCnt=0;
        for(String line:(List<String>) StringUtil.split(s,"\n",false,false)) {
            String tline = line.trim();
            int starCnt =0;
            while(tline.startsWith("*")) {
                tline = tline.substring(1);
                starCnt++;
            }
            if(starCnt>0) {
                if(starCnt>ulCnt) {
                    while(starCnt>ulCnt) {
                        buff.append("<ul>");
                        ulCnt++;
                    }
                } else {
                    while(starCnt<ulCnt && ulCnt>0) {
                        buff.append("</ul>");
                        ulCnt--;
                    }
                }
                buff.append("<li> ");
                buff.append(tline);
                continue;
            } 


            int hashCnt =0;
            while(tline.startsWith("#")) {
                tline = tline.substring(1);
                hashCnt++;
            }
            if(hashCnt>0) {
                if(hashCnt>olCnt) {
                    while(hashCnt>olCnt) {
                        buff.append("<ol>");
                        olCnt++;
                    }
                } else {
                    while(hashCnt<olCnt && olCnt>0) {
                        buff.append("</ol>");
                        olCnt--;
                    }
                }
                buff.append("<li> ");
                buff.append(tline);
                continue;
            } 

            while(olCnt>0) {
                buff.append("</ol>");
                olCnt--;
            }

            buff.append(line);
            buff.append("\n");
        }
        while(ulCnt>0) {
            buff.append("</ul>");
            ulCnt--;
        }

        while(olCnt>0) {
            buff.append("</ol>");
            olCnt--;
        }

        s = buff.toString();


        //        System.err.println("S:" + s.trim()+"\n***********************");
        StringBuffer sb = new StringBuffer();
        int baseIdx = 0;
        while(true) {
            int idx1 = s.indexOf("{{",baseIdx);
            if(idx1<0) {
                //                System.err.println("no idx1");
                sb.append(s.substring(baseIdx));
                break;
            }
            int idx2 = s.indexOf("}}",idx1);
            if(idx2<=idx1) {
                //                System.err.println("no idx2");
                sb.append(s.substring(baseIdx));
                break;
            }
            sb.append(s.substring(baseIdx,idx1));
            String property = s.substring(idx1+2,idx2);
            //            System.err.println("property:" + property);
            baseIdx = idx2+2;
            String value=null;
            if(handler!=null) {
                value = handler.getWikiPropertyValue(this,property);
            } 
            if(value==null) value = "Unknown property:" + property;
            sb.append(value);
        }

        s = sb.toString();

        /*
          <block title="foo">xxxxx</block>
         */
        //        while(true) {
            //            int idx1 = s.indexOf("<block");
            //            if(idx1<0) break;
            //            String first  = s.substring(0, idx1);
        //        }

        s = s.replace("_BRACKETOPEN_","[");
        s = s.replace("_BRACKETCLOSE_","]");
        //        s = s.replaceAll("(\n\r)+","<br>\n");
        //        s = s.replaceAll("\n+","<br>\n");

        if(headings.size()>=4) {
            StringBuffer toc  = new StringBuffer();
            makeHeadings(headings,toc,-1,"");
            String block;
            if(handler!=null) {
                block  = HtmlUtil.makeShowHideBlock("Contents", toc.toString(),true,HtmlUtil.cssClass("wiki-tocheader"),HtmlUtil.cssClass("wiki-toc"));
            } else {
                block = HtmlUtil.div(HtmlUtil.div("Contents"," class=\"wiki=tocheader\""),
                                     " class=\"wiki-toc\" ");
            }
            block = "<table width=\"30%\"><tr><td>" + block +"</td></tr></table>";
            s = block +s;
        }

        return s;
    }


    private static void makeHeadings(List headings, StringBuffer toc, int parentLevel, String parentPrefix) {
        int cnt = 0;
        int currentLevel=-1;
        String prefix="";
        while(headings.size()>0) {
            Object[]pair = (Object[]) headings.get(0);
            int level = ((Integer)pair[0]).intValue();
            if(level>currentLevel && currentLevel>=0) {
                makeHeadings(headings,toc,currentLevel,prefix);
                continue;
            }  else if (level< currentLevel) {
                if(parentLevel>=0) 
                    return;
            }
            headings.remove(0);
            cnt++;
            String label = (String)pair[1];
            if(parentPrefix.length()>0) {
                prefix = parentPrefix+"." + cnt;
            } else {
                prefix = "" + cnt;
            }
            //            System.err.println(prefix);
            toc.append(StringUtil.repeat("&nbsp;&nbsp;",level-1));
            toc.append("<a href=\"#" + label +"\">");
            toc.append(prefix);
            toc.append(HtmlUtil.space(1));
            toc.append(label);
            toc.append("</a><br>\n");
            currentLevel=level;
        }

    }


    public static void main(String[]args)  {
        try {
            String contents  = IOUtil.readContents(new java.io.File(args[0]));
            contents = new WikiUtil().wikify(contents,null);
            System.out.println (contents);
        } catch(Exception exc) {
            exc.printStackTrace();
        }
    }

}

