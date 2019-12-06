package com.syb.lintplugin.utils

import org.gradle.api.Project
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class XMLUtils {

    private static FileReader fileReader

    private static StringReader changeSource(String filePath) {
        fileReader = new FileReader(filePath)
        StringBuilder xmlAsString = new StringBuilder(512)
        String line
        while ((line = fileReader.readLine()) != null) {
            if (line.contains(">&")) {
                xmlAsString.append(line.replace(">&", "*"))
            } else {
                xmlAsString.append(line)
            }
        }
        return new StringReader(xmlAsString.toString())
    }

    private static void closeFileReader() {
        try {
            if (fileReader != null) {
                fileReader.close()
            }
        } catch (Exception e) {
            println(e.message)
        }
    }

    static Map<String, String> parseLintResult(Project project, String filePath, int errorCount) {
        if (errorCount == 0) {
            errorCount = Integer.MAX_VALUE
        }
        Map<String, String> results = new HashMap<>()
        Map<String, String> result = null
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance()
            XmlPullParser xmlPullParser = factory.newPullParser()
            xmlPullParser.setInput(changeSource(filePath))
            String title = ""
            String desc = ""
            boolean need = true
            boolean add = true
            boolean have = false
            int eventType = xmlPullParser.getEventType()
            int count = 0
            while (eventType != (XmlPullParser.END_DOCUMENT) && count < errorCount){
                String nodeName = xmlPullParser.getName()
                switch (eventType){
                    case XmlPullParser.START_TAG:
                        if ("issue".equals(nodeName)) {
                            result = new HashMap<>()
                            String id = xmlPullParser.getAttributeValue(null, "id")
                            if (have && !"".equals(id) && (id.contains("InnerClassError") || id.contains("MapUseError"))) {
                                add = false
                            } else {
                                add = true
                                title = "【Lint扫描】" + id +
                                        "：" + xmlPullParser.getAttributeValue(null, "message")
                                desc = "扫描分支：" + GitUtils.getGitBranchName(project) +
                                        "\n问题所属模块：" + project.name +
                                        "\n请在 develop 分支进行修改。" +
                                        "\n说明：" + xmlPullParser.getAttributeValue(null, "explanation") +
                                        "\n问题代码：" + xmlPullParser.getAttributeValue(null, "errorLine1") +
                                        "\n文件位置：\n"
                            }
                        } else if ("location".equals(nodeName) && add) {
                            String file = xmlPullParser.getAttributeValue(null, "file")
                            if (file.contains("androidTest")) {
                                need = false
                            } else {
                                need = true
                                if (file.contains("allmodules")) {
                                    file = "/" + file.substring(file.indexOf("allmodules"), file.length())
                                } else {
                                    file = "/" + file.substring(file.indexOf(project.name), file.length())
                                }
                                desc = desc + file +
                                        "\n\t行数：" + xmlPullParser.getAttributeValue(null, "line") +
                                        "\n"
                            }
                        }
                        break
                    case XmlPullParser.END_TAG:
                        if ("issue".equals(nodeName) && need) {
                            if (add) {
                                result.put(desc, title)
                                results.putAll(result)
                                result.clear()
                                result == null
                                count++
                                if (title.contains("InnerClassError") || title.contains("MapUseError")) {
                                    have = true
                                }
                            }
                        }
                        break
                    default:
                        break
                }
                eventType = xmlPullParser.next()
            }
            return results
        } catch (Exception e) {
            println(e.message)
        } finally {
            closeFileReader()
        }
        return result
    }

    static Map<String, List<String>> parseLintResult(String filePath) {
        Map<String, List<String>> results = new HashMap<>()
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance()
            XmlPullParser xmlPullParser = factory.newPullParser()
            xmlPullParser.setInput(changeSource(filePath))
            String resourceId = ""
            List<String> files = null
            int eventType = xmlPullParser.getEventType()
            while (eventType != (XmlPullParser.END_DOCUMENT)){
                String nodeName = xmlPullParser.getName()
                switch (eventType){
                    case XmlPullParser.START_TAG:
                        if ("issue".equals(nodeName)) {
                            files = new ArrayList<>()
                            String message = xmlPullParser.getAttributeValue(null, "message")
                            resourceId = message.substring(message.indexOf("`")+1, message.lastIndexOf("`"))
                        } else if ("location".equals(nodeName)) {
                            String file = xmlPullParser.getAttributeValue(null, "file")
                            files.add(file)
                        }
                        break
                    case XmlPullParser.END_TAG:
                        if ("issue".equals(nodeName)) {
                            results.put(resourceId, files)
                        }
                        break
                    default:
                        break
                }
                eventType = xmlPullParser.next()
            }
            return results
        } catch (Exception e) {
            println(e.message)
        } finally {
            closeFileReader()
        }
        return results
    }

    static void modifyXml(String file, List<String> codes, String property, String value) {
        for (String code : codes) {
            modifyXml(file, code, property, value)
        }
    }

    static void modifyXml(String file, String code, String property, String value) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance()
            DocumentBuilder builder = factory.newDocumentBuilder()
            Document doc = builder.parse(file)
            Element root = doc.getDocumentElement()
            if ("selector".equals(root.nodeName)) {
                GroovyUtil.delete(new File(file))
                return
            }
            NodeList nodeList = doc.getElementsByTagName(code)
            if (nodeList == null || nodeList.length == 0) {
                return
            }
            for (int i = 0; i < nodeList.length; i++) {
                Element element = (Element) nodeList.item(i)
                String prop = element.getAttribute(property)
                prop = prop.replace(".", "_")
                if (prop.equals(value)) {
                    element.parentNode.removeChild(element)
                }
            }

            TransformerFactory tfactory = TransformerFactory.newInstance()
            Transformer trans = tfactory.newTransformer()
            doc.setXmlStandalone(true)
            DOMSource source = new DOMSource(doc)
            StreamResult result = new StreamResult(file)
            trans.transform(source, result)
        } catch (Exception e) {
            println("modifyXml Exception : " + e.message)
        }
    }

}