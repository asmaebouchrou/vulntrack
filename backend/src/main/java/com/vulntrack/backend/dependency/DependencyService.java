package com.vulntrack.backend.dependency;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Service
public class DependencyService {

    public List<DependencyInfo> extractDependencies(MultipartFile file) {

        try (InputStream input = file.getInputStream()) {

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            factory.setNamespaceAware(true);

            // Protección contra ataques XXE
            factory.setFeature(
                    "http://apache.org/xml/features/disallow-doctype-decl",
                    true);

            factory.setFeature(
                    "http://xml.org/sax/features/external-general-entities",
                    false);

            factory.setFeature(
                    "http://xml.org/sax/features/external-parameter-entities",
                    false);

            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            factory.setAttribute(
                    XMLConstants.ACCESS_EXTERNAL_DTD, "");

            factory.setAttribute(
                    XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            var document = factory.newDocumentBuilder().parse(input);
            Element root = document.getDocumentElement();

            if (!"project".equals(root.getLocalName()) ||
                    !"http://maven.apache.org/POM/4.0.0"
                            .equals(root.getNamespaceURI())) {
                throw new IllegalArgumentException(
                        "El archivo no es un POM Maven válido");
            }

            List<DependencyInfo> dependencies = new ArrayList<>();

            // Solo dependencias directas del proyecto
            Element dependenciesElement = directChild(root, "dependencies");

            if (dependenciesElement == null) {
                return dependencies;
            }

            NodeList nodes = dependenciesElement.getChildNodes();

            for (int i = 0; i < nodes.getLength(); i++) {

                Node node = nodes.item(i);

                if (!(node instanceof Element element) ||
                        !"dependency".equals(element.getLocalName())) {
                    continue;
                }

                String groupId = childText(element, "groupId");
                String artifactId = childText(element, "artifactId");
                String version = childText(element, "version");

                if (groupId == null || artifactId == null) {
                    continue;
                }

                dependencies.add(
                        new DependencyInfo(groupId, artifactId, version));
            }

            return dependencies;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "No se ha podido procesar el archivo pom.xml", e);
        }
    }

    private Element directChild(Element parent, String name) {

        NodeList children = parent.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);

            if (node instanceof Element element &&
                    name.equals(element.getLocalName())) {
                return element;
            }
        }

        return null;
    }

    private String childText(Element parent, String name) {

        Element child = directChild(parent, name);

        if (child == null) {
            return null;
        }

        String text = child.getTextContent().trim();

        return text.isEmpty() ? null : text;
    }
}