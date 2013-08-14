package mon4h.framework.dashboard.common.config.impl;

import mon4h.framework.dashboard.common.config.Configure;
import mon4h.framework.dashboard.common.config.ConfigureNode;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicReference;


public class XmlConfigure implements Configure {
    private AtomicReference<ConfigureNode> configRoot = new AtomicReference<ConfigureNode>();
    //    private String xmlFileName;
    private InputStream is;
    private String name;

    private boolean isRealTextNodeValue(String value) {
        if (value == null) {
            return false;
        }
        int len = value.length();
        for (int i = 0; i < len; i++) {
            char iChar = value.charAt(i);
            if (iChar != '\t' && iChar != ' ' && iChar != '\r' && iChar != '\n') {
                return true;
            }
        }
        return false;
    }

    private ConfigureNode parseNode(Node node) {
        ConfigureNode rt = new ConfigureNode();
        rt.name = node.getNodeName();
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            if (node.hasAttributes()) {
                int attrs = node.getAttributes().getLength();
                for (int i = 0; i < attrs; i++) {
                    Node attrNode = node.getAttributes().item(i);
                    if (rt.attrs == null) {
                        rt.attrs = new LinkedHashMap<String, String>();
                    }
                    rt.attrs.put(attrNode.getNodeName(),
                            attrNode.getNodeValue());
                }
            }
        }
        if (node.hasChildNodes()) {
            NodeList nodeList = node.getChildNodes();
            if (nodeList != null) {
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node child = nodeList.item(i);
                    if (child != null
                            && child.getNodeType() == Node.TEXT_NODE) {
                        String value = child.getNodeValue();
                        if (isRealTextNodeValue(value)) {
                            rt.value = value;
                        }
                    } else if (child != null
                            && child.getNodeType() == Node.ELEMENT_NODE) {
                        if (rt.childs == null) {
                            rt.childs = new ArrayList<ConfigureNode>();
                        }
                        rt.childs.add(parseNode(child));
                    }
                }
            }
        }
        return rt;
    }

    private ConfigureNode parseXml()
            throws ParserConfigurationException, SAXException, IOException {
        ConfigureNode rt = new ConfigureNode();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(this.is);
        Element root = document.getDocumentElement();
        if (root == null) {
            return null;
        }
        rt.name = root.getNodeName();
        if (root.hasAttributes()) {
            int attrs = root.getAttributes().getLength();
            for (int i = 0; i < attrs; i++) {
                Node attrNode = root.getAttributes().item(i);
                if (rt.attrs == null) {
                    rt.attrs = new LinkedHashMap<String, String>();
                }
                rt.attrs.put(attrNode.getNodeName(), attrNode.getNodeValue());
            }
        }
        if (root.hasChildNodes()) {
            NodeList nodeList = root.getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node child = nodeList.item(i);
                if (child != null
                        && child.getNodeType() == Node.TEXT_NODE) {
                    String value = child.getNodeValue();
                    if (isRealTextNodeValue(value)) {
                        rt.value = value;
                    }
                } else if (child != null && child.getNodeType() == Node.ELEMENT_NODE) {
                    if (rt.childs == null) {
                        rt.childs = new ArrayList<ConfigureNode>();
                    }
                    rt.childs.add(parseNode(child));
                }
            }
        }
        return rt;
    }

    public static String lineTrim(String value) {
        if (value == null) {
            return null;
        }
        int len = value.length();
        int startPos = 0;
        int endPos = len - 1;
        for (int i = 0; i < len; i++) {
            char iChar = value.charAt(i);
            if (iChar != '\t' && iChar != ' ' && iChar != '\r' && iChar != '\n') {
                startPos = i;
                break;
            }
        }
        for (int i = len - 1; i >= 0; i--) {
            char iChar = value.charAt(i);
            if (iChar != '\t' && iChar != ' ' && iChar != '\r' && iChar != '\n') {
                endPos = i;
                break;
            }
        }
        if (endPos <= startPos) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = startPos; i <= endPos; i++) {
            sb.append(value.charAt(i));
        }
        return sb.toString();
    }

    public void setConfigFile(String xmlFileName) throws FileNotFoundException {
        File file = new File(xmlFileName);
        if (!file.exists()) {
            throw new java.io.FileNotFoundException(xmlFileName);
        }
        this.is = new FileInputStream(xmlFileName);
    }

    public void setInputStream(InputStream is) {
        this.is = is;
    }

    public void parse() throws ParserConfigurationException, SAXException, IOException {
        if (is == null) {
            throw new java.lang.IllegalArgumentException("XML file not set.");
        }
        ConfigureNode tmp = parseXml();
        if (tmp != null) {
            configRoot.getAndSet(tmp);
        }
    }

    @Override
    public int getInt(String cfgName, int defaultVal) {
        ConfigureNode configNode = configRoot.get();
        if (configNode != null) {
            try {
                String value = configNode.getValue(cfgName);
                int rt = Integer.parseInt(value);
                return rt;
            } catch (Exception e) {
                return defaultVal;
            }
        }
        return defaultVal;
    }

    @Override
    public double getDouble(String cfgName, double defaultVal) {
        ConfigureNode configNode = configRoot.get();
        if (configNode != null) {
            try {
                String value = configNode.getValue(cfgName);
                double rt = Double.parseDouble(value);
                return rt;
            } catch (Exception e) {
                return defaultVal;
            }
        }
        return defaultVal;
    }

    @Override
    public String getString(String cfgName, String defaultVal) {
        ConfigureNode configNode = configRoot.get();
        if (configNode != null) {
            try {
                String value = configNode.getValue(cfgName);
                return value;
            } catch (Exception e) {
                return defaultVal;
            }
        }
        return defaultVal;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ctrip.dashboard.common.config.Configure#getInt(java.lang.String)
     */
    @Override
    public int getInt(String cfgName) throws Exception {
        ConfigureNode configNode = configRoot.get();
        if (configNode != null) {
            try {
                String value = configNode.getValue(cfgName);
                int rt = Integer.parseInt(value);
                return rt;
            } catch (Exception e) {
                throw e;
            }
        }
        throw new ConfigureNode.NoSuchNodeException();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.ctrip.dashboard.common.config.Configure#getDouble(java.lang.String)
     */
    @Override
    public double getDouble(String cfgName) throws Exception {
        ConfigureNode configNode = configRoot.get();
        if (configNode != null) {
            try {
                String value = configNode.getValue(cfgName);
                double rt = Double.parseDouble(value);
                return rt;
            } catch (Exception e) {
                throw e;
            }
        }
        throw new ConfigureNode.NoSuchNodeException();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.ctrip.dashboard.common.config.Configure#getString(java.lang.String)
     */
    @Override
    public String getString(String cfgName) throws Exception {
        ConfigureNode configNode = configRoot.get();
        if (configNode != null) {
            try {
                String value = configNode.getValue(cfgName);
                return value;
            } catch (Exception e) {
                throw e;
            }
        }
        throw new ConfigureNode.NoSuchNodeException();
    }

    @Override
    public boolean getBoolean(String cfgName, boolean defaultVal) throws Exception {
        ConfigureNode configNode = configRoot.get();
        if (configNode != null) {
            try {
                String value = configNode.getValue(cfgName);
                return Boolean.valueOf(value);
            } catch (Exception e) {
                return defaultVal;
            }
        }
        return defaultVal;
    }

    @Override
    public ConfigureNode getConfigNode(String cfgName) throws Exception {
        ConfigureNode configNode = configRoot.get();
        if (configNode != null) {
            try {
                ConfigureNode value = configNode.getConfigNode(cfgName);
                return value;
            } catch (Exception e) {
                throw e;
            }
        }
        throw new ConfigureNode.NoSuchNodeException();
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

}
