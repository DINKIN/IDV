/*
 * $Id: TestForms.java,v 1.31 2006/06/28 16:52:33 jeffmc Exp $
 *
 * Copyright  1997-2015 Unidata Program Center/University Corporation for
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

/**
 * In order to test the changes to HttpFormEntry, I created
 * a unit test for it. Testing http connections is always a bit
 * tricky, so what I did was to use an echo service: http://echo.httpkit.com.
 * The echo services accepts requests from some client and returns
 * what it received in text form, but encoded in Json format.
 * So, This test creates a form and sends it to the echo service.
 * It collects the returned Json, processes it and compares it
 * to expected value (see end of this file). Processing involves
 * removing irrelevant info and converting changeable info
 * to a fixed form or to removing it altogether.
 * (see the procedures process and cleanup).
 * If, eventually, the echo service I am using goes away,
 * then this test will need to be rewritten for some other
 * echo service.
 */


package ucar.unidata.ui.test;

import org.junit.Test;
import ucar.httpservices.HTTPException;
import ucar.httpservices.HTTPFormBuilder;
import ucar.httpservices.HTTPUtil;
import ucar.unidata.ui.HttpFormEntry;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test HttpFormEntry
 *
 * @author IDV development team
 */


public class TestForms extends UnitTestCommon
{

    //////////////////////////////////////////////////                         b
    // Constants

    static final protected boolean RAW = true;
    static final protected boolean DEBUG = true;

    // Pick a URL to test

    // Will send to esupport
    //static protected final String TESTURL = "http://www.unidata.ucar.edu/support/requestSupport.jsp";

    static protected final String TESTURL = "http://echo.httpkit.com";

    // Field values to use

    static final String DESCRIPTIONENTRY = "hello world";
    static final String NAMEENTRY = "Jim Jones";
    static final String EMAILENTRY = "jones@gmail.com";
    static final String ORGENTRY = "UCAR";
    static final String SUBJECTENTRY = "test httpformentry";
    static final String NAGENTRY = "I have no idea";
    static final String VERSIONENTRY = "1.0";
    static final String HARDWAREENTRY = "x86";
    static final String SOFTWAREPACKAGEENTRY = "IDV";
    static final String OSTEXT = System.getProperty("os.name");
    static final String EXTRATEXT = "whatever";
    static final String BUNDLETEXT = "bundle";
    static final String ATTACHTEXT = "arbitrary data\n";

    static final char QUOTE = '"';

    //////////////////////////////////////////////////
    // Instance fields

    protected boolean pass = true;

    File attach3file = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public TestForms()
    {
        super("TestForms");
    }

    //////////////////////////////////////////////////

    @Test
    public void
    testSimple()
            throws Exception
    {
        pass = true;
        List<HttpFormEntry> form = buildForm(false);
        String[] results = HttpFormEntry.doPost(form, TESTURL);
        assert (results.length == 2);
        if(results[0] != null) {
            System.err.println("fail; message=" + results[0]);
            pass = false;
        } else if(results[0] == null) {
            pass |= process(results[1], false);
        }
        assertTrue("TestForms.simple: failed", pass);
    }

    @Test
    public void
    testMultipartForm()
            throws Exception
    {
        // Try to create a tmp file
        this.attach3file = HTTPUtil.fillTempFile("attach3.txt",ATTACHTEXT);
        attach3file.deleteOnExit();

        pass = false;
        List<HttpFormEntry> form = buildForm(true);
        String[] results = HttpFormEntry.doPost(form, TESTURL);
        assert (results.length == 2);
        if(results[0] != null) {
            System.err.println("fail; message=" + results[0]);
            pass = false;
        } else if(results[0] == null) {
            pass |= process(results[1], true);
        }
        assertTrue("TestForms.multipart: failed", pass);
    }

    /**
     * Compute list of HttpFormEntry's to test.
     */
    protected List<HttpFormEntry> buildForm(boolean multipart)
    {
        List<HttpFormEntry> entries = new ArrayList<>();
        StringBuffer javaInfo = new StringBuffer();
        javaInfo.append("Java: home: " + System.getProperty("java.home"));
        javaInfo.append(" version: " + System.getProperty("java.version"));

        HttpFormEntry descriptionEntry;
        HttpFormEntry nameEntry;
        HttpFormEntry emailEntry;
        HttpFormEntry orgEntry;

        entries.add(nameEntry = new HttpFormEntry(HttpFormEntry.TYPE_INPUT,
                "fullName", "Name:", NAMEENTRY));
        entries.add(emailEntry = new HttpFormEntry(HttpFormEntry.TYPE_INPUT,
                "emailAddress", "Your Email:", EMAILENTRY));
        entries.add(orgEntry = new HttpFormEntry(HttpFormEntry.TYPE_INPUT,
                "organization", "Organization:", ORGENTRY));
        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_INPUT, "subject",
                "Subject:", SUBJECTENTRY));
        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_LABEL, "", NAGENTRY));
        entries.add(descriptionEntry =
                new HttpFormEntry(HttpFormEntry.TYPE_AREA, "description",
                        "Description:", DESCRIPTIONENTRY, 5, 30, true));
        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_HIDDEN, "submit",
                "", "Send Email"));

        entries.add(
                new HttpFormEntry(
                        HttpFormEntry.TYPE_HIDDEN, "softwarePackage", "",
                        SOFTWAREPACKAGEENTRY));
        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_HIDDEN,
                "packageVersion", "",
                VERSIONENTRY));
        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_HIDDEN, "os", "",
                OSTEXT));
        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_HIDDEN, "hardware",
                "", HARDWAREENTRY));

        if(multipart) {
            try {
                entries.add(new HttpFormEntry("attachmentOne",
                        "extra.html", EXTRATEXT.getBytes("UTF-8")));
                entries.add(new HttpFormEntry(
                        "attachmentTwo", "bundle.xidv",
                        BUNDLETEXT.getBytes("UTF-8")));
                if(attach3file != null)
                     entries.add(new HttpFormEntry(HttpFormEntry.TYPE_FILE,
                        "attachmentThree", "Attachment:", attach3file.getAbsolutePath()));
            } catch (UnsupportedEncodingException e) {
                return null;
            }
        }
        return entries;
    }

    protected boolean process(String body, boolean multipart)
            throws IOException
    {
        if(RAW)
            visual("Raw text", body);
        Object json = Json.parse(body);
        json = cleanup(json, multipart);
        String text = Json.toString(json);
        text = localize(text, OSTEXT);
        if(DEBUG)
            visual(multipart ? "TestMultipart" : "TestSimple", text);
        String diffs = compare(multipart ? "TestMultipart" : "TestSimple",
                multipart ? expectedMultipart : expectedSimple, text);
        if(diffs != null) {
            System.err.println(diffs);
            return false;
        }
        return true;
    }

    protected Map<String, Object> cleanup(Object o, boolean multipart)
            throws IOException
    {
        Map<String, Object> map = (Map<String, Object>) o;
        map = (Map<String, Object>) sort(map);
        Object oh = map.get("headers");
        String boundary = null;
        if(oh != null) {
            Map<String, Object> headers = (Map<String, Object>) oh;
            String formdata = (String) headers.get("content-type");
            if(oh == null) formdata = (String) headers.get("Content-Type");
            if(oh != null) {
                String[] pieces = formdata.split("[ \t]*[;][ \t]*");
                for(String p : pieces) {
                    if(p.startsWith("boundary=")) {
                        boundary = p.substring("boundary=".length(), p.length());
                        break;
                    }
                }
            }
            // Remove headers
            map.remove("headers");
        }
        String body = (String) map.get("body");
        if(body != null) {
            if(multipart && boundary != null) {
                Map<String, String> bodymap = parsemultipartbody(body);
                map.put("body", mapjoin(bodymap, "\n", ": "));
            } else {
                Map<String, String> bodymap = parsesimplebody(body);
                map.put("body", mapjoin(bodymap, "&", "="));
            }
        }
        return map;
    }

    protected String localize(String text, String os)
            throws HTTPException
    {
	// Replace both cases
        text = text.replace(os, "<OSNAME>");
        os = os.replace(' ', '+');
        text = text.replace(os, "<OSNAME>");
        return text;
    }

    protected Object sort(Object o)
    {
        if(o instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) o;
            map = new TreeMap(map); // Convert the map to sorted order
            for(Map.Entry<String, Object> entry : map.entrySet()) {
                map.put(entry.getKey(), sort(entry.getValue()));
            }
            return map;
        } else if(o instanceof List) {
            List<Object> list = (List) o;
            List<Object> out = new ArrayList<>();
            for(int i = 0; i < list.size(); i++) {
                out.add(sort(list.get(i)));
            }
            return out;
        } else
            return o;
    }

    protected Map<String, String> parsesimplebody(String body)
            throws HTTPException
    {
        Map<String, String> map = new TreeMap<>();
        String[] pieces = body.split("[&]");
        for(String piece : pieces) {
            String[] pair = piece.split("[=]");
            if(pair.length == 1) {
                pair = new String[]{pair[0], ""};
            }
            if(pair[0] == null || pair[0].length() == 0)
                throw new HTTPException("Illegal body : " + body);
            map.put(pair[0], pair[1]);
        }
        return map;
    }


    static final String patb = "--.*";
    static final Pattern blockb = Pattern.compile(patb);

    static final String patcd =
            "Content-Disposition:\\s+form-data;\\s+name=[\"]([^\"]*)[\"]";
    static final Pattern blockcd = Pattern.compile(patcd);
    static final String patcdx = patcd
            + "\\s*[;]\\s+filename=[\"]([^\"]*)[\"]";
    static final Pattern blockcdx = Pattern.compile(patcdx);

    protected Map<String, String> parsemultipartbody(String body)
            throws IOException
    {
        Map<String, String> map = new TreeMap<>();
        body = body.replace("\r\n", "\n");
        StringReader sr = new StringReader(body);
        BufferedReader rdr = new BufferedReader(sr);
        String line = rdr.readLine();
        if(line == null)
            throw new HTTPException("Empty body");
        for(; ; ) { // invariant is that the next unconsumed line is in line
            String name = null;
            String filename = null;
            StringBuilder value = new StringBuilder();
            if(!line.startsWith("--"))
                throw new HTTPException("Missing boundary marker : " + line);
            line = rdr.readLine();
            // This might have been the trailing boundary marker
            if(line == null)
                break;
            if(line.toLowerCase().startsWith("content-disposition")) {
                // Parse the content-disposition
                Matcher mcd = blockcdx.matcher(line); // try extended
                if(!mcd.lookingAt()) {
                    mcd = blockcd.matcher(line);
                    if(!mcd.lookingAt())
                        throw new HTTPException("Malformed Content-Disposition marker : " + line);
                    name = mcd.group(1);
                }  else {
                    name = mcd.group(1);
                    filename = mcd.group(2);
                }
            } else
                throw new HTTPException("Missing Content-Disposition marker : " + line);
            // Treat content-type line as optional; may or may not have charset
            line = rdr.readLine();
            if(line.toLowerCase().startsWith("content-type")) {
                line = rdr.readLine();
            }
            // treat content-transfer-encoding line as optional
            if(line.toLowerCase().startsWith("content-transfer-encoding")) {
                line = rdr.readLine();
            }
            // Skip one blank line
            line = rdr.readLine();
            // Extract the content
            value.setLength(0);
            while(!line.startsWith("--")) {
                value.append(line);
                value.append("\n");
                line = rdr.readLine();
            }
            map.put(name, value.toString());
        }
        return map;
    }

    static protected String join(String[] pieces, String sep)
    {
        StringBuilder buf = new StringBuilder();
        for(int i = 0; i < pieces.length; i++) {
            if(i > 0) buf.append(sep);
            buf.append(pieces[i]);
        }
        return buf.toString();
    }

    static protected String mapjoin(Map<String, String> map, String sep1, String sep2)
    {
        StringBuilder buf = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : map.entrySet()) {
            if(!first) buf.append(sep1);
            first = false;
            buf.append(entry.getKey());
            buf.append(sep2);
            buf.append(entry.getValue());
        }
        return buf.toString();
    }

    //////////////////////////////////////////////////

    static final String expectedMultipart =
            "{\n"
                    + "  \"body\" : \"attachmentOne: whatever\n"
                    + "attachmentThree: arbitrary data\n"
                    + "attachmentTwo: bundle\n"
                    + "description: hello world\n"
                    + "emailAddress: jones@gmail.com\n"
                    + "fullName: Jim Jones\n"
                    + "hardware: x86\n"
                    + "organization: UCAR\n"
                    + "os: <OSNAME>\n"
                    + "packageVersion: 1.0\n"
                    + "softwarePackage: IDV\n"
                    + "subject: test httpformentry\n"
                    + "submit: Send Email\n\",\n"
                    + "  \"docs\" : \"http://httpkit.com/echo\",\n"
                    + "  \"ip\" : \"127.0.0.1\",\n"
                    + "  \"method\" : \"POST\",\n"
                    + "  \"path\" : {\n"
                    + "    \"name\" : \"/\",\n"
                    + "    \"params\" : {},\n"
                    + "    \"query\" : \"\"\n"
                    + "  },\n"
                    + "  \"powered-by\" : \"http://httpkit.com\",\n"
                    + "  \"uri\" : \"/\"\n"
                    + "}\n";

    static final String expectedSimple =
            "{\n"
                    + "  \"body\" : \"description=hello+world&emailAddress=jones%40gmail.com&fullName=Jim+Jones&hardware=x86&organization=UCAR&os=<OSNAME>&packageVersion=1.0&softwarePackage=IDV&subject=test+httpformentry&submit=Send+Email\",\n"
                    + "  \"docs\" : \"http://httpkit.com/echo\",\n"
                    + "  \"ip\" : \"127.0.0.1\",\n"
                    + "  \"method\" : \"POST\",\n"
                    + "  \"path\" : {\n"
                    + "    \"name\" : \"/\",\n"
                    + "    \"params\" : {},\n"
                    + "    \"query\" : \"\"\n"
                    + "  },\n"
                    + "  \"powered-by\" : \"http://httpkit.com\",\n"
                    + "  \"uri\" : \"/\"\n"
                    + "}\n"
                    + "\n";
}
