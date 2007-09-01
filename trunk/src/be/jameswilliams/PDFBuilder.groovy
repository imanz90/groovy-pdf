// Copyright © 2007 James Williams 
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not 
// use this file except in compliance with the License. You may obtain a copy 
// of the License at 
//
// http://www.apache.org/licenses/LICENSE-2.0 
//
// Unless required by applicable law or agreed to in writing, software 
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
// License for the specific language governing permissions and limitations 
// under the License.
//  Author : James Williams
package be.jameswilliams

import com.lowagie.text.Document
import com.lowagie.text.DocumentException
import com.lowagie.text.Paragraph
import com.lowagie.text.PageSize
import com.lowagie.text.pdf.PdfWriter
import com.lowagie.text.Chunk
import com.lowagie.text.Font
import com.lowagie.text.FontFactory
import com.lowagie.text.Phrase
import com.lowagie.text.Image
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPRow
import java.net.URL
import java.util.logging.Level
import java.util.logging.Logger
import groovy.util.BuilderSupport
import org.codehaus.groovy.runtime.InvokerHelper
public class PDFBuilder extends BuilderSupport{
	private Logger log = Logger.getLogger(getClass().getName())
    private Map factories = new HashMap()
	static writer
	private elements = [ ]
    public shortcuts = [:]
    Map widgets = [:]
    
	PDFBuilder() {
		registerComponents()
    }
	
	def chunkFactory = {
		Chunk a = new Chunk(attributes.remove("text"))
		return a
	}
	
    void registerComponents() {
		registerBeanFactory("document", Document.class)
		registerBeanFactory("paragraph", Paragraph.class)
		registerBeanFactory("phrase", Phrase.class)
		registerBeanFactory("chunk", Chunk.class)
		registerBeanFactory("image", ImageFacade.class)
		registerBeanFactory("table", TableFacade.class)
		registerBeanFactory("cell", TableCellFacade.class)
		registerBeanFactory("row", PdfPRow.class)
		registerBeanFactory("alignedText", AlignedTextFacade.class)
		registerBeanFactory("directContent", DirectContentFacade.class)
    }
	
    Object createNode(name) {
		return createNode(name, null, null)
    }
	
    Object createNode(name,value) {
		return createNode(name,null,value)
    }
	
    Object createNode(name, Map attributes) {
		return createNode(name, attributes, null)
    }
	
	Object createNode(name, Map attributes, value) {
		def widget = null
		def factory = (Closure) factories.get(name)
		
		// stuff with getInstance methods have to be
		// handled differently
		// move to custom factories later
		if (name == "chunk") {
			widget = new Chunk(attributes.remove("text"))
			processAttributes(name,widget,attributes)
			return widget
		}
		String widgetName = (String) attributes.remove("id")
        if (factory == null) {
            log.log(Level.WARNING, "Could not find match for name: " + name)
            return null
        }
        try {
            widget = factory()
			if (widget == null) {
                log.log(Level.WARNING, "Factory for name: " + name + " returned null")
                return null
            }
            if (widgetName != null) {
                //widgets.put(widgetName, widget);
            }
            if (log.isLoggable(Level.FINE)) {
                log.fine("For name: " + name + " created widget: " + widget)
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create component for '" + name + "' reason: " + e, e)
        }
		println name
		processAttributes(name,widget, attributes)
		
		return widget
	}
	
	void processAttributes(widgetName, widget, attributes) {
		println "processing attrib "+ widget
		/*if (widget instanceof Chunk || widget instanceof Paragraph || widget instanceof Phrase) {
			if (attributes.text != null) {
				widget.add(new Chunk(attributes.remove("text")))
			}
		}
		else*/ if ( widget instanceof Document) {
			attributes.each{println it}
			if (attributes.filename != null) {
				println "reading filename"
				writer = PdfWriter.getInstance(widget, new FileOutputStream(attributes.remove("filename")))
				widget.open()
			}
		}
		/*else if (widgetName == "writeDirectTextContent") {
			widget.add(attributes)
		}*/
		for (entry in attributes) {
			println widgetName +" "+ entry
            String property = entry.getKey().toString()
            Object value = entry.getValue()
			if (property != "content")
				InvokerHelper.setProperty(widget, property, value)
			else InvokerHelper.setProperty(widget, property, new StringBuffer(value))
        }
	}
	
	void setParent(parent,child) {

	}
	
	void nodeCompleted(parent,node) {
		println parent
		println node
		if (node instanceof Document) {
			println "closing document"
			node.close()
		}
		else if (node instanceof ImageFacade)
			parent.add(node.process())
		else if (node instanceof DirectContentFacade)
			node.process()
		else if (node instanceof TableFacade)
			node.process()
		else if (node instanceof AlignedTextFacade) {
				println "here"
				parent.add(node)
		}
		else if (parent instanceof TableFacade) {
			parent.add(node)
		}
		
	}
	
	def createFactory = { a -> return { return a.newInstance() } }

    public void registerBeanFactory(String name, final Class beanClass) {
        registerFactory(name, createFactory(beanClass))
    }
    
    public void registerFactory(String name, Closure factory) {
        factories.put(name, factory);
    }
	
	public void addShortcut(className, propName, shortcut) {
		if (shortcuts[className] !=null) {
			shortcuts[className].put(shortcut,propName)
		}
		else {
			def a = [(shortcut.toString()):propName]
			shortcuts.put(className, a)
		}
    }
	
}